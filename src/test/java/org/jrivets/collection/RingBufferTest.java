package org.jrivets.collection;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jrivets.collection.RingBuffer;
import org.junit.Test;

public class RingBufferTest {

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacity() {
        @SuppressWarnings("unused")
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCapacity() {
        @SuppressWarnings("unused")
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(-10);
    }

    @Test
    public void addTest() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(1);
        assertTrue(ringBuffer.add(1));
        assertFalse(ringBuffer.add(2));
        assertFalse(ringBuffer.add(3));
        assertEquals(1, ringBuffer.size());
    }

    @Test
    public void addTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(3);
        assertTrue(ringBuffer.add(1));
        assertTrue(ringBuffer.add(2));
        assertEquals(2, ringBuffer.size());
        assertTrue(ringBuffer.add(3));
        assertFalse(ringBuffer.add(4));
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
            assertEquals(new Integer(i - 2), ringBuffer.removeFirst());
            assertEquals(1, ringBuffer.size());
            assertEquals(new Integer(i - 1), ringBuffer.first());
            assertEquals(new Integer(i - 1), ringBuffer.last());
            ringBuffer.add(i);
            assertEquals(new Integer(i - 1), ringBuffer.first());
            assertEquals(new Integer(i), ringBuffer.last());
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void firstTest() {
        new RingBuffer<Integer>(3).first();
    }

    @Test
    public void firstTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        assertTrue(ringBuffer.first() == 1);
        assertTrue(ringBuffer.size() == 2);
    }

    @Test(expected = NoSuchElementException.class)
    public void lastTest() {
        new RingBuffer<Integer>(3).last();
    }

    @Test
    public void lastTest2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        assertTrue(ringBuffer.last() == 2);
    }

    @Test(expected = NoSuchElementException.class)
    public void removeFirst() {
        new RingBuffer<Integer>(3).removeFirst();
    }

    @Test
    public void removeFirst2() {
        RingBuffer<Integer> ringBuffer = new RingBuffer<Integer>(2);
        ringBuffer.add(1);
        ringBuffer.add(2);
        ringBuffer.add(3);
        ringBuffer.removeFirst();
        assertTrue(ringBuffer.last() == 2);
        assertTrue(ringBuffer.first() == 2);
        assertTrue(ringBuffer.size() == 1);
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
        ringBuffer.removeFirst();
        
        assertTrue(ringBuffer.size() == 2);
        assertEquals(new Integer(2), ringBuffer.first());
        assertEquals(new Integer(3), ringBuffer.last());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(ringBuffer);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        RingBuffer<Integer> readBuffer = (RingBuffer<Integer>) ois.readObject();
        assertTrue(readBuffer.size() == 2);
        assertEquals(new Integer(2), readBuffer.first());
        assertEquals(new Integer(3), readBuffer.last());
    }
}
