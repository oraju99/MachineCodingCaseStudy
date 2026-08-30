package org.example.pubSub.repository;

import org.example.pubSub.models.Subscriber;
import org.example.pubSub.models.Topic;
import org.example.pubSub.models.TopicSubscriber;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryPubSubRepository implements PubSubRepository {
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();
    private final Map<String, List<TopicSubscriber>> topicSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void saveTopic(Topic topic) {
        topics.put(topic.getTopicId(), topic);
    }

    @Override
    public Optional<Topic> findTopicById(String id) {
        return Optional.ofNullable(topics.get(id));
    }

    @Override
    public List<Topic> getAllTopics() {
        return new ArrayList<>(topics.values());
    }

    @Override
    public void saveSubscriber(Subscriber subscriber) {
        subscribers.put(subscriber.getId(), subscriber);
    }

    @Override
    public Optional<Subscriber> findSubscriberById(String id) {
        return Optional.ofNullable(subscribers.get(id));
    }

    @Override
    public void addSubscription(String topicId, TopicSubscriber topicSubscriber) {
        topicSubscriptions
                .computeIfAbsent(topicId, k -> new CopyOnWriteArrayList<>())
                .add(topicSubscriber);
    }

    @Override
    public List<TopicSubscriber> getSubscriptionByTopic(String topicId) {
        List<TopicSubscriber> subs = topicSubscriptions.get(topicId);
        return subs != null ? subs : Collections.emptyList();
    }
}
