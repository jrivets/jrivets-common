package org.jrivets.util;

import static org.testng.Assert.*;

import java.util.UUID;

import org.testng.annotations.Test;

public class UIDTest {

    @Test
    public void checkSerializationGUID() {
        UUID g = UUID.randomUUID();
        UID u = new UID(g);
        UUID g1 = (UUID) UID.getUID(u.toString(), UUID.class).getID();
        assertEquals(g, g1);
    }
    
    @Test
    public void checkSerializationGUID2() {
        UUID g = UUID.randomUUID();
        UID u = new UID(g);
        String su = u.toString();
        UID u1 = new UID(su);
        assertEquals(u, u1);
        assertEquals(u.hashCode(), u1.hashCode());
    }
    
    @Test
    public void checkSerializationLong() {
        Long l = 123456789L;
        UID u = new UID(l);
        Long l1 = (Long) UID.getUID(u.toString(), Long.class).getID();
        assertEquals(l, l1);
    }
    
    @Test
    public void checkSerializationString() {
        String s = "A string UID";
        UID u = new UID(s);
        String s1 = UID.getUID(u.toString(), String.class).asStringId();
        assertEquals(s, s1);
    }
    
    @Test
    public void checkHashCode() {
        String s = "A string UID";
        UID u = new UID(s);
        assertEquals(u.hashCode(), s.hashCode());
    }

    @Test
    public void checkEquals() {
        String s = "123";
        UID u = new UID(s);
        UID ul = new UID(123L);
         
        assertFalse(u.equals(null));
        assertTrue(u.equals(ul));
        assertTrue(u.equals(u));
        assertTrue(u.equals(new UID(s)));
    }
    
    @Test(expectedExceptions={NullPointerException.class})
    public void assertNotNullLongId() {
        Long id = null;
        new UID(id);
    }

    @Test(expectedExceptions={NullPointerException.class})
    public void assertNotNullStringId() {
        String id = null;
        new UID(id);
    }
    
    @Test(expectedExceptions={NullPointerException.class})
    public void assertNotNullUUIDId() {
        UUID id = null;
        new UID(id);
    }
    
    @Test(expectedExceptions={IllegalArgumentException.class})
    public void checkSerializationInteger() {
        UID.getUID("1324", Integer.class);
    }
    
    @Test
    public void longStringTest() {
        UID uidl = new UID(1234L);
        UID uids = new UID("1234");
        
        assertEquals(uidl, uids);
        assertEquals((Long) uids.asLongId(), (Long) 1234L);
        assertEquals((String) uidl.asStringId(), (String) "1234");
        assertEquals(uidl.hashCode(), uids.hashCode());
    }
    
    @Test
    public void uuidStringTest() {
        UID uidu = new UID(UUID.randomUUID());
        UID uids = new UID(uidu.getID().toString());
        
        assertEquals(uidu, uids);
        assertEquals((UUID) uids.asUUID(), (UUID) uidu.getID());
        assertEquals((String) uidu.asStringId(), uids.toString());
        assertEquals(uidu.hashCode(), uids.hashCode());
    }
}
