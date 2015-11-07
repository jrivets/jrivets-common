package org.jrivets.util.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jrivets.util.CloseableLock;

/**
 * Key-value holder, with limited number of elements and TTL.
 * 
 * <p>
 * The holder allows to store value for a specified key for some period of time.
 * It also can keep no more pairs than specified {@code maxSize} value. If
 * holder size hits this limit, the oldest key-value pair will be removed from
 * the holder. Elements that are stored longer than the specified
 * {@code timeout} will be removed from the holder either by next call of
 * {@code put()} or {@code get()} method.
 * 
 * @param <K>
 *            - the key type
 * @param <V>
 *            - the value type
 */
public class LimitedKeyValueHolder<K, V> {

    private final long timeout;

    private final boolean timeoutAfterLastTouch;

    private final long sweepTimeout;

    private final int maxSize;

    private final boolean notifyWhenReplace;

    private long nextSweepTimeMillis = System.currentTimeMillis();

    protected final Map<K, Holder> holders = new HashMap<K, Holder>();

    protected final CloseableLock lock = new CloseableLock();

    protected final TreeSet<Holder> sortedSet = new TreeSet<Holder>((Holder h1, Holder h2) -> {
        if (h1.equals(h2)) {
            return 0;
        }
        return h1.lastTouch < h2.lastTouch ? -1 : 1;
    });

    protected class Holder {
        public final K key;
        public final V value;
        public final long ttl;
        long lastTouch;

        Holder(K key, V value, long ttl, long lastTouch) {
            this.key = key;
            this.value = value;
            this.ttl = ttl;
            this.lastTouch = lastTouch;
        }
    }

    /**
     * Constructs new instance of LimitedKeyValueHolder
     * 
     * @param timeout
     *            - defines timeout for keeping elements in the container.
     * @param maxSize
     *            - defines the maximum number of elements the container can
     *            keep inside.
     * @param timeoutAfterLastTouch
     *            - defines whether the timeout should be reset for an element
     *            if it is selected by <code>get()</code> or <code>put()</code>
     *            methods
     * @param notifyWhenReplace
     *            - the flag defines <code>put()</code> method behavior. The
     *            flag defines whether the <code>onRemove()</code> method will
     *            be called if the same value is stored for the same key
     *            (replaced), or not.
     */
    public LimitedKeyValueHolder(long timeout, int maxSize, boolean timeoutAfterLastTouch, boolean notifyWhenReplace) {
        this.timeout = timeout;
        this.timeoutAfterLastTouch = timeoutAfterLastTouch;
        this.maxSize = maxSize;
        this.sweepTimeout = timeout / 10;
        this.notifyWhenReplace = notifyWhenReplace;
    }

    public V get(K key) {
        long now = System.currentTimeMillis();
        sweep(now);
        try (CloseableLock l = lock.autounlock()) {
            Holder h = holders.get(key);
            touchHolder(h, now);
            return h == null ? null : h.value;
        }
    }

    /**
     * Puts an element into the holder.
     * <p>
     * The method will store value for the provided key into the container. If
     * there is another value, which was associated with the key before, it will
     * be removed out from the container and the <code>onRemove()</code>
     * notification will be called for the holder. Note: If the
     * <code>notifyWhenReplace</code> flag is not set (false), and the
     * substituted element value equals to the new one, the
     * <code>onRemove()</code> will not be called for the same key value.
     *
     * @param key
     * @param value
     * @param ttl - timeout (Milliseconds).
     */
    public void put(K key, V value, long ttl) {
        long now = System.currentTimeMillis();
        sweep(now);
        Holder removed = null;
        lock.lock();
        try {
            Holder h = holders.get(key);
            if (h != null) {
                if (!notifyWhenReplace && h.value.equals(value)) {
                    touchHolder(h, now);
                    return;
                }
                sortedSet.remove(h);
                holders.remove(h.key);
                removed = h;
            }
            Holder oversized = putUnsafe(key, value, ttl, now);
            removed = removed != null ? removed : oversized;
        } finally {
            lock.unlock();
            onRemove(removed);
        }
    }

    public void put(K key, V value) {
        put(key, value, timeout);
    }
    
    public V remove(K key) {
        long now = System.currentTimeMillis();
        sweep(now);
        Holder removed = null;
        lock.lock();
        try {
            removed = holders.remove(key);
            if (removed != null) {
                sortedSet.remove(removed);
                return removed.value;
            }
            return null;
        } finally {
            lock.unlock();
            onRemove(removed);
        }
    }

    public void clear() {
        List<Holder> removed = new ArrayList<Holder>(holders.size());
        lock.lock();
        try {
            removed.addAll(holders.values());
            holders.clear();
            sortedSet.clear();
        } finally {
            lock.unlock();
            onRemoved(removed);
        }
    }

    private void touchHolder(Holder holder, long now) {
        if (holder != null && timeoutAfterLastTouch) {
            sortedSet.remove(holder);
            holder.lastTouch = now;
            sortedSet.add(holder);
        }
    }

    protected void sweep(long timeMillis) {
        if (timeMillis - nextSweepTimeMillis >= 0L) {
            List<Holder> removed = new ArrayList<Holder>();
            try (CloseableLock l = lock.autounlock()) {
                if (timeMillis - nextSweepTimeMillis >= 0L) {
                    if (!holders.isEmpty()) {
                        Iterator<Map.Entry<K, Holder>> it = holders.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<K, Holder> e = it.next();
                            Holder holder = e.getValue();
                            if (timeMillis - holder.lastTouch >= holder.ttl) {
                                it.remove();
                                removed.add(holder);
                                sortedSet.remove(holder);
                            }
                        }
                    }
                    nextSweepTimeMillis = timeMillis + sweepTimeout;
                }
            }
            onRemoved(removed);
        }
    }

    /**
     * stores value for the key and returns removed holder if it happens due to
     * oversize.
     * 
     * @param key
     * @param value
     * @param now
     * @return
     */
    protected Holder putUnsafe(K key, V value, long ttl, long now) {
        Holder h = new Holder(key, value, ttl, now);
        Holder removed = null;
        holders.put(key, h);
        sortedSet.add(h);

        if (holders.size() > maxSize) {
            removed = sortedSet.first();
            sortedSet.remove(removed);
            holders.remove(removed.key);
        }
        return removed;
    }

    private void onRemoved(List<Holder> removed) {
        for (Holder holder : removed) {
            try {
                onRemove(holder);
            } catch (Exception ex) {
            }
        }
    }

    /**
     * on holder remove notification. Derived class can override and provide
     * some specific implementation. The method MUST be called WITHOUT holding
     * the component {@link lock} object.
     * 
     * @param holder
     *            - removed holder
     */
    protected void onRemove(Holder holder) {

    }
}
