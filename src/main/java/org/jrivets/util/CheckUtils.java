package org.jrivets.util;

public final class CheckUtils extends StaticSingleton {

    public static void arrayBounds(int arrayLength, int offset, int length) {
        if (offset < 0 || length < 0 || length > arrayLength - offset) {
            throw new IndexOutOfBoundsException("array.length=" + arrayLength + ", offset=" + offset + ", length=" + length);
        } 
    }
    
    public static void notNull(Object o1) {
        if (o1 == null) {
            throw new NullPointerException();
        }
    }
    
    public static void notNull(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            throw new NullPointerException();
        }
    }
    
    public static void notNull(Object o1, Object o2, Object o3) {
        if (o1 == null || o2 == null || o3 == null) {
            throw new NullPointerException();
        }
    }
    
    public static void notNull(Object o1, Object o2, Object o3, Object o4) {
        if (o1 == null || o2 == null || o3 == null || o4 == null) {
            throw new NullPointerException();
        }
    }
}
