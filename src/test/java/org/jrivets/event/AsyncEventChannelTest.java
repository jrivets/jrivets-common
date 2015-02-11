package org.jrivets.event;

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
    
    @Test(timeOut=1000L)
    public void waitTest() throws InterruptedException {
        A a = new A();
        AsyncEventChannel ec = new AsyncEventChannel(Executors.newFixedThreadPool(1));
        ec.addSubscriber(a);
        synchronized (a) {
            ec.publish(23);
            a.wait();
        }
        assertEquals(a.i, 23);
    }
    
}
