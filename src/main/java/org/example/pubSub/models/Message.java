package org.example.pubSub.models;

import lombok.Data;

@Data
public class Message {
    private final String id;
    private final String payload;

    public Message(String id, String payload) {
        this.id = id;
        this.payload = payload;
    }
}
