package org.jrivets.mq;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.util.OverflowException;

final class InMemoryFifoQueue implements MessageQueue {

    final int maxSize;
    
    int size;
    
    final Lock lock = new ReentrantLock();
    
    final Condition cond = lock.newCondition();
    
    ValueHolder head;
    
    ValueHolder tail;
    
    int readers;
    
    boolean terminated;
        
    private static class ValueHolder {
        
        Object value;
        
        ValueHolder next;
        
        ValueHolder(Object value) {
            this.value = value;
        }
        
        void clear() {
            value = null;
            next = null;
        }
    }

    InMemoryFifoQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public <T> void put(T value) throws OverflowException {
        checkWrite();
        lock.lock();
        try {
            checkWrite();
            putInternal(value);
        } finally {
            notifyReaders();
            lock.unlock();
        }
    }

    @Override
    public Object get(long timeoutMs) {
        long stopTime = timeoutMs > 0L ? System.currentTimeMillis() + timeoutMs : 0L;
        lock.lock();
        ++readers;
        try {
            return getInternal(stopTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            --readers;
            notifyReaders();
            lock.unlock();
        }
    }
    
    @Override
    public void terminate() {
        lock.lock();
        try {
            terminated = true;
            clean();
            if (readers > 0) {
                cond.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private Object getInternal(long stopTime) throws InterruptedException {
        do {
            if (terminated) {
                break;
            }
            if (head != null) {
                ValueHolder holder = head;
                size--;
                Object value = holder.value;
                head = head.next;
                if (head == null) {
                    tail = null;
                }
                holder.clear();
                return value;
            }
            if (stopTime > 0L) {
                cond.await(stopTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            }
        } while (stopTime > 0L && stopTime > System.currentTimeMillis());
        return null;
    }
    
    private void putInternal(Object value) {
        ValueHolder holder = new ValueHolder(value);
        size++;
        if (head == null) {
            tail = head = holder;
            return;
        }
        
        tail.next = holder;
        tail = holder;
    }
    
    private void clean() {
        while (head != null) {
            ValueHolder holder = head;
            head = head.next;
            holder.value = null;
            holder.next = null;
        }
        head = null;
        tail = null;
        size = 0;
    }

    private void notifyReaders() {
        if (readers > 0 && size > 0) {
            cond.signal();
        }
    }
    
    private void checkWrite() throws OverflowException {
        if (size >= maxSize) {
            throw new OverflowException("The size=" + size + " cannot be greater than " + maxSize);
        }
        if (terminated) {
            throw new IllegalStateException("The queue is terminated");
        }
    }
    
    @Override
    public String toString() {
        return "InMemoryFifoQueue {maxSize=" + maxSize + ", size=" + size + ", readers=" + readers + "}";
    }

}
