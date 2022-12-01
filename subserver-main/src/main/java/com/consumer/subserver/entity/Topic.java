package com.consumer.subserver.entity;

import javax.validation.constraints.NotNull;

public class Topic {
    @NotNull
    private String topic;
    @NotNull
    private String subscriberIp;

    public Topic() {
    }

    public Topic(String topicId, String subscriberId) {
        this.topic = topicId;
        this.subscriberIp = subscriberId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubscriberIp() {
        return subscriberIp;
    }

    public void setSubscriberIp(String subscriberIp) {
        this.subscriberIp = subscriberIp;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "topicId='" + topic + '\'' +
                ", subscriberId='" + subscriberIp + '\'' +
                '}';
    }
}
