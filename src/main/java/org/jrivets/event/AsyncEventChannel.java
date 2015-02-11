package org.jrivets.event;

import java.util.concurrent.Executor;

public class AsyncEventChannel implements EventChannel {

    private final SubscribersRegistry subscribersRegistry;
    
    private final Executor executor;

    public AsyncEventChannel(Executor executor) {
        this.subscribersRegistry = new SubscribersRegistry(CommonSubscriberTypeParserProvider.getSubscriberTypeParser());
        this.executor = executor;
    }
    
    @Override
    public boolean publish(final Object e) {
        executor.execute(() -> {
            for (Subscriber subscriber : subscribersRegistry.getSubscribers()) {
                subscriber.notifySubscriberSilently(e);
            }
        });
        return true;
    }

    @Override
    public void addSubscriber(Object subscriber) {
        subscribersRegistry.subscribe(subscriber);
    }

    @Override
    public void removeSubscriber(Object subscriber) {
        subscribersRegistry.unsubscribe(subscriber);
    }

}
