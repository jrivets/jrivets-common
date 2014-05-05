package org.jrivets.collection;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jrivets.collection.RingBuffer;
import org.testng.annotations.Test;

public class RingBufferTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void zeroCapacity() {
        @SuppressWarnings("unused")
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void negativeCapacity() {
        @SuppressWarnings("unused")
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(-10);
    }

    @Test(expectedExceptions=IllegalStateException.class)
    public void addTest() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(1);
        assertTrue(ringBuffer.add(1));
        ringBuffer.add(2);
    }
    
    @Test
    public void offerTest() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(1);
        assertTrue(ringBuffer.offer(1));
        assertFalse(ringBuffer.offer(2));
        assertFalse(ringBuffer.offer(3));
        assertEquals(1, ringBuffer.size());
    }

    @Test
    public void addTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(3);
        assertTrue(ringBuffer.add(1));
        assertTrue(ringBuffer.add(2));
        assertEquals(2, ringBuffer.size());
        assertTrue(ringBuffer.add(3));
        assertFalse(ringBuffer.offer(4));
        assertEquals(3, ringBuffer.size());
        assertEquals(3, ringBuffer.capacity());
    }
    
    @Test
    public void offerTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(3);
        assertTrue(ringBuffer.offer(1));
        assertTrue(ringBuffer.offer(2));
        assertEquals(2, ringBuffer.size());
        assertTrue(ringBuffer.offer(3));
        assertFalse(ringBuffer.offer(4));
        assertEquals(3, ringBuffer.size());
        assertEquals(3, ringBuffer.capacity());
    }
    
    @Test
    public void addCircular() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        assertTrue(ringBuffer.add(1));
        assertTrue(ringBuffer.add(2));
        for (int i = 3; i < 10; i++) {
            assertEquals(2, ringBuffer.size());
            assertEquals(new Integer(i - 2), ringBuffer.remove());
            assertEquals(1, ringBuffer.size());
            assertEquals(new Integer(i - 1), ringBuffer.element());
            assertEquals(new Integer(i - 1), ringBuffer.last());
            ringBuffer.add(i);
            assertEquals(new Integer(i - 1), ringBuffer.element());
            assertEquals(new Integer(i), ringBuffer.last());
        }
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void firstTest() {
        new RingBuffer<Integer>(3).element();
    }

    @Test
    public void firstTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.offer(3);
        assertTrue(ringBuffer.element() == 1);
        assertTrue(ringBuffer.size() == 2);
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void lastTest() {
        new RingBuffer<Integer>(3).last();
    }
    
    @Test
    public void peekLastTest() {
        assertNull(new RingBuffer<Integer>(3).peekLast());
    }

    @Test
    public void lastTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.offer(1);
        ringBuffer.add(2);
        ringBuffer.offer(3);
        assertTrue(ringBuffer.last() == 2);
        assertTrue(ringBuffer.peekLast() == 2);
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void remove() {
        new RingBuffer<Integer>(3).remove();
    }

    @Test
    public void remove2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.offer(3);
        ringBuffer.remove();
        assertTrue(ringBuffer.last() == 2);
        assertTrue(ringBuffer.element() == 2);
        assertTrue(ringBuffer.size() == 1);
    }
    
    @Test
    public void peek() {
        assertNull(new RingBuffer<Integer>(3).peek());
    }
    
    @Test
    public void peek2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.offer(3);
        assertEquals(ringBuffer.peek(), new Integer(1));
        assertEquals(ringBuffer.last(), new Integer(2));
        assertEquals(ringBuffer.element(), new Integer(1));
        assertEquals(ringBuffer.size(), 2);
    }
    
    @Test
    public void pool() {
        assertNull(new RingBuffer<Integer>(3).poll());
    }
    
    @Test
    public void poll2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.offer(3);
        assertEquals(ringBuffer.poll(), new Integer(1));
        assertEquals(ringBuffer.last(), new Integer(2));
        assertEquals(ringBuffer.element(), new Integer(2));
        assertEquals(ringBuffer.size(), 1);
    }


    @Test
    public void equalsTest() {
        RingBuffer<Integer> ringBuffer1 = new RingBuffer<Integer>(2);
        RingBuffer<Integer> ringBuffer2 = new RingBuffer<Integer>(20);
        assertEquals(ringBuffer1, ringBuffer2);
        assertEquals(ringBuffer1.hashCode(), ringBuffer2.hashCode());
    }
    
    @Test
    public void equalsTest2() {
        RingBuffer<Integer> ringBuffer1 = new RingBuffer<Integer>(2);
        RingBuffer<Integer> ringBuffer2 = new RingBuffer<Integer>(20);
        ringBuffer1.add(1);
        ringBuffer1.add(2);
        ringBuffer2.add(1);
        ringBuffer2.add(2);
        assertEquals(ringBuffer1, ringBuffer2);
        assertEquals(ringBuffer1.hashCode(), ringBuffer2.hashCode());
    }
    
    @Test
    public void equalsTest3() {
        RingBuffer<Integer> ringBuffer1 = new RingBuffer<Integer>(2);
        RingBuffer<Integer> ringBuffer2 = new RingBuffer<Integer>(20);
        ringBuffer1.add(1);
        ringBuffer1.add(2);
        ringBuffer2.add(2);
        ringBuffer2.add(1);
        assertFalse(ringBuffer1.equals(ringBuffer2));
        // most probably they are different:
        assertTrue(ringBuffer1.hashCode() != ringBuffer2.hashCode());
    }
    
    @Test
    public void iterator() {
        RingBuffer<Integer> ringBuffer1 = new RingBuffer<Integer>(2);
        Iterator<Integer> it = ringBuffer1.iterator();
        assertFalse(it.hasNext());
        ringBuffer1.add(1);
        it = ringBuffer1.iterator();
        assertTrue(it.hasNext());
        assertEquals(new Integer(1), it.next());
        assertFalse(it.hasNext());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(3);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        ringBuffer.remove();
        
        assertTrue(ringBuffer.size() == 2);
        assertEquals(new Integer(2), ringBuffer.element());
        assertEquals(new Integer(3), ringBuffer.last());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(ringBuffer);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        RingBuffer<Integer> readBuffer = (RingBuffer<Integer>) ois.readObject();
        assertTrue(readBuffer.size() == 2);
        assertEquals(new Integer(2), readBuffer.element());
        assertEquals(new Integer(3), readBuffer.last());
    }
}
