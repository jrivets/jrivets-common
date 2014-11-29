package org.jrivets.util;

public final class CheckUtils extends StaticSingleton {

    public static void arrayBounds(int arrayLength, int offset, int length) {
        if (offset < 0 || length < 0 || length > arrayLength - offset) {
            throw new IndexOutOfBoundsException("array.length=" + arrayLength + ", offset=" + offset + ", length=" + length);
        } 
    }
    
}
