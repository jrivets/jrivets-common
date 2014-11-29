package org.jrivets.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Strings extends StaticSingleton {

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }
 
    public static Map<String, String> asMap(String ... strings) {
        if (strings == null || strings.length == 0) {
            return Collections.emptyMap();
        }
        if ((strings.length&1) != 0) {
            throw new IllegalArgumentException("Number of arguments for asMap method should be even: " + Arrays.toString(strings));
        }
        HashMap<String, String> result = new HashMap<String, String>();
        int i = 0; 
        while (i < strings.length) {
            result.put(strings[i], strings[i+1]);
            i += 2;
        }
        return result;
    }
    
    public static Long asLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException ne) {
            // it's ok
        }
        return null;
    }
    
}
