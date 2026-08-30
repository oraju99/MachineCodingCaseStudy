package org.example.pubSub.models;

public class SimpleSubscriber extends Subscriber {
    protected SimpleSubscriber(String id, String name) {
        super(id, name);
    }

    @Override
    public void onMessage(Message message) throws InterruptedException {
        System.out.println("Subscriber [" + getName() + "] processed message: " + message.getPayload());
        Thread.sleep(100); // consumer processing latency
    }
}
