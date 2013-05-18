package org.jrivets.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

public class GUIDTest {

    @Test
    public void naiveTest() {
        Set<GUID> set = new HashSet<GUID>();
        for (int i = 1; i < 10000; i++) {
            GUID g = new GUID();
            set.add(g);
            assertEquals(i, set.size());
        }
    }
    
    @Test
    public void equalsStrTest() {
        GUID id = new GUID();
        String strId = id.toString();
        assertEquals(id, new GUID(strId.toUpperCase()));
    }
    
    @Test
    public void copyConstructor() {
        GUID id = new GUID();
        assertEquals(id, new GUID(id));
    }
    
    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        GUID id = new GUID();
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
        ObjectOutput out = new ObjectOutputStream(bos) ;
        out.writeObject(id);
        out.close();
        
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        GUID id2 = (GUID) in.readObject();
        in.close();
        
        assertEquals(id, id2);
    }
    
    @Test
    public void wrongString() {
        try {
            new GUID((String) null);
            fail("Should fail");
        } catch(NullPointerException npe) {
            // ok
        }
    }
    
    @Test
    public void wrongString2() {
        try {
            new GUID("1234abc");
            fail("Should fail - wrong length");
        } catch(IllegalArgumentException iae) {
            // ok
        }
    }
    
    @Test
    public void wrongString3() {
        new GUID("0123456789abcdef0123456789abcdef");
        try {
            new GUID("0123456789abcdeh0123456789abcdef");
            fail("Should fail - wrong letter");
        } catch(IllegalArgumentException iae) {
            // ok
        }
    }

    @Test
    public void goodString() {
        goodStringTest("00123456789ABcDeF00123456789ABcD");
        goodStringTest("00000000000000000000000000000000");
        goodStringTest("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF");
        goodStringTest("00000000FFFFFFFFF00000000FFFFFFF");
    }
    
    @Test
    public void msbLsbTest() {
        GUID id = new GUID("FFFFFFFFFFFFFFFF0000000000000000");
        assertEquals(-1L, id.getMSB());
        assertEquals(0L, id.getLSB());
        id = new GUID(0L, -1L);
        assertEquals("0000000000000000ffffffffffffffff", id.toString());
    }
    
    private void goodStringTest(String strId) {
        GUID id = new GUID(strId);
        GUID id2 = new GUID(id);
        assertEquals(strId.toLowerCase(), id2.toString());
        assertEquals(id, id2);
    }

    @Test
    public void wrongId() {
        try {
            new GUID((GUID) null);
            fail("Should fail - wrong id");
        } catch(NullPointerException npe) {
            // ok
        }
    }

}
