package org.jrivets.util.container;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.jrivets.util.testing.MockUtils;
import org.junit.Test;

public class AbstractKeyValueHolderTest {

    private static class KeyValueHolderTest extends AbstractKeyValueHolder<Long, Long> {

        private Map<Long, Long> values = new HashMap<Long, Long>();
        
        private final Long delayedKey;
        
        private final long delay;
        
        public KeyValueHolderTest(long expirationTimeout) {
            this(expirationTimeout, 0L, 0L);
        }
        
        public KeyValueHolderTest(long expirationTimeout, long delayedKey, long delay) {
            super(expirationTimeout);
            this.delayedKey = delayedKey;
            this.delay = delay;
        }

        @Override
        protected Long getNewValue(Long key) {
            Long value = values.get(key);
            if (value == null) {
                if (key == delayedKey) {
                    try { Thread.sleep(delay); } catch (InterruptedException e) {};
                }
                value = key*1000L;
            }
            value++;
            values.put(key, value);
            return value;
        }
        
    }
    
    @Test
    public void testGet() {
        KeyValueHolderTest holder = new KeyValueHolderTest(1000L);
        assertTrue(holder.getValue(1L) == 1001L);
        assertTrue(holder.getValue(10L) == 10001L);
    }
    
    @Test
    public void testNoSweep() {
        KeyValueHolderTest holder = new KeyValueHolderTest(100L);
        long start = System.currentTimeMillis();
        assertTrue(holder.getValue(1L) == 1001L);
        while(System.currentTimeMillis() - start < 10L);
        assertTrue(holder.getValue(1L) == 1001L);
        while(System.currentTimeMillis() - start < 150L);
        assertTrue(holder.getValue(1L) == 1002L);        
    }

    @Test
    public void testSweep() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(100L);
        assertTrue(holder.getValue(1L) == 1001L);
        assertTrue(holder.getValue(2L) == 2001L);
        Thread.sleep(150L);
        assertTrue(holder.getValue(1L) == 1002L);
        assertTrue(holder.getValue(2L) == 2002L);
    }
    
    @Test
    public void noKeepForTimeout() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(200L);
        long newVal = System.currentTimeMillis() + 200L;
        assertTrue(holder.getValue(1L) == 1001L);
        while (System.currentTimeMillis() < newVal + 200L) {
            if (holder.getValue(1L) == 1002L) {
                assertTrue(System.currentTimeMillis() >= newVal);
                return;
            }
        }
        fail("Should keep value for 200ms only");
    }
    
    @Test
    public void keepForTimeout() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(100L);
        MockUtils.setFieldValue(AbstractKeyValueHolder.class, holder, "timeoutAfterLastTouch", true);
        long newVal = System.currentTimeMillis() + 200L;
        while (System.currentTimeMillis() < newVal) {
            assertTrue(holder.getValue(1L) == 1001L);
        }
    }
    
    @Test
    public void copyCollection() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(100L);
        assertNotNull(holder.getCopyCollection());
        assertEquals(0, holder.getCopyCollection().size());
    }
    
    @Test
    public void copyCollection2() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(100L);
        assertTrue(holder.getValue(1L) == 1001L);
        assertTrue(holder.getValue(2L) == 2001L);
        Map<Long, Long> collection = holder.getCopyCollection();
        assertEquals(2, collection.size());
        assertEquals(new Long(1001), collection.get(1L));
        assertEquals(new Long(2001), collection.get(2L));
    }
    
    @Test
    public void testAccess() throws InterruptedException {
        final KeyValueHolderTest holder = new KeyValueHolderTest(500L, 2L, 250L);
        assertTrue(holder.getValue(1L) == 1001L);
         
        class OtherThread implements Runnable {

            volatile boolean running = true;

            public void run() {
                try {
                    holder.getValue(2L);
                } finally {
                    running = false;
                }
            }
            
        }
        OtherThread ot = new OtherThread();
        Thread t = new Thread(ot);
        
        Thread.sleep(500L);
        assertTrue(holder.getValue(1L) == 1002L);
        t.start();
        int count = 0;
        while (ot.running) {
            assertTrue(holder.getValue(1L) == 1002L);
            count++;
        }
        assertTrue(holder.getValue(2L) == 2001L);
        assertTrue(count > 5);
        t.join();
    }
    
    @Test
    public void testMaxSize() throws InterruptedException {
        KeyValueHolderTest holder = new KeyValueHolderTest(1000L);
        MockUtils.setFieldValue(AbstractKeyValueHolder.class, holder, "maxSize", 1);
        assertTrue(holder.getValue(1L) == 1001L);
        assertTrue(holder.getValue(2L) == 2001L);
        assertTrue(holder.getValue(1L) == 1001L);
        assertTrue(holder.getValue(2L) == 2002L);
        assertTrue(holder.getValue(2L) == 2003L);        
    }
    
}
