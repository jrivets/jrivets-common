package org.jrivets.kvstorage;

public class VersionedValue<V> {

    V value;
    
    int version;

    private VersionedValue() {
    }
    
    private VersionedValue(V value, int version) {
        this.value = value;
        this.version = version;
    }
       
    public V getValue() {
        return value;
    }
    
    public int getVersion() {
        return version;
    }
    
    public static <T> VersionedValue<T> get(T value, int version) {
        return new VersionedValue<T>(value, version);
    }
    
}
