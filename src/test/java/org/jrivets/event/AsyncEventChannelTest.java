package org.jrivets.event;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AsyncEventChannelTest {

    static class A {
        
        int i;
        
        @OnEvent
        synchronized void onEvent(Integer i) {
            this.i = i;
            notify();
        }
    }
    
    static class AsyncEventChannelExt extends AsyncEventChannel {

        volatile Object o;
        
        public AsyncEventChannelExt(Executor executor) {
            super(executor);
        }
        
        @Override
        protected void onNoSubscribers(Object o) {
            this.o = o;
        }
        
    }
    
    AsyncEventChannelExt ec = new AsyncEventChannelExt(Executors.newFixedThreadPool(1));
    
    @Test(timeOut=1000L)
    public void waitNotificationTest() throws InterruptedException {
        A a = new A();
        ec.addSubscriber(a);
        synchronized (a) {
            ec.publish(23);
            a.wait();
        }
        assertEquals(a.i, 23);
    }
    
    @Test(timeOut=1000L)
    public void onNoSubscribersTest() throws InterruptedException {
        A a = new A();
        ec.addSubscriber(a);
        String msg = "Hello";
        ec.publish(msg);
        while (ec.o == null) {
            Thread.yield();
        }
        assertEquals(ec.o, msg);
    }
    
}
