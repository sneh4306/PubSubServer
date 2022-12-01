package com.consumer.subserver.entity;

import javax.validation.constraints.NotNull;
import java.util.Set;

public class Subscriber {
    private Set<String> topicList;
    @NotNull
    private String subscriberIp;

    public Subscriber() {
    }

    public Subscriber(Set<String> topicList, String subscriberIp) {
        this.topicList = topicList;
        this.subscriberIp = subscriberIp;
    }

    public Set<String> getTopicList() {
        return topicList;
    }

    public void setTopicList(Set<String> topicList) {
        this.topicList = topicList;
    }

    public String getSubscriberIp() {
        return subscriberIp;
    }

    public void setSubscriberIp(String subscriberIp) {
        this.subscriberIp = subscriberIp;
    }


    public void addTopic(String topicId) {
        this.topicList.add(topicId);
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "topicList=" + topicList +
                ", subscriberIp='" + subscriberIp + '\'' +
                '}';
    }
}
