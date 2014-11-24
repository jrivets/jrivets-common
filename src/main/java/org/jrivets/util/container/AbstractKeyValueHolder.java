package org.jrivets.util.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Time-based limited cached storage for key-value pairs
 * 
 * <p> The helper allows to store value for a specified key for some time
 * without calling source of the value for the period of time. It would be useful 
 * in the environment with a lot of clients requests for a value by key for 
 * short period of time, but where the value is not changed so often, 
 * so no reason to bother the source of the value for each get request.
 * 
 *  @author Dmitry Spasibenko 
 *
 * @param <K> - the key type
 * @param <V> - the value type
 */
public abstract class AbstractKeyValueHolder<K, V> {

    protected class Holder {
        
        private V value;
        
        public final K key;
        
        private volatile long accessTime = -1L;
        
        Holder(K key) {
            this.key = key;
        }
        
        boolean isNew() {
            return accessTime == -1L;
        }
        
        long getLastAccessTime() {
            return accessTime;
        }
        
        public synchronized V getValue() {
            if (isNew()) {
                value = getNewValue(key);
                accessTime = System.currentTimeMillis();
            } else if (timeoutAfterLastTouch) {
                accessTime = System.currentTimeMillis();
            }
            return value;
        }

        @Override
        public int hashCode() {
            return (key == null) ? 0 : key.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            @SuppressWarnings("unchecked")
            Holder other = (Holder) obj;
            if (key == null) {
                return other.key == null;
            }
            return key.equals(other.key);
        }
    }
    
    protected final Map<K, Holder> holders = new HashMap<K, Holder>();
    
    private final long expirationTimeout;
    
    private final long sweepTimeout;
    
    protected final int maxSize;
    
    private final boolean timeoutAfterLastTouch;
    
    private long nextSweepTimeMillis = System.currentTimeMillis();
    
    protected AbstractKeyValueHolder(long expirationTimeoutMs) {
        this(expirationTimeoutMs, Integer.MAX_VALUE);
    }
    
    protected AbstractKeyValueHolder(long expirationTimeoutMs, int maxSize) {
        this(expirationTimeoutMs, maxSize, false);
    }
    
    protected AbstractKeyValueHolder(long expirationTimeoutMs, int maxSize, boolean timeoutAfterLastTouch) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize=" + maxSize + " should be greater than 0.");
        }
        this.expirationTimeout = expirationTimeoutMs;
        this.sweepTimeout = expirationTimeoutMs / 5;
        this.maxSize = maxSize;
        this.timeoutAfterLastTouch = timeoutAfterLastTouch;
    }
    
    private void sweep(long timeMillis) {
        if (timeMillis - nextSweepTimeMillis >= 0L) {
            synchronized (this) {
                if (timeMillis - nextSweepTimeMillis >= 0L) {
                    if (!holders.isEmpty()) {
                        Iterator<Map.Entry<K, Holder>> it = holders.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<K, Holder> e = it.next();
                            Holder holder = e.getValue();
                            if (!holder.isNew() && (timeMillis - holder.getLastAccessTime() >= expirationTimeout)) {
                                it.remove();
                                onRemove(holder);
                            }
                        }
                    }
                    nextSweepTimeMillis = timeMillis + sweepTimeout;
                }
            }
        }
    }
    
    protected abstract V getNewValue(K key);
    
    protected synchronized Holder getHolderByKey(K key) {
        Holder holder = holders.get(key);
        if (holder == null) {
            holder = new Holder(key);
            if (holders.size() < maxSize) {
                holders.put(key, holder);
            }
        }
        return holder;
    }
    
    public V getValue(K key) {
        long now = System.currentTimeMillis();
        sweep(now);        
        Holder holder = getHolderByKey(key);        
        V value = holder.getValue();
        return value;
    }
    
    public synchronized Map<K, V> getCopyCollection() {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, Holder> entry: holders.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getValue());
        }
        return result;
    }
    
    public synchronized final void clear() {
        onClear();
        holders.clear();
    }

    public synchronized boolean drop(K key) {
        return dropUnsafe(key);
    }
    
    public boolean containsKey(K key) {
        sweep(System.currentTimeMillis());
        synchronized(this) {
            return holders.containsKey(key);
        }
    }
    
    protected boolean dropUnsafe(K key) {
        Holder holder = holders.remove(key);
        onRemove(holder);
        return holder != null;
    }
    
    /**
     * The method is called when holder is removed from the container 
     * @param holder
     */
    protected void onRemove(Holder holder) {
        
    }
    
    protected void onClear() {
        
    }
}
