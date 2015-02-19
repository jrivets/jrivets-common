package org.jrivets.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class PropertiesUtilsTest {

    @Test(expectedExceptions = {FileNotFoundException.class})
    public void loadFromWrongFile() throws IOException {
        PropertiesUtils.loadFromResource("tratatasomenotexistingfile");
    }
    
    @Test
    public void loadFromWrongFileQuietly() {
        assertNull(PropertiesUtils.loadFromResourceQuietly("tratatasomenotexistingfile"));
    }
    
    @Test
    public void loadFromFileOkTest() throws IOException {
        File tF = File.createTempFile("pref", Long.toString(System.currentTimeMillis()));
        tF.deleteOnExit();
        try (FileWriter fw = new FileWriter(tF.getAbsoluteFile())) {
            fw.write("abc=123\n");
            fw.write("cde=456\n");
        }
        
        Properties p = PropertiesUtils.loadFromFile(tF.getAbsolutePath());
        assertEquals(p.get("abc"), "123");
        assertEquals(p.get("cde"), "456");
        
        p = PropertiesUtils.loadFromFileQuietly(tF.getAbsolutePath());
        assertEquals(p.get("abc"), "123");
        assertEquals(p.get("cde"), "456");
    }
    
    @Test
    public void loadFromResourceOkTest() throws IOException {
        Properties p = PropertiesUtils.loadFromResource("properties.test");
        assertEquals(p.get("prop1"), "abc");
        assertEquals(p.get("prop2"), "cde");
        
        p = PropertiesUtils.loadFromResourceQuietly("properties.test");
        assertEquals(p.get("prop1"), "abc");
        assertEquals(p.get("prop2"), "cde");
    }
    
    @Test
    public void addOrOverrideTest() {
        Properties p = PropertiesUtils.loadFromResourceQuietly("properties.test");
        PropertiesUtils.addOrOverrideProps(p, null);
        assertEquals(p.get("prop1"), "abc");
        assertEquals(p.get("prop2"), "cde");
        
        Properties p2 = new Properties();
        p2.put("prop2", "ov");
        p2.put("prop3", "p3");
        
        PropertiesUtils.addOrOverrideProps(p, p2);
        assertEquals(p.get("prop1"), "abc");
        assertEquals(p.get("prop2"), "ov");
        assertEquals(p.get("prop3"), "p3");
    }
    
}
