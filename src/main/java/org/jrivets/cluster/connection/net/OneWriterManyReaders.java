package org.jrivets.cluster.connection.net;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.collection.RingBuffer;

final class OneWriterManyReaders<T> {

    private final RingBuffer<T> buffer;

    private final Lock lock = new ReentrantLock();

    private final Condition notFull = lock.newCondition();

    private final Condition notEmpty = lock.newCondition();

    private volatile int readers;

    private volatile int writers;

    private volatile int interrupts;

    OneWriterManyReaders(int capacity) {
        buffer = new RingBuffer<T>(capacity);
    }

    void put(T element) throws InterruptedException {
        if (isFull()) {
            waitNotFull();
        }
        buffer.add(element);
        signalNotEmpty();
    }

    T removeLast(long time, TimeUnit unit) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            readers++;
            long currentTime = System.currentTimeMillis();
            long stopTime = time > 0 ? currentTime + unit.toMillis(time) : Long.MAX_VALUE;
            while (buffer.isEmpty() && currentTime < stopTime) {
                notEmpty.await(stopTime - currentTime, TimeUnit.MILLISECONDS);
                checkInterrupted();
                currentTime = System.currentTimeMillis();
            }
            if (buffer.isEmpty()) {
                return null;
            }
            if (isFull() && writers > 0) {
                notFull.signal();
            }
            return buffer.removeFirst();
        } finally {
            if (--readers > 0 && !buffer.isEmpty()) {
                notEmpty.signal();
            }
            lock.unlock();
        }
    }

    /**
     * Causes all blocked treads in put() or removeLast() operations are
     * finished immediately after the call throwing InterruptedException to
     * their invokers.
     */
    void interrupt() {
        lock.lock();
        try {
            interrupts = readers + writers;
            if (readers > 0) {
                notEmpty.signalAll();
            }
            if (writers > 0) {
                notFull.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    int size() {
        return buffer.size();
    }

    private void checkInterrupted() throws InterruptedException {
        if (interrupts > 0) {
            interrupts--;
            throw new InterruptedException("Interrupted by waiting event in " + this.getClass());
        }
    }

    private void signalNotEmpty() {
        if (readers == 0) {
            return;
        }
        lock.lock();
        try {
            notEmpty.notify();
        } finally {
            lock.unlock();
        }
    }

    private void waitNotFull() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            ++writers;
            while (isFull()) {
                notFull.await();
                checkInterrupted();
            }
        } finally {
            --writers;
            lock.unlock();
        }
    }

    private boolean isFull() {
        return buffer.size() == buffer.capacity();
    }
}
