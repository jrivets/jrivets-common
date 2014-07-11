package org.jrivets.util.container;

import java.util.Comparator;
import java.util.TreeSet;

/**
 * "Preemptive" key-value holder implementation.
 * <p>
 * This container allows to keep key-value pair for a specific time. If the key
 * value is not requested for the specified timeout the key-value pair will be
 * deleted from the container. Additionally, in case of the container reaches
 * maximum capacity, the "oldest" key-value pair (which has smallest accessTime
 * value) will be removed and new one is placed instead.
 * 
 * @author Dmitry Spasibenko
 *
 * @param <K>
 * @param <V>
 */

public abstract class PreemptiveKeyValueHodler<K, V> extends AbstractKeyValueHolder<K, V> {

    private TreeSet<Holder> treeSet = new TreeSet<Holder>(new HolderComparator());

    private class HolderComparator implements Comparator<Holder> {

        @Override
        public int compare(Holder h1, Holder h2) {
            if (h1.equals(h2)) {
                return 0;
            }
            return h1.getLastAccessTime() < h2.getLastAccessTime() ? -1 : (h1.getLastAccessTime() == h2
                    .getLastAccessTime() ? 0 : 1);
        }

    }

    protected PreemptiveKeyValueHodler(long expirationTimeoutMs, int maxSize) {
        super(expirationTimeoutMs, maxSize, true);

    }

    @Override
    protected synchronized Holder getHolderByKey(K key) {
        Holder holder = holders.get(key);
        if (holder == null) {
            holder = new Holder(key);
            if (holders.size() == maxSize) {
                Holder toRemove = treeSet.first();
                treeSet.remove(toRemove);
                holders.remove(toRemove.key);
                treeSet.remove(toRemove);
            }
            holders.put(key, holder);
        }
        holder.getValue(); // change access Time
        treeSet.remove(holder);
        treeSet.add(holder);
        return holder;
    }

    @Override
    protected void onRemove(Holder holder) {
        if (holder == null) {
            return;
        }
        treeSet.remove(holder);
    }

    @Override
    protected void onClear() {
        treeSet.clear();
    }

}
