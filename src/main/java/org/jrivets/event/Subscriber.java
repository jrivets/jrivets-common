package org.jrivets.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public final class Subscriber {

    private final Object subscriber;

    private final SubscriberTypeDetails typeDetails;

    Subscriber(Object subscriber, SubscriberTypeDetails typeDetails) {
        this.subscriber = subscriber;
        this.typeDetails = typeDetails;
    }

    public Subscriber(Object subscriber) {
        this.subscriber = subscriber;
        this.typeDetails = CommonSubscriberTypeParserProvider.getSubscriberTypeParser().getSubscriberTypeDetails(
                subscriber.getClass());
    }

    public Object notifySubscriber(Object e) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException  {
        Method m = typeDetails.getMethod(e);
        if (m != null) {
            return m.invoke(subscriber, e);
        }
        throw new NoSuchMethodException("There is no annotated method in " + subscriber.getClass() + " class to handle "
                + (e == null ? null : e.getClass()) + " types of events");
    }

    public Exception notifySubscriberSilently(Object e) {
        try {
            notifySubscriber(e);
            return null;
        } catch (Exception ex) {
            return ex;
        }
    }

    Set<Class<?>> getAcceptedEventsSet() {
        return typeDetails.getEventsMap().keySet();
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
