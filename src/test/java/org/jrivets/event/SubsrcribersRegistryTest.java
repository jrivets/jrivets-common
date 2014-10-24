package org.jrivets.event;

import org.jrivets.event.OnEvent;
import org.jrivets.event.Subscriber;
import org.jrivets.event.SubscriberTypeParser;
import org.jrivets.event.SubscribersRegistry;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SubsrcribersRegistryTest {

    private SubscribersRegistry registry;
    
    private SubscriberTypeParser typeParser = new SubscriberTypeParser();
    
    @BeforeMethod
    public void init() {
        registry = new SubscribersRegistry(typeParser);
    }
    
    private class A {
        @OnEvent
        void integer(Integer i) {
        }
    }

    private class B {
        @OnEvent
        void integer(Integer i) {
        }
        
        @OnEvent
        void integer(Double d) {
            
        }
    }
    
    @Test
    public void subscribe() {
        A a1 = new A();
        A a2 = new A();
        registry.subscribe(a1);
        assertTrue(registry.getSubscribers().contains(new Subscriber(a1, null)));
        assertFalse(registry.getSubscribers().contains(new Subscriber(a2, null)));
        registry.subscribe(a2);
        assertTrue(registry.getSubscribers().contains(new Subscriber(a1, null)));
        assertTrue(registry.getSubscribers().contains(new Subscriber(a2, null)));
        registry.unsubscribe(a2);
        assertTrue(registry.getSubscribers().contains(new Subscriber(a1, null)));
        assertFalse(registry.getSubscribers().contains(new Subscriber(a2, null)));
        registry.unsubscribe(a1);
        assertFalse(registry.getSubscribers().contains(new Subscriber(a1, null)));
        assertFalse(registry.getSubscribers().contains(new Subscriber(a2, null)));
    }

    @Test
    public void doubleSubscribe() {
        A a = new A();
        registry.subscribe(a);
        try {
            registry.subscribe(a);
            fail("Should thros IllegalArgumentException");
        } catch(IllegalArgumentException iae) {
            //OK
        }
    }
    
    @Test
    public void eventsCounter() {
        A a = new A();
        B b = new B();
        registry.subscribe(a);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
        registry.subscribe(b);
        assertTrue(registry.isEventAcceptable(new Integer(1)));
        assertTrue(registry.isEventAcceptable(new Double(1)));
        registry.unsubscribe(a);
        assertTrue(registry.isEventAcceptable(new Integer(1)));
        assertTrue(registry.isEventAcceptable(new Double(1)));
        registry.unsubscribe(b);
        assertFalse(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
    }
    
    @Test
    public void eventsCounter2() {
        A a = new A();
        B b = new B();
        registry.subscribe(a);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
        registry.subscribe(b);
        assertTrue(registry.isEventAcceptable(new Integer(1)));
        assertTrue(registry.isEventAcceptable(new Double(1)));
        registry.unsubscribe(b);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
        registry.unsubscribe(a);
        assertFalse(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
    }
    
    @Test
    public void eventsCounter3() {
        B b1 = new B();
        B b2 = new B();
        registry.subscribe(b1);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertTrue(registry.isEventAcceptable(new Double(0)));
        registry.subscribe(b2);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertTrue(registry.isEventAcceptable(new Double(0)));
        registry.unsubscribe(b1);
        assertTrue(registry.isEventAcceptable(new Integer(0)));
        assertTrue(registry.isEventAcceptable(new Double(0)));
        registry.unsubscribe(b2);
        assertFalse(registry.isEventAcceptable(new Integer(0)));
        assertFalse(registry.isEventAcceptable(new Double(0)));
    }
}
