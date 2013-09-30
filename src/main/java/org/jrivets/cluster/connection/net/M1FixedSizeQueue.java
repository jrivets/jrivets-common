package org.jrivets.cluster.connection.net;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.collection.RingBuffer;

final class M1FixedSizeQueue<T> {
    
    private final RingBuffer<T> buffer;
    
    private final Lock lock = new ReentrantLock();
    
    private final Condition notFull = lock.newCondition();
    
    private volatile int rVersion;
    
    private volatile int wVersion;

    M1FixedSizeQueue(int capacity) {
        this.buffer = new RingBuffer<T>(capacity);
    }
    
    /**
     * Retrieves and removes the head of this queue, or null if the queue is empty.
     */
    T poll() {
        T result = buffer.poll(); 
        if (rVersion != wVersion) {
            notifyNotFull();
        }
        return result;
    }
    
    void put(T element, Runnable kickReader) throws InterruptedException {
        lock.lock();
        try {
            while (isFull()) {
                wVersion++;
                kickReader.run();
                notFull.await();
            }
            buffer.offer(element);
            kickReader.run();            
        } finally {
            lock.unlock();
        }
    }
    
    boolean offer(T element) {
        lock.lock();
        try {
            return buffer.offer(element);
        } finally {
            lock.unlock();
        }
    }
     
    int size() {
        return buffer.size();
    }
    
    private boolean isFull() {
        return buffer.size() == buffer.capacity();
    }
    
    private void notifyNotFull() {
        lock.lock();
        try {
            rVersion = wVersion;
            notFull.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public String toString() {
        return "{size=" + buffer.size() + ", capacity=" + buffer.capacity() + "}";
    }
}
