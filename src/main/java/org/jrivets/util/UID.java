package org.jrivets.util;

import java.util.UUID;

/**
 * The class can represent different types of IDs. It provides an unified
 * abstraction layer for ID. An interface can use the type for to provide an
 * unified identifier for an object, but the real type of the identifier can be
 * different for different implementations.
 * 
 * @author Dmitry Spasibenko
 */
public final class UID {

    private final Object uid;
    
    private transient int hashCode;
    
    public UID(Object id) {
        this.uid = id;
        assertNotNullUid();
    }

    public UID(UID uid) {
        this.uid = uid.uid;
        this.hashCode = uid.hashCode;
    }
    
    public Object getID() {
        return uid;
    }
    
    public String asStringId() {
        return uid.toString();
    }
    
    public Long asLongId() {
        if (uid.getClass() == Long.class) {
            return (Long) uid;
        }
        if (uid.getClass() == String.class) {
            return Long.parseLong((String) uid);
        }
        throw new ClassCastException("Cannot convert value " + uid + "  to long");
    }
    
    public UUID asUUID() {
        if (uid.getClass() == UUID.class) {
            return (UUID) uid;
        }
        if (uid.getClass() == String.class) {
            return UUID.fromString((String) uid);
        }
        throw new ClassCastException("Cannot convert value " + uid + " to UUID");
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UID)) {
            return false;
        }
        Object ouid = ((UID) other).uid;
        if (uid.equals(ouid) || uid.toString().equals(ouid.toString())) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = uid.toString().hashCode(); 
        }
        return hashCode;
    }
    
    public static UID getUID(String s, Class<?> clazz) {
        if (clazz == String.class) {
            return new UID(s);
        }
        if (clazz == UUID.class) {
            return new UID(UUID.fromString(s));
        }
        if (clazz == Long.class) {
            return new UID(Long.valueOf(s));
        }
        throw new IllegalArgumentException("The object " + s 
                + " should be one of the types: String, UUID or Long");
    }

    public static UID randomUID() {
        return new UID(UUID.randomUUID());
    }

    private void assertNotNullUid() {
        if (uid == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public String toString() {
        return uid.toString();
    }

}
