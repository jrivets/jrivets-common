package org.jrivets.kvstorage;

public interface KeyValueStorage<K, V> {  
    
    /**
     * puts value to the store
     * @param key
     * @param value
     * @param version - non-negative value of version assigned to the change
     */
    int put(K key, V value);
    
    /**
     * performs CAS operation over the version. 
     * @param key
     * @param value
     * @param version - expected version value
     * @return >0 indicates that version is updated. The returned value is new version for the key value.
     */
    int cas(K key, V value, int version);
    
    /**
     * returns value by key. 
     * @param key
     * @return
     */
    VersionedValue<V> get(K key);

    /**
     * removes data unconditionally.
     * @param key
     * @return
     */
    VersionedValue<V> remove(K key);
    
    /**
     * removes key value if the store version is same to the version param
     * @param key
     * @param version
     * @return
     */
    VersionedValue<V> remove(K key, int version);

}
