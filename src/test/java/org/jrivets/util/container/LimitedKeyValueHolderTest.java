package org.jrivets.util.container;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

public class LimitedKeyValueHolderTest {

    class TestedHolder extends LimitedKeyValueHolder<String, String> {

        List<String> deleted = new ArrayList<String>();

        public TestedHolder(long timeout, int maxSize, boolean timeoutAfterLastTouch, boolean notifyWhenReplace) {
            super(timeout, maxSize, timeoutAfterLastTouch, notifyWhenReplace);
        }
        
        public TestedHolder(long timeout, int maxSize, boolean timeoutAfterLastTouch) {
            super(timeout, maxSize, timeoutAfterLastTouch, true);
        }

        @Override
        protected void onRemove(Holder holder) {
            if (holder == null) {
                return;
            }
            deleted.add(holder.key);
        }
    }

    @Test
    public void sweepTest() throws InterruptedException {
        TestedHolder h = new TestedHolder(50L, 1, false);
        h.put("A", "B");
        assertEquals("B", h.get("A"));
        assertEquals("B", h.get("A"));
        Thread.sleep(20L);
        assertEquals("B", h.get("A"));
        Thread.sleep(30L);
        assertNull(h.get("A"));
        assertEquals(h.deleted.get(0), "A");

        h = new TestedHolder(50L, 1, true);
        h.put("A", "B");
        assertEquals("B", h.get("A"));
        assertEquals("B", h.get("A"));
        Thread.sleep(20L);
        assertEquals("B", h.get("A"));
        Thread.sleep(30L);
        assertEquals("B", h.get("A"));
        assertEquals(h.deleted.size(), 0);

        h = new TestedHolder(5L, 3, true);
        h.put("A", "B");
        h.put("B", "B");
        h.put("C", "B");
        Thread.sleep(10L);
        assertEquals(h.deleted.size(), 0);
        assertNull(h.get("A"));
        assertEquals(h.deleted.size(), 3);
        assertTrue(h.deleted.contains("A"));
        assertTrue(h.deleted.contains("B"));
        assertTrue(h.deleted.contains("C"));
    }

    @Test
    public void sortTest() throws InterruptedException {
        TestedHolder h = new TestedHolder(500000L, 2, true);
        h.put("A", "1");
        Thread.sleep(1L);
        h.put("B", "2");
        Thread.sleep(1L);
        h.put("C", "3");
        assertNull(h.get("A"));
        assertEquals(h.deleted.get(0), "A");
        assertEquals("2", h.get("B"));
        assertEquals("3", h.get("C"));

        assertEquals("B", h.sortedSet.first().key);
        h.get("B");
        assertEquals("C", h.sortedSet.first().key);
    }

    @Test
    public void clearTest() {
        TestedHolder h = new TestedHolder(50000L, 3, true);
        h.put("A", "B");
        h.put("B", "B");
        h.put("C", "B");
        assertEquals(h.deleted.size(), 0);
        h.clear();
        assertEquals(h.deleted.size(), 3);
        assertTrue(h.deleted.contains("A"));
        assertTrue(h.deleted.contains("B"));
        assertTrue(h.deleted.contains("C"));
    }
    
    @Test
    public void removeTest() {
        TestedHolder h = new TestedHolder(50000L, 3, true);
        h.put("A", "B");
        assertEquals(h.deleted.size(), 0);
        h.remove("B");
        assertNotNull(h.get("A"));
        assertEquals(h.deleted.size(), 0);
        h.remove("A");
        assertNull(h.get("A"));
        assertEquals(1, h.deleted.size());
        assertTrue(h.deleted.contains("A"));
    }
    
    @Test
    public void notifyWhenReplaceTest() {
        TestedHolder h = new TestedHolder(500L, 1, false, true);
        h.put("A", "B");
        h.put("A", "B");
        assertEquals(1, h.deleted.size());
        assertTrue(h.deleted.contains("A"));
        
        h = new TestedHolder(50L, 1, false, false);
        h.put("A", "B");
        h.put("A", "B");
        assertEquals(0, h.deleted.size());
        h.put("B", "C");
        assertEquals(1, h.deleted.size());
        assertTrue(h.deleted.contains("A"));
    }
}
