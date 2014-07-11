package org.jrivets.util.container;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SequentialBucketBufferCounterTest {

    @Test(expectedExceptions={IllegalArgumentException.class})
    public void wrongBucketNumberTest() {
        SequentialBucketBufferCounter bc = new SequentialBucketBufferCounter(2);
        bc.add(10, 21);
        bc.add(9, 2);
    }
    
    @Test
    public void counterTest() {
        SequentialBucketBufferCounter bc = new SequentialBucketBufferCounter(2);
        bc.add(-9, 1);
        assertEquals(bc.getTotalCount(), 1);
        bc.add(-9, 1);
        assertEquals(bc.getTotalCount(), 2);
        bc.add(-8, 1);
        assertEquals(bc.getTotalCount(), 3);
        bc.add(-7, 1);
        assertEquals(bc.getTotalCount(), 2);
        bc.add(10, 10);
        assertEquals(bc.getTotalCount(), 10);
    }
    
    @Test
    public void neverExceedTest() {
        SequentialBucketBufferCounter bc = new SequentialBucketBufferCounter(10);
        for (int i = 0; i < 10; i++) {
            bc.add(i, 1);
            assertEquals(bc.getTotalCount(), i + 1);
        }

        for (int i = 10; i < 100; i++) {
            bc.add(i, 1);
            assertEquals(bc.getTotalCount(), 10);
        }
    }
    
}
