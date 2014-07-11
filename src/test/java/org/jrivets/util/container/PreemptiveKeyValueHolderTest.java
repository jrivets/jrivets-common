package org.jrivets.util.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PreemptiveKeyValueHolderTest {

    private List<Integer> requests = new ArrayList<Integer>();
    
    private class PreemptiveHolderImpl extends PreemptiveKeyValueHodler<Integer, Integer> {

        protected PreemptiveHolderImpl(long expirationTimeoutMs, int maxSize) {
            super(expirationTimeoutMs, maxSize);
        }

        @Override
        protected Integer getNewValue(Integer key) {
            requests.add(key);
            return key;
        }
        
    }
    
    @BeforeMethod
    private void init() {
        requests.clear();
    }
    
    @Test
    public void getTest() {
        PreemptiveHolderImpl ph = new PreemptiveHolderImpl(Long.MAX_VALUE, 1);
        assertEquals(ph.getValue(1), new Integer(1));
        assertEquals(ph.getValue(1), new Integer(1));
        assertEquals(ph.getValue(1), new Integer(1));
        
        assertEquals(requests.size(), 1);
        assertEquals(requests.get(0), new Integer(1));
    }
    
    @Test
    public void preemptiveTest() {
        PreemptiveHolderImpl ph = new PreemptiveHolderImpl(Long.MAX_VALUE, 1);
        assertEquals(ph.getValue(1), new Integer(1));
        assertEquals(ph.getValue(2), new Integer(2));
        assertEquals(ph.getValue(1), new Integer(1));
        
        assertEquals(requests.size(), 3);
        assertEquals(requests, Arrays.asList(1, 2, 1));
    }
    
    @Test
    public void preemptiveTest2() throws InterruptedException {
        PreemptiveHolderImpl ph = new PreemptiveHolderImpl(Long.MAX_VALUE, 2);
        assertEquals(ph.getValue(1), new Integer(1));
        Thread.sleep(1L); // allow access time be different for different elements
        assertEquals(ph.getValue(2), new Integer(2));
        Thread.sleep(1L);
        assertEquals(ph.getValue(3), new Integer(3));
        Thread.sleep(1L);
        assertEquals(ph.getValue(1), new Integer(1));
        
        assertEquals(requests.size(), 4);
        assertEquals(requests, Arrays.asList(1, 2, 3, 1));
    }
    
    @Test
    public void preemptiveTest3() throws InterruptedException {
        PreemptiveHolderImpl ph = new PreemptiveHolderImpl(Long.MAX_VALUE, 2);
        assertEquals(ph.getValue(1), new Integer(1));
        Thread.sleep(1L);
        assertEquals(ph.getValue(2), new Integer(2));
        Thread.sleep(1L);
        assertEquals(ph.getValue(1), new Integer(1));
        Thread.sleep(1L);
        assertEquals(ph.getValue(3), new Integer(3));
        Thread.sleep(1L);
        assertEquals(ph.getValue(1), new Integer(1));
        Thread.sleep(1L);
        assertEquals(ph.getValue(2), new Integer(2));
        
        assertEquals(requests.size(), 4);
        assertEquals(requests, Arrays.asList(1, 2, 3, 2));
    }

}
