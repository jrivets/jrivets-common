package org.jrivets.cluster;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class TimerTask {

    private final ScheduledExecutorService scheduler;

    private final Runnable r;

    private final long timeoutMillis;

    private ScheduledFuture<?> scheduledTask;

    public TimerTask(ScheduledExecutorService scheduler, Runnable r, long timeoutMillis) {
        this.scheduler = scheduler;
        this.r = r;
        this.timeoutMillis = timeoutMillis;
        reschedule();
    }

    public boolean cancel() {
        try {
            if (scheduledTask == null || scheduledTask.isDone()) {
                return true;
            }
            return scheduledTask.cancel(false);
        } finally {
            scheduledTask = null;
        }
    }

    public void reschedule() {
        if (scheduledTask == null || scheduledTask.cancel(false)) {
            scheduledTask = scheduler.schedule(r, timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }
}
