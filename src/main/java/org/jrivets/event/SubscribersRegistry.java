package org.jrivets.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SubscribersRegistry {

    private final CopyOnWriteArraySet<Subscriber> subscribers = new CopyOnWriteArraySet<Subscriber>();
    
    private Set<Class<?>> acceptableEvents = new HashSet<Class<?>>();
    
    private final Map<Class<?>, Integer> listenersCounter = new HashMap<Class<?>, Integer>();
    
    private final SubscriberTypeParser parser;
    
    private final Lock lock = new ReentrantLock();
    
    SubscribersRegistry(SubscriberTypeParser parser) {
        this.parser = parser;
    }
    
    void subscribe(Object subscriberObject) {
        lock.tryLock();
        try {
            Subscriber subscriber = newSubscriber(subscriberObject);
            if (subscribers.contains(subscriber)) {
                throw new IllegalArgumentException("Already subscribed");
            }
            storeNewSubscriber(subscriber);
        } finally {
            lock.unlock();
        }
    }
    
    void unsubscribe(Object subscriberObject) {
        lock.tryLock();
        try {
            Subscriber subscriber = newSubscriber(subscriberObject);
            if (subscribers.remove(subscriber)) {
                removeSubscriber(subscriber);
            }
        } finally {
            lock.unlock();
        }
    }
    
    boolean isEventAcceptable(Object event) {
        Set<Class<?>> events = acceptableEvents;
        return events.contains(event.getClass());
    }
    
    Set<Subscriber> getSubscribers() {
        return subscribers;
    }
    
    private void storeNewSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
        Set<Class<?>> acceptedEvents = subscriber.getAcceptedEventsSet();
        for (Class<?> event: acceptedEvents) {
            addNewEventOrUpdateCounter(event);
        }
    }
    
    private void addNewEventOrUpdateCounter(Class<?> event) {
        Integer counter = listenersCounter.get(event);
        if (counter == null) {
            Set<Class<?>> events = new HashSet<Class<?>>(acceptableEvents);
            events.add(event);
            acceptableEvents = events;
            counter = 0;
        }
        listenersCounter.put(event, counter + 1);
    }
    
    private void removeSubscriber(Subscriber subscriber) {
        subscribers.remove(subscriber);
        Set<Class<?>> acceptedEvents = subscriber.getAcceptedEventsSet();
        for (Class<?> event: acceptedEvents) {
            removeEvent(event);
        }
    }
    
    private void removeEvent(Class<?> event) {
        Integer counter = listenersCounter.get(event);
        if (counter.equals(1)) {
            listenersCounter.remove(event);
            Set<Class<?>> events = new HashSet<Class<?>>(acceptableEvents);
            events.remove(event);
            acceptableEvents = events;
            return;
        }
        listenersCounter.put(event, counter - 1);
    }
    
    private Subscriber newSubscriber(Object subscriberObject) {
        SubscriberTypeDetails typeDetails = parser.getSubscriberTypeDetails(subscriberObject.getClass());
        return new Subscriber(subscriberObject, typeDetails);
    }
    
}
