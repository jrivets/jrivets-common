package org.jrivets.util;

public final class SyncUtils {

    public static boolean waitQuietly(Object o) {
        return waitQuietly(o, 0L);
    }
    
    public static boolean waitQuietly(Object o, long timeout) {
        try {
            o.wait(timeout);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
    
}
