package com.consumer.subserver.controller;

import com.consumer.subserver.entity.Subscriber;
import com.consumer.subserver.entity.Topic;
import com.consumer.subserver.service.ConsumerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subserver/")
public class ConsumerController {

    private final ConsumerService consumerService;

    @Autowired
    public ConsumerController(final ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @RequestMapping(value = "health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> isHealthy() {
        return new ResponseEntity<>("Service is healthy!", HttpStatus.OK);
    }

    @RequestMapping(value = "trigger", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> trigger() throws JsonProcessingException {
        consumerService.fetchMessage();
        return new ResponseEntity<>("Service triggered", HttpStatus.OK);
    }

    @RequestMapping(value = "queue-id", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> updateQueue(@RequestParam String ipAddress) {
        consumerService.updateQueue(ipAddress);
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @RequestMapping(value = "add-subscriber", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> addSubscriber(@RequestBody Subscriber subscriber) {
        String subscriberId = consumerService.addSubscriber(subscriber);
        return new ResponseEntity<>(subscriberId, HttpStatus.OK);
    }

    @RequestMapping(value = "register-topic", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> registerTopic(@RequestBody Topic topic) {
        boolean registered = consumerService.registerTopic(topic);
        if (!registered)
            return new ResponseEntity<>("Could not register topic. Check if given subscriber and topic exist.", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>("Topic registered", HttpStatus.OK);
    }

    @RequestMapping(value = "subscribers", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<String>> getAllSubscribers() {
        List<String> subscriberList = consumerService.getAllSubscribers();
        return new ResponseEntity<>(subscriberList, HttpStatus.OK);
    }
}
