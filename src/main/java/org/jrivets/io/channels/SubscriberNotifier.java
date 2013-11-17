package org.jrivets.io.channels;

import org.jrivets.event.Subscriber;

interface SubscriberNotifier {

    void notifySubscriber(Subscriber subscriber, Object event);
    
}
