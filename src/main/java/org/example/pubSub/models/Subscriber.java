package org.example.pubSub.models;

import lombok.Data;

@Data
public abstract class Subscriber {
    private final String id;
    private final String name;

    protected Subscriber(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract void onMessage(Message message) throws InterruptedException;
}
