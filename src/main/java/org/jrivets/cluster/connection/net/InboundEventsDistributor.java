package org.jrivets.cluster.connection.net;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class InboundEventsDistributor {
    
    private final Logger logger = LoggerFactory.getLogger(InboundEventsDistributor.class); 

    private final ExecutorService executor;

    private final int minWorkers;

    private final int maxWorkers;

    private int starting;

    private int running;

    private final Lock lock = new ReentrantLock();

    private final OneWriterManyReaders<InboundEventHolder> inboundQueue;

    private int removeWorkerAfterIdleMins;
    
    private final AtomicInteger busy = new AtomicInteger();

    private class Worker implements Runnable {

        @Override
        public void run() {
            onStart();
            boolean active = true;
            try {
                while (active) {
                    InboundEventHolder holder = inboundQueue.removeLast(removeWorkerAfterIdleMins,
                            TimeUnit.MINUTES);
                    active = notifySubsciber(holder) || !exitByLongIdle();
                }
            } catch (InterruptedException e) {
                logger.debug("Worker thread is interrupted with message ", e.getMessage());
            } finally {
                onExit(!active);
            }
        }

        private void onStart() {
            lock.lock();
            try {
                --starting;
                ++running;
            } finally {
                lock.unlock();
            }
        }

        private void onExit(boolean interrupted) {
            lock.lock();
            try {
                if (interrupted) {
                    --running;
                }
            } finally {
                lock.unlock();
            }
        }

        private boolean notifySubsciber(InboundEventHolder holder) throws InterruptedException {
            if (holder == null) {
                return false;
            }
            busy.incrementAndGet();
            try {
                addWorkerIfNeeded();
                holder.notifySubscriber();
            } catch (InvocationTargetException ite) {
                if (ite.getCause() instanceof InterruptedException) {
                    throw (InterruptedException) ite.getCause();
                }
                // TODO: print to log, probably error
            } catch (Exception e) {
                // TODO: print some details about the exception.. probably error
            } finally {
                busy.decrementAndGet();
            }
            return true;
        }
        
        private boolean exitByLongIdle() {
            lock.lock();
            try {
                if (workersCount() > minWorkers) {
                    --running;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }
        
        private void addWorkerIfNeeded() {
            if (notEnoughWorkersForQueue()) {
                lock.lock();
                try {
                    if (notEnoughWorkersForQueue()) {
                        startNewWorker();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        
        private boolean notEnoughWorkersForQueue() {
            int workers = workersCount();
            return inboundQueue.size() > 0 && 
                    workersCount() < maxWorkers && 
                    busy.get() == workers; 
        }
    }

    InboundEventsDistributor(ExecutorService executor, int minWorkers, int maxWorkers) {
        this.executor = executor;
        this.minWorkers = Math.min(1, minWorkers);
        this.maxWorkers = Math.max(this.minWorkers, maxWorkers);
        this.inboundQueue = new OneWriterManyReaders<InboundEventHolder>(10000); // TODO
                                                                                  // configure
        startWorkers(this.minWorkers);
    }

    private void startWorkers(int count) {
        lock.lock();
        try {
            for (int i = 0; i < count; ++i) {
                startNewWorker();
            }
        } finally {
            lock.unlock();
        }
    }

    private int workersCount() {
        return starting + running;
    }

    private void startNewWorker() {
        if (workersCount() >= maxWorkers) {
            return;
        }
        starting++;
        try {
            executor.execute(new Worker());
        } catch (Throwable t) {
            --starting;
        }
    }
}
