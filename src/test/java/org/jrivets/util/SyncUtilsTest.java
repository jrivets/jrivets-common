package org.jrivets.util;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*; 

public class SyncUtilsTest {

    @BeforeMethod
    public void init() {
        Thread.interrupted(); // clear the flag just in case
    }
    
    @Test(timeOut=1000L)
    public void timeProcessQuietlyTest() {
        assertTrue(SyncUtils.timeProcessQuietly(Thread::sleep, 10L));
    }
    
    @Test(timeOut=1000L)
    public void timeProcessQuietlyInterruptedTest() {
        final Thread t = Thread.currentThread();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SyncUtils.sleepQuietly(10L);
                t.interrupt();
            }
        }).start();
        assertFalse(SyncUtils.timeProcessQuietly(Thread::sleep, 100000000L));
    }
    
    @Test(timeOut=1000L)
    public void sleepQuietlyInterruptedBerofTest() {
        Thread.currentThread().interrupt();
        assertFalse(SyncUtils.timeProcessQuietly(Thread::sleep, 100000000L));
    }
    
}
