package org.jrivets.mq;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jrivets.util.OverflowException;
import org.testng.annotations.Test;

public class InMemoryQueueTest {
    
    @Test
    public void putGetPutTest() throws OverflowException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        assertEquals(q.size, 0L);
        q.put(new byte[10]);
        assertEquals(q.size, 1L);
        q.get(0L);
        assertEquals(q.size, 0L);
        assertNull(q.head);
        assertNull(q.tail);
        q.put(new byte[10]);
        assertEquals(q.size, 1);
        assertEquals(q.head, q.tail);
    }
    
    @Test(expectedExceptions={OverflowException.class})
    public void maxSizeTest() throws OverflowException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(1);
        q.put(new Object());
        assertEquals(q.size, 1);
        q.put(new Object());
    }
    
    @Test
    public void fifoTest() throws OverflowException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        byte[] msg1 = new byte[10];
        byte[] msg2 = new byte[10];
        q.put(msg1);
        q.put(msg2);
        assertEquals(q.get(0L), msg1);
        assertEquals(q.get(0L), msg2);
        assertEquals(q.size, 0L);
        assertNull(q.get(0L));
    }
    
    @Test
    public void getTimeoutTest() throws OverflowException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        long start = System.currentTimeMillis();
        assertNull(q.get(50L));
        assertTrue(System.currentTimeMillis() - start >= 50L);
    }
    
    @Test(timeOut=5000)
    public void blockReaderTest() throws OverflowException, InterruptedException {
        final InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        final AtomicInteger ai = new AtomicInteger();
        Thread t = new Thread(new Runnable(){
            public void run() {
                ai.addAndGet(((byte[]) q.get(10000L)).length);
            };
        });
        t.start();
        while (q.readers < 1) {
            Thread.yield();
        }
        q.put(new byte[13]);
        t.join();
        assertEquals(ai.get(), 13);
        assertEquals(0, q.readers);
        assertEquals(q.size, 0L);
    }
    
    @Test(timeOut=5000)
    public void getInterruptedTest() throws OverflowException, InterruptedException {
        final InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        final AtomicBoolean ab = new AtomicBoolean();
        Thread t = new Thread(new Runnable(){
            public void run() {
                q.get(10000L);
                ab.set(Thread.currentThread().isInterrupted());
            };
        });
        t.start();
        while (q.readers < 1) {
            Thread.yield();
        }
        t.interrupt();
        t.join();
        assertEquals(0, q.readers);
        assertTrue(ab.get());
    }
    
    @Test(timeOut=5000)
    public void getTerminatedTest() throws OverflowException, InterruptedException {
        final InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        final AtomicBoolean ab = new AtomicBoolean(true);
        Thread t = new Thread(new Runnable(){
            public void run() {
                q.get(10000L);
                ab.set(Thread.currentThread().isInterrupted());
            };
        });
        assertTrue(ab.get());
        t.start();
        while (q.readers < 1) {
            Thread.yield();
        }
        q.terminate();
        t.join();
        assertEquals(0, q.readers);
        assertFalse(ab.get());
    }
    
    @Test(timeOut=5000)
    public void terminateTest() throws OverflowException, InterruptedException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        q.put(new byte[11]);
        q.put(new byte[4]);
        assertEquals(q.size, 2);
        assertNotNull(q.head);
        assertNotNull(q.tail);
        q.terminate();
        assertEquals(q.size, 0);
        assertNull(q.head);
        assertNull(q.tail);
        assertNull(q.get(100000L));
    }
    
    @Test(timeOut=5000, expectedExceptions={IllegalStateException.class})
    public void putThrowsTest() throws OverflowException, InterruptedException {
        InMemoryFifoQueue q = new InMemoryFifoQueue(20);
        q.terminate();
        q.put(new byte[11]);
    }
}
