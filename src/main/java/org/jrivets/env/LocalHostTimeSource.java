package org.jrivets.env;

public final class LocalHostTimeSource implements TimeSource {

    private static class LocalHostTimeSourceHolder {
        private static final LocalHostTimeSource instance = new LocalHostTimeSource();
    }
    
    private LocalHostTimeSource() {
    }
    
    public static LocalHostTimeSource getInstance() {
        return LocalHostTimeSourceHolder.instance;
    }
    
    @Override
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
