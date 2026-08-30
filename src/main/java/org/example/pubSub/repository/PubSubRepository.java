package org.example.pubSub.repository;

import org.example.pubSub.models.Subscriber;
import org.example.pubSub.models.Topic;
import org.example.pubSub.models.TopicSubscriber;

import java.util.List;
import java.util.Optional;

public interface PubSubRepository {
    void saveTopic(Topic topic);
    Optional<Topic> findTopicById(String id);
    List<Topic> getAllTopics();

    void saveSubscriber(Subscriber subscriber);
    Optional<Subscriber> findSubscriberById(String id);

    void addSubscription(String topicId, TopicSubscriber topicSubscriber);
    List<TopicSubscriber> getSubscriptionByTopic(String topic);
}
