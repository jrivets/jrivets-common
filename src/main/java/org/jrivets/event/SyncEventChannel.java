package org.jrivets.event;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Event channel with synchronous delivery discipline.
 * <p>
 * This implementation delivers every event under holding internal lock object
 * from the publisher thread. This guarantees that only one event can be
 * delivered at a time to all subscribers, but it doesn't guarantee the delivery
 * order. Publishers can be blocked on the lock object for unpredicted time. The
 * internal lock object can be configured like a fair one to avoid unreasonable
 * blocking some publishers for a long time (please see fairness for the
 * {@link Lock} objects).
 * 
 * @author Dmitry Spasibenko
 * 
 */
public class SyncEventChannel implements EventChannel {

    private final SubscribersRegistry subscribersRegistry;

    private final Lock lock;

    public SyncEventChannel() {
        this(false);
    }

    public SyncEventChannel(boolean fairLock) {
        subscribersRegistry = new SubscribersRegistry(CommonSubscriberTypeParserProvider.getSubscriberTypeParser());
        lock = new ReentrantLock(fairLock);
    }

    @Override
    public boolean publish(Object e) {
        lock.lock();
        try {
            for (Subscriber subscriber : subscribersRegistry.getSubscribers()) {
                subscriber.notifySubscriberSilently(e);
            }
        } finally {
            lock.unlock();
        }
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
