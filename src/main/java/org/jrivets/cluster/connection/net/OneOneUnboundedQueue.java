package org.jrivets.cluster.connection.net;

import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * One Writer One Reader unbounded queue. 
 * 
 *  @author Dmitry Spasibenko 
 *
 * @param <E>
 */
final class OneOneUnboundedQueue<E>  {
    
    private final LinkedList<E[]> chunks = new LinkedList<E[]>();
    
    private final int chunkSize;
    
    private E[] headChunk;
    
    private int head;
    
    private E[] tailChunk;
    
    private int tail;

    OneOneUnboundedQueue() {
        this(1024);
    }
    
    OneOneUnboundedQueue(int chunkSize) {
        this.chunkSize = chunkSize;
        newTailChunk();
        this.headChunk = this.tailChunk;
    }
    
    /**
     * not 1/1 safe
     */
    int size() {
        if (tailChunk == headChunk) {
            return tail-head;
        }
        return chunkSize - head + tail + chunkSize*(chunks.size()-2);
    }

    /**
     * 1/1 safe.
     */
    boolean isEmpty() {
        return tailChunk == headChunk && head == tail;
    }

    boolean add(E e) {
        return offer(e);
    }

    /**
     * 1/1 safe.
     */
    boolean offer(E e) {
        tailChunk[tail++] = e;
        if (tail == chunkSize) {
            newTailChunk();
        }
        return true;
    }

    /**
     * 1/1 safe.
     */
    E remove() {
        assertSizeIsNotZero();
        E result = headChunk[head];
        headChunk[head++] = null;
        if (head == chunkSize) {
            advanceHeadChunk();
        }
        return result;
    }

    E poll() {
        if (isEmpty()) {
            return null;
        }
        return pollInternal();
    }
    
    private E pollInternal() {
        E result = headChunk[head];
        headChunk[head++] = null;
        if (head == chunkSize) {
            head = 0;
            chunks.remove();
            headChunk = chunks.peek();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private synchronized void newTailChunk() {
        E[] a = (E[]) new Object[chunkSize];
        chunks.add(a);
        tailChunk = a;
        tail = 0;
    }
    
    private synchronized void advanceHeadChunk() {
        chunks.remove();
        headChunk = chunks.peek();
        head = 0;;
    }
    
    private void assertSizeIsNotZero() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
    }
}
