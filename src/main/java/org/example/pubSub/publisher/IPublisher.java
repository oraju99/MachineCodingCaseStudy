package org.example.pubSub.publisher;

public interface IPublisher {
    String getId();
    void publish(String topicId, String message) throws IllegalArgumentException;
}
