package org.jrivets.env;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DefaultLockFactory implements LockFactory {

    private static class DefaultLockFactoryHolder {
        private static final DefaultLockFactory instance = new DefaultLockFactory();
    }
    
    private DefaultLockFactory() {
    }
    
    public static DefaultLockFactory getInstance() {
        return DefaultLockFactoryHolder.instance;
    }
    
    @Override
    public Lock getReentrantLock() {
        return new ReentrantLock();
    }

}
