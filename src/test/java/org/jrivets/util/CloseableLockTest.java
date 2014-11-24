package org.jrivets.util;

import java.util.concurrent.locks.ReentrantLock;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CloseableLockTest {

    CloseableLock lock = new CloseableLock();
    
    @Test
    public void releaseTest() {
        try (CloseableLock l = lock.autounlock()) {
            assertTrue(((ReentrantLock) lock.lock).isLocked());
        }
        assertFalse(((ReentrantLock) lock.lock).isLocked());
    }
}
