package com.consumer.subserver.entity;

public class Message {
    private String topic;
    private String message;
    private String publisher;

    public Message() {
    }

    public Message(String topic, String message, String publisher) {
        this.topic = topic;
        this.message = message;
        this.publisher = publisher;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public String toString() {
        return "Message{" +
                "topic='" + topic + '\'' +
                ", message='" + message + '\'' +
                ", publisher='" + publisher + '\'' +
                '}';
    }
}
