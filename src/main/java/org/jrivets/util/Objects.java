package org.jrivets.util;

public final class Objects {

    public static boolean equal(Object o1, Object o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }
    
    public static int hashCode(int hash, Object o) {
        return hash*31 + (o == null ? null : o.hashCode());
    }
}
