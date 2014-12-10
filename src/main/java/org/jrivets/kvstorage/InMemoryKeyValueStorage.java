package org.jrivets.kvstorage;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;
import org.jrivets.util.CloseableLock;
import org.jrivets.util.container.LimitedKeyValueHolder;

public final class InMemoryKeyValueStorage<K, V> extends LimitedKeyValueHolder<K, V> implements KeyValueStorage<K, V> {

    private final Logger logger;

    public InMemoryKeyValueStorage(String bucketId, int maxSize, long ttl) {
        super(ttl, maxSize, true, false);
        this.logger = LoggerFactory.getLogger(InMemoryKeyValueStorage.class + "(" + bucketId + ")", null, null);
    }

    @Override
    public boolean cas(K key, V expected, V value) {
        long now = System.currentTimeMillis();
        sweep(now);
        try (CloseableLock l = lock.autounlock()) {
            logger.debug("CAS: key=", key, ", expected=", expected, ", value=", value);
            Holder h = holders.get(key);
            if (h == null || h.value == null) {
                if (expected != null) {
                    return false;
                }
            } else if (!h.value.equals(expected)) {
                return false;
            }
            putUnsafe(key, value, now);
            return true;
        }
    }
    
    @Override
    public void put(K key, V value) {
        logger.debug("PUT: key=", key, ", value=", value);
        super.put(key, value);
    }

    @Override
    public V remove(K key) {
        V v = super.remove(key);
        logger.debug("REMOVE: key=", key, ", result=", v);
        return v;
    }

}
