package org.jrivets.event;

import static org.testng.Assert.*;

import java.lang.reflect.InvocationTargetException;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SubscriberTest {

    @SuppressWarnings("unused")
    private static class TestListener {
        
        @OnEvent
        int onInteger(Integer i) {
            return i + 1;
        }
        
        @OnEvent
        void onInteger(Double d) {
        }
        
        @OnEvent
        void onException(Exception ex) throws IllegalArgumentException {
            throw new IllegalArgumentException();
        }
        
        @OnEvent
        void onException(InterruptedException ie) throws InterruptedException {
            throw ie;
        }
    }
    
    private Subscriber subscriber;
    
    @BeforeTest
    void init() {
        subscriber = new Subscriber(new TestListener());
    }
    
    @Test
    public void notifyTest() throws Exception {
        assertEquals(subscriber.notifySubscriber(new Integer(2)), 3);
        assertNull(subscriber.notifySubscriber(new Double(3.4)));
    }
    
    @Test(expectedExceptions = InvocationTargetException.class)
    public void internalExceptionTest() throws Exception {
        subscriber.notifySubscriber(new Exception());
    }
    
    @Test(expectedExceptions = NoSuchMethodException.class)
    public void notifyNoMethodTest() throws Exception {
        subscriber.notifySubscriber(new Boolean(true));
    }
    
    @Test
    public void notifySilentlyTest() throws Exception {
        assertNull(subscriber.notifySubscriberSilently(new Integer(2)));
        assertNull(subscriber.notifySubscriberSilently(new Double(3.4)));
    }
    
    @Test
    public void internalExceptionSilentlyTest() {
        assertEquals(subscriber.notifySubscriberSilently(new Exception()).getClass(), InvocationTargetException.class);
    }
    
    @Test
    public void internalInterruptedExceptionSilentlyTest() {
        InterruptedException cause = new InterruptedException();
        Object result = subscriber.notifySubscriberSilently(cause);
        assertEquals(result.getClass(), InvocationTargetException.class);
        assertEquals(((InvocationTargetException) result).getCause(), cause); 
    }
    
    @Test
    public void notifyNoMethodSilentlyTest() throws Exception {
        assertNull(subscriber.notifySubscriberSilently(new Boolean(false)));
    }
}
