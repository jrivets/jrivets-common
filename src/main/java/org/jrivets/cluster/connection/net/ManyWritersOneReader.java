package org.jrivets.cluster.connection.net;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.collection.RingBuffer;

final class ManyWritersOneReader<T> {
    
    private final RingBuffer<T> buffer;
    
    private final Lock lock = new ReentrantLock();
    
    private final Condition notFull = lock.newCondition();
    
    private volatile int blockedWriters;

    ManyWritersOneReader(int capacity) {
        this.buffer = new RingBuffer<T>(capacity);
    }
    
    /**
     * Retrieves and removes the head of this queue, or null if the queue is empty.
     */
    T poll() {
        boolean isFull = isFull();
        T result = buffer.size() == 0 ? null : buffer.removeFirst(); 
        if (isFull && blockedWriters > 0) {
            notifyNotFull();
        }
        return result;
    }
    
    void put(T element) throws InterruptedException {
        lock.lock();
        try {
            blockedWriters++;
            while (isFull()) {
                notFull.await();
            }
            buffer.add(element);
        } finally {
            --blockedWriters;
            lock.unlock();
        }
    }
    
    private boolean isFull() {
        return buffer.size() == buffer.capacity();
    }
    
    private void notifyNotFull() {
        lock.lock();
        try {
            notFull.signal();
        } finally {
            lock.unlock();
        }
    }
}
