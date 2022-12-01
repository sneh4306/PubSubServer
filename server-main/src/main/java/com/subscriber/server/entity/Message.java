package com.subscriber.server.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    private String topic;
    private String message;
    private String publisher;

    public Message() {
    }

    @JsonCreator
    public Message(@JsonProperty("topic") String topic, @JsonProperty("message") String message,
                   @JsonProperty("publisher") String publisher) {
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
