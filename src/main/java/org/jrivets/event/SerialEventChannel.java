package org.jrivets.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.jrivets.collection.RingBuffer;
import org.jrivets.env.DefaultLockFactory;
import org.jrivets.env.LockFactory;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

/**
 * Event channel with asynchronous, sequential and guaranteed delivery
 * discipline. The {@link SerialEventChannel} intends for architectures where
 * guaranteed events delivery to few subscribers in published order is more
 * important than performance. It is not good for environments with many
 * independent subscribers, high amount of events per short period of time, or
 * where the event delivery sequence is not so important like the event
 * processing performance.
 * <p>
 * The implementation contains a buffer with fixed size where the published, but
 * not yet delivered events are stored. Normally the <tt>publish()</tt> returns
 * result instantly, but it blocks invocation thread in case of the buffer is
 * full until there is a place for the new event or the thread is interrupted.
 * All events are delivered to subscribers in a separate thread. Because of the
 * buffer and only one thread for delivery event to subscribers the events are
 * are always delivered in FIFO order.
 * <p>
 * <strong>NOTE: the implementation can be a cause of dead lock in case of
 * scenario as follows:</strong> subscriber which publishes an event to the same
 * <tt>SerialEventChannel</tt> which the subscriber listens for can be a cause
 * of dead-lock on the <tt>SerialEventChannel</tt>. This not healthy pattern of
 * usage should never take place with the implementation to avoid the problem.
 * <p>
 * Events are delivered synchronously for all subscribers. This means that two
 * or more subscribers cannot be notified simultaneously, but they are always
 * notified sequentially. This apply another limitation for the subscribers
 * implementation that should not block notification method for a long time,
 * otherwise they will block event delivery for the whole channel.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public final class SerialEventChannel implements EventChannel {

    private final Logger logger;

    private final Lock lock;

    private Condition condition;

    private State state = State.WAITING;

    private final SubscribersRegistry subscribersRegistry;

    private final RingBuffer<Object> events;

    private final Executor executor;

    private long waitingEventTimeoutMillis = 0L;

    enum State {
        WAITING, PROCESSING, PROCESSING_WAITING;
    }

    public SerialEventChannel(String name) {
        this(name, 100);
    }

    public SerialEventChannel(String name, int capacity) {
        this(name, capacity, Executors.newSingleThreadExecutor());
    }

    public SerialEventChannel(String name, int capacity, Executor executor) {
        this(name, capacity, executor, DefaultLockFactory.getInstance());
    }

    public SerialEventChannel(String name, int capacity, Executor executor, LockFactory lockFactory) {
        this(name, capacity, executor, lockFactory, new SubscribersRegistry(
                CommonSubscriberTypeParserProvider.getSubscriberTypeParser()));
    }

    SerialEventChannel(String name, int capacity, Executor executor, LockFactory lockFactory,
            SubscribersRegistry subscribersRegistry) {
        this.logger = LoggerFactory.getLogger(SerialEventChannel.class, name + "(%1$s): %2$s", state);
        this.lock = lockFactory.getReentrantLock();
        this.events = new RingBuffer<Object>(capacity);
        this.executor = executor;
        this.subscribersRegistry = subscribersRegistry;
    }

    @Override
    public boolean publish(Object e) {
        lock.lock();
        try {
            onNewEvent(e);
            runExecutorIfRequired();
            return true;
        } catch (InterruptedException ie) {
            logger.info("Could not publish the event ", e, " invoker is interrupted ");
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return false;
    }

    @Override
    public void addSubscriber(Object subscriber) {
        subscribersRegistry.subscribe(subscriber);
    }

    @Override
    public void removeSubscriber(Object subscriber) {
        subscribersRegistry.unsubscribe(subscriber);
    }

    void setWaitingTimeout(long millis) {
        waitingEventTimeoutMillis = millis;
    }

    State getState() {
        return state;
    }

    private void onNewEvent(Object e) throws InterruptedException {
        while (events.size() == events.capacity()) {
            logger.debug("Event queue is full, waiting space. ", this);
            getCondition().await();
        }
        logger.debug("New event to the channel: ", e);
        events.add(e);
    }

    private Condition getCondition() {
        if (condition == null) {
            condition = lock.newCondition();
        }
        return condition;
    }

    private void runExecutorIfRequired() {
        if (State.WAITING.equals(state)) {
            logger.trace("Running new execution thread.");
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    notifyListeners();
                }
            });
            state = State.PROCESSING;
        } else if (State.PROCESSING_WAITING.equals(state)) {
            getCondition().signalAll();
        }
    }

    private void notifyListeners() {
        logger.trace("Notify listeners");
        for (Object e = getEvent(); e != null; e = getEvent()) {
            logger.debug("Notify listeners about the event: ", e);
            for (Subscriber subscriber : subscribersRegistry.getSubscribers()) {
                subscriber.notifySubscriberSilently(e);
            }
        }
        logger.trace("No events in the queue, release the notification thread.");
    }

    private Object getEvent() {
        lock.lock();
        try {
            if (!waitAnEventOrDoneInTimeout()) {
                return null;
            }
            boolean needNotifyPublishThread = events.size() == events.capacity();
            Object result = events.removeFirst();
            if (needNotifyPublishThread) {
                logger.debug("Notify publish threads about getting events from full queue.");
                getCondition().signalAll();
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    private boolean waitAnEventOrDoneInTimeout() {
        if (!waitNewEvent()) {
            state = State.WAITING;
            return false;
        }
        return true;
    }

    private boolean waitNewEvent() {
        if (events.size() == 0 && waitingEventTimeoutMillis > 0L) {
            try {
                state = State.PROCESSING_WAITING;
                logger.trace("Waiting events in queue for ", waitingEventTimeoutMillis, "ms");
                getCondition().await(waitingEventTimeoutMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                logger.warn("The notification thread is interrupted, leaving it ...");
                return false;
            } finally {
                state = State.PROCESSING;
            }
        }
        return events.size() > 0;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{state=").append(state).append(", subscribers=")
                .append(subscribersRegistry.getSubscribers().size()).append(", events=").append(events.size())
                .append("}").toString();
    }
}
