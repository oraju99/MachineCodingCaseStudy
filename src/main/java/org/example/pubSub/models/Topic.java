package org.example.pubSub.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class Topic {
    private final String topicName;
    private final String topicId;
    private final List<Message> messages;

    public Topic(String topicName, String topicId) {
        this.topicName = topicName;
        this.topicId = topicId;
        this.messages = new ArrayList<Message>();
    }

    public synchronized void addMessage(Message message) {
        messages.add(message);
    }

    public synchronized List<Message> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
