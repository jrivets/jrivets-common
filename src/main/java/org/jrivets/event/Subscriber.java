package org.jrivets.event;

import java.lang.reflect.Method;
import java.util.Set;

final class Subscriber {

    private final Object subscriber;
    
    private final SubscriberTypeDetails typeDetails;
    
    Subscriber(Object subscriber, SubscriberTypeDetails typeDetails) {
        this.subscriber = subscriber;
        this.typeDetails = typeDetails;
    }
    
    Exception notify(Object e) {
        Method m = typeDetails.getMethod(e);
        if (m != null) {
            return invoke(m, e);
        }
        return null;
    }
    
    Set<Class<?>> getAcceptedEventsSet() {
        return typeDetails.getEventsMap().keySet();
    }
    
    private Exception invoke(Method m, Object e) {
        try {
            m.setAccessible(true);
            m.invoke(subscriber, e);
            return null;
        } catch (Exception ex) {
            return ex;
        }
    }
    
    @Override
    public int hashCode() {
        return subscriber.hashCode();
    }
    
    @Override
    public boolean equals(Object anotherObject) {
        if (!(anotherObject instanceof Subscriber)) {
            return false;
        }
        return subscriber.equals(((Subscriber) anotherObject).subscriber);
    }
    
}
