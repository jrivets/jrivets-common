package org.jrivets.util;

public final class Strings {

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }
    
}
