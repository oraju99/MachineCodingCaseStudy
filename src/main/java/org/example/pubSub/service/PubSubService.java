package org.example.pubSub.service;

import org.example.pubSub.models.Message;
import org.example.pubSub.models.Subscriber;
import org.example.pubSub.models.Topic;
import org.example.pubSub.models.TopicSubscriber;
import org.example.pubSub.repository.PubSubRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PubSubService {
    private final PubSubRepository pubSubRepository;
    private final ExecutorService subscriberExecutor;

    public PubSubService(PubSubRepository pubSubRepository) {
        this.pubSubRepository = pubSubRepository;
        subscriberExecutor = Executors.newCachedThreadPool();
    }

    public Topic createTopic(String id, String name) {
        Topic topic = new Topic(id, name);
        pubSubRepository.saveTopic(topic);
        System.out.println("Topic created: " + name + " [ID: " + id + "]");
        return topic;
    }

    public void registerSubscriber(Subscriber subscriber) {
        pubSubRepository.saveSubscriber(subscriber);
    }

    public void subscribe(String subscriberId, String topicId) throws Exception {
        Subscriber subscriber = pubSubRepository.findSubscriberById(subscriberId)
                .orElseThrow( () -> new Exception("Subscriber not found for : " + subscriberId) );
        Topic topic = pubSubRepository.findTopicById(topicId)
                .orElseThrow( () -> new Exception("Topic not found for : " + topicId) );

        TopicSubscriber topicSubscriber = new TopicSubscriber(topic, subscriber);
        pubSubRepository.addSubscription(topicId, topicSubscriber);

        subscriberExecutor.submit( () -> consumeLoop(topicSubscriber) );
        System.out.println("Subscriber " + subscriber.getName() + " subscribed to Topic " + topic.getTopicName());
    }

    public void publish(String topicId, Message message) throws Exception {
        Topic topic = pubSubRepository.findTopicById(topicId)
                .orElseThrow(() -> new Exception("Topic not found with ID: " + topicId));
        topic.addMessage(message);

        List<TopicSubscriber> topicSubscriberList = pubSubRepository.getSubscriptionByTopic(topicId);
        for (TopicSubscriber ts : topicSubscriberList) {
            synchronized (ts) {
                ts.notify();
            }
        }
    }

    private void consumeLoop(TopicSubscriber ts) {
        Topic topic = ts.getTopic();
        Subscriber subscriber = ts.getSubscriber();

        while( !Thread.currentThread().isInterrupted() ) {
            Message messageToProcess;
            synchronized (ts) {
                while ( ts.getOffset().get() >= topic.getMessages().size() ) {
                    try {
                        ts.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                int currentOffset = ts.getOffset().getAndIncrement();
                messageToProcess = topic.getMessages().get(currentOffset);
            }

            try {
                subscriber.onMessage(messageToProcess);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void shutdown() {
        subscriberExecutor.shutdown();
    }

}
