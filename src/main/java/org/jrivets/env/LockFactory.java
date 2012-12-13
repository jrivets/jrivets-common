package org.jrivets.env;

import java.util.concurrent.locks.Lock;

public interface LockFactory {

    Lock getReentrantLock();
    
}
