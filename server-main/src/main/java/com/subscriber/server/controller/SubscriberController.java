package com.subscriber.server.controller;

import com.subscriber.server.entity.MessageList;
import com.subscriber.server.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriber/")
public class SubscriberController {

    private final SubscriberService subscriberService;

    @Autowired
    public SubscriberController(final SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @RequestMapping(value = "health", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String isHealthy() {
        return "Service is healthy!";
    }

    @RequestMapping(value = "message", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void getMessage(@RequestBody MessageList messageList) {
        subscriberService.getMessage(messageList);
        return;
    }
}
