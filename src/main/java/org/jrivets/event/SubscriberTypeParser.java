package org.jrivets.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class SubscriberTypeParser {

    private final Logger logger = LoggerFactory.getLogger(SubscriberTypeParser.class);
    
    private final Lock lock = new ReentrantLock();
    
    private Map<Class<?>, SubscriberTypeDetails> subscriberTypes = new HashMap<Class<?>, SubscriberTypeDetails>(0);
    
    SubscriberTypeDetails getSubscriberTypeDetails(Class<?> subscriberType) {
        SubscriberTypeDetails details = subscriberTypes.get(subscriberType);
        if (details == null) {
            details = getNewSubscriberTypeDetails(subscriberType);
        }
        return details;
    }
    
    private SubscriberTypeDetails getNewSubscriberTypeDetails(Class<?> subscriberType) {
        lock.lock();
        try {
            SubscriberTypeDetails details = subscriberTypes.get(subscriberType);
            if (details == null) {
                Map<Class<?>, SubscriberTypeDetails> newMap = new HashMap<Class<?>, SubscriberTypeDetails>(subscriberTypes);
                details = parseNewType(subscriberType);
                newMap.put(subscriberType, details);
                subscriberTypes = newMap;
            }
            return details;
        } finally {
            lock.unlock();
        }
    }
    
    private SubscriberTypeDetails parseNewType(Class<?> clazz) {
        logger.info("Parsing class ", clazz);
        Map<Class<?>, Method> eventsMap = new HashMap<Class<?>, Method>();
        Class<?> clz = clazz;
        while(clz != null) {
            parseType(clz, eventsMap);
            clz = clz.getSuperclass();
        }
        if (eventsMap.size() == 0) {
            onError("The class " + clazz + " cannot be used like subscriber: no methods annoteated by @OnEvent annotation.");
        }
        return new SubscriberTypeDetails(eventsMap);
    }
    
    private void parseType(Class<?> clazz, Map<Class<?>, Method> eventsMap) {
        Method[] methods = clazz.getDeclaredMethods();
        if (methods != null) {
            for (Method m: methods) {
                if (m.isAnnotationPresent(OnEvent.class)) {
                    logger.info("Found annotated method ", m.getName());
                    Class<?>[] params = m.getParameterTypes();
                    if (params == null || params.length != 1) {
                        onError("The class " + clazz + " cannot be used like subscriber: method " + m.getName() 
                                + " should contain only one parameter with the handled event type.");
                    }
                    Method anotherMethod = eventsMap.get(params[0]);
                    if (anotherMethod != null) {
                        onError("The class " + clazz + " cannot be used like subscriber due to ambiguous methods: " + m.getName() 
                                + " and " + anotherMethod.getName() + " both have same type parameter.");
                    }
                    logger.info("Accepting ", m.getName(), " with param ", params[0]);
                    eventsMap.put(params[0], m);
                }
            }
        }
    }
    
    private void onError(String message) {
        logger.error("Wrong subscriber: ", message);
        throw new IllegalArgumentException(message);
    }
}
