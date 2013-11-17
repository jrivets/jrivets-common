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

    private final Logger logger;

    private final ExecutorService executor;

    private final int minWorkers;

    private final int maxWorkers;

    private int starting;

    private int running;

    private final Lock lock = new ReentrantLock();

    private final OneMFixedSizeQueue<SubscriberNotification> inboundQueue;

    private final int removeWorkerAfterIdleMins;

    private final AtomicInteger busy = new AtomicInteger();

    private class Worker implements Runnable {

        @Override
        public void run() {
            onStart();
            boolean active = true;
            try {
                while (active) {
                    SubscriberNotification holder = inboundQueue.removeLast(removeWorkerAfterIdleMins, TimeUnit.MINUTES);
                    active = notifySubsciber(holder) || !exitByLongIdle();
                }
            } catch (InterruptedException e) {
                logger.debug("Worker thread is interrupted with message ", e.getMessage());
            } finally {
                onExit(active);
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

        private boolean notifySubsciber(SubscriberNotification holder) throws InterruptedException {
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
                logger.error("Notification exception. holder=", holder, ite.getCause());
            } catch (NoSuchMethodException nme) {
                logger.debug("Cannot find method: ", nme.getMessage(), holder);
            } catch (Exception e) {
                logger.error("Exception while notify subscriber: ", e);
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
                    logger.info("Reduce number of workers due to idle state ", InboundEventsDistributor.this);
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

    }

    InboundEventsDistributor(ExecutorService executor, String name, int minWorkers, int maxWorkers, int queueSize,
            int idleMins) {
        this.logger = LoggerFactory.getLogger(InboundEventsDistributor.class, "(" + name + ") %2$s", null);
        this.executor = executor;
        this.minWorkers = Math.min(1, minWorkers);
        this.maxWorkers = Math.max(this.minWorkers, maxWorkers);
        this.inboundQueue = new OneMFixedSizeQueue<SubscriberNotification>(queueSize);
        this.removeWorkerAfterIdleMins = idleMins;
        logger.info("Starting with minWorkers=", minWorkers, ", maxWorkers=", maxWorkers, " qSize=", queueSize,
                ", removeWorkerAfterIdleMins=", removeWorkerAfterIdleMins);
        startWorkers(this.minWorkers);
    }

    void put(SubscriberNotification holder) throws InterruptedException {
        inboundQueue.put(holder);
        addWorkerIfNeeded();
    }

    private void startWorkers(int count) {
        lock.lock();
        try {
            for (int i = 0; i < count; ++i) {
                startNewWorker();
            }
            logger.info("Starting ", count, " workers ", this);
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

    private void addWorkerIfNeeded() {
        if (notEnoughWorkersForQueue()) {
            lock.lock();
            try {
                if (notEnoughWorkersForQueue()) {
                    logger.info("Adding new worker ", InboundEventsDistributor.this);
                    startNewWorker();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private boolean notEnoughWorkersForQueue() {
        int workers = workersCount();
        return inboundQueue.size() > 0 && workersCount() < maxWorkers && busy.get() == workers;
    }
    
    @Override
    public String toString() {
        return new StringBuilder(128).append("{workers=").append(workersCount()).append("(r=").append(running)
                .append(" + s=").append(starting).append("), busy=").append(busy).append(", qSize=")
                .append(inboundQueue.size()).append("}").toString();
    }
}
