package org.jrivets.io.channels;

import java.util.concurrent.Executor;

import org.jrivets.event.Subscriber;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class ExecutorSubscriberNotifier implements SubscriberNotifier {
    
    private final Logger logger;

    private final Executor executor;
    
    ExecutorSubscriberNotifier(Executor executor) {
        this.logger = LoggerFactory.getLogger(ExecutorSubscriberNotifier.class);
        this.executor = executor;
    }
    
    @Override
    public void notifySubscriber(final Subscriber subscriber, final Object event) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    subscriber.notifySubscriber(event);
                } catch (NoSuchMethodException nme) {
                    logger.debug("notifySubscriber(): Cannot find method: ", nme.getMessage(), " subscriber: ",
                            subscriber, ", event: ", event);
                } catch (Exception e) {
                    logger.error("notifySubscriber(): Exception while notify subscriber: ", subscriber, ", event: ", event, e);
                }
            }
        });
    }
    
    @Override
    public String toString() {
        return "{executor=" + executor.toString() + "}";
    }
}
