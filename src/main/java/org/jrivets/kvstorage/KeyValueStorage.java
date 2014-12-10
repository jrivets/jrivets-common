package org.jrivets.kvstorage;

public interface KeyValueStorage<K, V> {

    void put(K key, V value);

    boolean cas(K key, V expected, V value);

    V get(K key);

    V remove(K key);

}
