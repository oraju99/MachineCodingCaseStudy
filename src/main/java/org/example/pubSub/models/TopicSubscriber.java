package org.example.pubSub.models;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class TopicSubscriber {
    private final Topic topic;
    private final Subscriber subscriber;
    private final AtomicInteger offset;

    public TopicSubscriber(Topic topic, Subscriber subscriber) {
        this.topic = topic;
        this.subscriber = subscriber;
        this.offset = new AtomicInteger(0);
    }
}
