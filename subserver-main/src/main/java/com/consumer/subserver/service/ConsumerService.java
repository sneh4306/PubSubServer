package com.consumer.subserver.service;

import com.consumer.subserver.entity.Message;
import com.consumer.subserver.entity.MessageList;
import com.consumer.subserver.entity.Subscriber;
import com.consumer.subserver.entity.Topic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class ConsumerService {

    static private HashMap<String, MessageList> map;
    private ReentrantReadWriteLock reentrantReadWriteLock;
    private ObjectMapper mapper;
    private String queueIpAddress;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> subscriberCollection;
    private MongoCollection<Document> topicCollection;


    public ConsumerService() {
        queueIpAddress = "localhost:9000";
        map = new HashMap<>();
        reentrantReadWriteLock = new ReentrantReadWriteLock();
        mapper = new ObjectMapper();
        mongoClient = new MongoClient(new MongoClientURI("mongodb+srv://root:passwordroot@cluster0.18gmih5.mongodb.net/?retryWrites=true&w=majority"));
        database = mongoClient.getDatabase("pub_sub");
        subscriberCollection = database.getCollection("subscribers");
        topicCollection = database.getCollection("topics");
    }

    @Async
    public void fetchMessage() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        List<MediaType> mediaTypeList = new ArrayList<>();
        mediaTypeList.add(MediaType.valueOf("text/html;charset=UTF-8"));
        mediaTypeList.add(MediaType.APPLICATION_JSON);
        converter.setSupportedMediaTypes(mediaTypeList);
        messageConverters.add(converter);
        restTemplate.setMessageConverters(messageConverters);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.ALL));

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        while (true) {
            String queueUrl = "http://" + queueIpAddress + "/nextMessage";
            try {
                ResponseEntity<Message> result =
                        restTemplate.exchange(queueUrl, HttpMethod.GET, entity, Message.class);
                Message message = result.getBody();
                addEntry(message);
                sendMessagesToSubscribers();
            } catch (Exception e) {
                return;
            }
        }
    }

    private void addEntry(Message message) {
        String topic = message.getTopic();

        if (topic != null) {
            reentrantReadWriteLock.writeLock().lock();
            try {
                MessageList messageList = map.getOrDefault(topic, new MessageList());
                messageList.add(message);
                map.put(topic, messageList);
            } finally {
                reentrantReadWriteLock.writeLock().unlock();
            }
        }
    }

    @Async
    public void sendMessagesToSubscribers() throws JsonProcessingException {
        reentrantReadWriteLock.writeLock().lock();
        try {
            for (String topic : map.keySet()) {
                List<Subscriber> subscribers = getSubscribers(topic);
                for (Subscriber subscriber : subscribers) {
                    sendMessage(subscriber, map.get(topic));
                }
            }
            map.clear();
        } finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Async
    private void sendMessage(Subscriber subscriber, MessageList messageList) throws JsonProcessingException {
        if (subscriber.getSubscriberIp() == null)
            return;

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + subscriber.getSubscriberIp() + ":8081/subscriber/message";
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        String json = mapper.writeValueAsString(messageList);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<Void> result =
                restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }

    public List<Subscriber> getSubscribers(String topicId) {
        List<Subscriber> subscriberList = new ArrayList<>();
        Map<String, Set<String>> subscriberToTopics = new HashMap<>();

        FindIterable<Document> iterable = topicCollection.find();
        if (iterable.first() != null) {
            MongoCursor<Document> cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                ArrayList<String> subscriberIpList = (ArrayList<String>) document.get("subscriber_list");
                for (String ip : subscriberIpList) {
                    Set<String> topics = subscriberToTopics.getOrDefault(ip, new HashSet<>());
                    String topicIp = (String) document.get("topic_ip");
                    topics.add(topicIp);
                    subscriberToTopics.put(ip, topics);
                }
            }
        }
        for (String subscriberIp : subscriberToTopics.keySet()) {
            if (subscriberToTopics.get(subscriberIp).contains(topicId)) {
                Subscriber subscriber = new Subscriber(subscriberToTopics.get(subscriberIp), subscriberIp);
                subscriberList.add(subscriber);
            }
        }
        return subscriberList;
    }

    public void updateQueue(String ipAddress) {
        queueIpAddress = ipAddress;
    }

    public String addSubscriber(Subscriber subscriber) {
        if (subscriberExists(subscriber.getSubscriberIp()))
            return "Subscriber already exists";

        subscriberCollection.insertOne(new Document().append("subscriber_ip", subscriber.getSubscriberIp()));
        return "Subscriber added successfully";
    }

    private boolean subscriberExists(String subscriberIp) {
        if (subscriberCollection.count() > 0) {
            FindIterable<Document> iterable = subscriberCollection.find(new Document("subscriber_ip", subscriberIp));
            if (iterable.first() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean registerTopic(Topic topic) {
        // Check if subscriber is valid
        if (!subscriberExists(topic.getSubscriberIp()))
            return false;

        FindIterable<Document> iterable = topicCollection.find(new Document("topic_ip", topic.getTopic()));
        // exists
        if (iterable.first() != null) {
            Document document = iterable.iterator().next();
            ArrayList<String> subscriberIpList = (ArrayList<String>) document.get("subscriber_list");
            if (!subscriberIpList.contains(topic.getSubscriberIp())) {
                subscriberIpList.add(topic.getSubscriberIp());
                BasicDBObject query = new BasicDBObject();
                query.put("topic_ip", topic.getTopic());

                BasicDBObject newDocument = new BasicDBObject();
                newDocument.put("subscriber_list", subscriberIpList);

                BasicDBObject updateObject = new BasicDBObject();
                updateObject.put("$set", newDocument);

                topicCollection.updateOne(query, updateObject);
            }
            return true;
        }
        return false;
    }

    public List<String> getAllSubscribers() {
        List<String> subscriberList = new ArrayList<>();
        FindIterable<Document> iterable = subscriberCollection.find();
        if (iterable.first() != null) {
            MongoCursor<Document> cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                subscriberList.add((String) document.get("subscriber_ip"));
            }
        }
        return subscriberList;
    }
}
