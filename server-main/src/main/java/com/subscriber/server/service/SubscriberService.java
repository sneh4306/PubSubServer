package com.subscriber.server.service;

import com.subscriber.server.entity.Message;
import com.subscriber.server.entity.MessageList;
import org.springframework.stereotype.Service;

@Service
public class SubscriberService {

    public SubscriberService() {
    }

    public void getMessage(MessageList messageList) {
        if (messageList == null)
            return;
        for (Message m : messageList.getMessageList()) {
            System.out.println("Received: " + m.getMessage());
        }
    }
}
