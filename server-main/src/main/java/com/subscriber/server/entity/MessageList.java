package com.subscriber.server.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private List<Message> messageList;

    public MessageList() {
        messageList = new ArrayList<>();
    }

    @JsonCreator
    public MessageList(@JsonProperty("messageList") List<Message> messageList) {
        this.messageList = messageList;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    @Override
    public String toString() {
        return "MessageList{" +
                "messageList=" + messageList +
                '}';
    }
}
