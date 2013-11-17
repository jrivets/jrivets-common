package org.jrivets.cluster.connection.net;

import org.jrivets.event.Subscriber;

public final class SubscriberNotification {

    private final Subscriber subscriber;
    
    private final Object event;
    
    SubscriberNotification(Subscriber subscriber, Object event) {
        this.subscriber = subscriber;
        this.event = event;
    }

    Subscriber getSubscriber() {
        return subscriber;
    }

    Object getEvent() {
        return event;
    }
    
    Object notifySubscriber() throws Exception {
        if (subscriber != null && event != null) {
            return subscriber.notifySubscriber(event);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "{subscriber=" + subscriber + ", event=" + event + "}";
    }
    
}
