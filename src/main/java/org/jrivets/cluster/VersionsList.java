package org.jrivets.cluster;

import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.collection.SortedArray;

public final class VersionsList {
    
    private final Comparator<Version> valsComparator = new Comparator<Version>() {

        @Override
        public int compare(Version arg0, Version arg1) {
            return Long.compare(arg0.version, arg1.version);
        }
        
    };

    private final SortedArray<Version> versions = new SortedArray<Version>(valsComparator, 1);
    
    private Lock lock = new ReentrantLock();
    
    Version newVersion(Value value) {
        lock.lock();
        try {
            Version version = new Version(getLatestVersion() + 1, VersionState.NEW);
            versions.add(version);
            return version;
        } finally {
            lock.unlock();
        }
    }
    
    void dropVersion(Version version) {
        lock.lock();
        try {
            
        } finally {
            lock.unlock();
        }
    }
    
    private Long getLatestVersion() {
        int len = versions.size();
        if (len == 0) {
            return 0L;
        }
        return versions.get(len - 1).version;
    }
    
}
