package org.jrivets.util.testing;

/**
 * Simple <code>Thread.sleep()</code> wrapper.
 * 
 * @author Dmitry Spasibenko 
 *
 */
public final class SilentSleep {
    
    private SilentSleep() {
        throw new AssertionError();
    }

    /**
     * Invokes <code>Thread.sleep()</code> and consumes {@link InterruptedException} if 
     * it is thrown. If the {@link InterruptedException} is caught, it invokes
     * <code>Thread.interrupt()</code> and return the result
     * 
     * @param millis
     * @return true if {@link InterruptedException} is thrown, or <code>null</code> otherwise.
     */
    public static InterruptedException sleep(long millis) {
        try {
            Thread.sleep(millis);
            return null;
        } catch (InterruptedException ie) {
            Thread.interrupted();
            return ie;
        }
    }
}
