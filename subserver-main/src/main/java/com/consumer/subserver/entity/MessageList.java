package com.consumer.subserver.entity;

import java.util.ArrayList;
import java.util.List;

public class MessageList {
    private List<Message> messageList;

    public MessageList() {
        messageList = new ArrayList<>();
    }

    public MessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }

    public void add(Message message) {
        messageList.add(message);
    }

    @Override
    public String toString() {
        return "MessageList{" +
                "messageList=" + messageList +
                '}';
    }
}
