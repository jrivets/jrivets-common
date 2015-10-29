package org.jrivets.event;

import java.util.Collection;
import java.util.concurrent.Executor;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public class AsyncEventChannel implements EventChannel {
    
    private final Logger logger = LoggerFactory.getLogger(AsyncEventChannel.class);

    private final SubscribersRegistry subscribersRegistry;
    
    private final Executor executor;

    public AsyncEventChannel(Executor executor) {
        this.subscribersRegistry = new SubscribersRegistry(CommonSubscriberTypeParserProvider.getSubscriberTypeParser());
        this.executor = executor;
    }
    
    @Override
    public boolean publish(final Object e) {
        executor.execute(() -> {
            int skip = 0;
            Collection<Subscriber> subscribers = subscribersRegistry.getSubscribers();
            for (Subscriber subscriber : subscribers) {
                try {
                    if (Subscriber.NO_SUCH_METHOD == subscriber.notifySubscriberIfMethodExists(e)) {
                        ++skip;
                    }
                } catch (Exception ex) {
                    logger.error("Got the error while delivering notification about event=", e, " to ", subscriber, ex);
                }
            }
            if (skip == subscribers.size()) {
                onNoSubscribers(e);
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

    protected void onNoSubscribers(Object e) {
        
    }
}
