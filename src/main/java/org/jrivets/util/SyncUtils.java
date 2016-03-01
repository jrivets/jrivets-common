package org.jrivets.util;

public final class SyncUtils extends StaticSingleton {
    
    public interface InterruptableTimeProcess {
        void apply(long timeMs) throws InterruptedException;
    }
    
    public static boolean waitQuietly(Object o) {
        return waitQuietly(o, 0L);
    }
    
    public static boolean waitQuietly(Object o, long timeMs) {
        return timeProcessQuietly(o::wait, timeMs);
    }
    
    public static boolean sleepQuietly(long timeMs) {
        return timeProcessQuietly(Thread::sleep, timeMs);
    }
    
    public static boolean timeProcessQuietly(InterruptableTimeProcess p, long timeMs) {
        try {
            p.apply(timeMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
    
    public interface Op {
        void run() throws Exception;
    }
    
    public static Exception runQuietly(Op r) {
        try {
            r.run();
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
