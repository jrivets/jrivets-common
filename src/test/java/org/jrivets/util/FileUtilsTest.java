package org.jrivets.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class FileUtilsTest {
    
    @AfterMethod
    public void afterTest() {
        FileUtils.underTest = false;
    }
    
    @Test
    public void createTempDirTest() throws IOException {
        File tmpDir = FileUtils.createTempDir();
        tmpDir.delete();
    }
    
    @Test
    public void createTempDirTimeoutTest() throws IOException {
        FileUtils.underTest = true;
        File tmpDirOk = FileUtils.createTempDir();
        File tmpDir = null;
        try {
            tmpDir = FileUtils.createTempDir();
        } catch (RuntimeException re) {
            // ok
            return;
        } finally {
            tmpDirOk.delete();
        }
        if (tmpDir != null) {
            tmpDir.delete();
        }
        fail();
    }

    @Test
    public void createTempDirQuietlyTest() {
        FileUtils.underTest = true;
        File tmpDirOk = null;
        File tmpDir = null;
        try {
            tmpDirOk = FileUtils.createTempDirQuietly();
            tmpDir = FileUtils.createTempDirQuietly();
            assertNotNull(tmpDirOk);
            assertNull(tmpDir);
        } finally {
            if (tmpDirOk != null) {
                tmpDirOk.delete();
            }
            if (tmpDir != null) {
                tmpDir.delete();
            }
        }
    }
    
    @Test
    public void getListOfDirectories() throws IOException {
        File tmpDir = FileUtils.createTempDir();
        try {
            File subDir = new File(tmpDir, "d1/d2/d2");
            subDir.mkdirs();
            subDir = new File(tmpDir, "d2/d2");
            subDir.mkdirs();
            
            File file = new File(tmpDir, "file");
            file.createNewFile();
            List<String> dirs = FileUtils.getListOfDirectories(tmpDir.getAbsolutePath());
            assertEquals(dirs.size(), 2);
            assertTrue(dirs.contains("d1"));
            assertTrue(dirs.contains("d2"));
        } finally { 
            FileUtils.deleteDirectory(tmpDir);
        }
    }
    
    @Test
    public void getListOfFiles() throws IOException {
        File tmpDir = FileUtils.createTempDir();
        try {
            File subDir = new File(tmpDir, "dab1/d2");
            subDir.mkdirs();
            new File(tmpDir, "dab2").createNewFile();
            new File(tmpDir, "dabbb2").createNewFile();
            new File(tmpDir, "dba2").createNewFile();
            
            assertEquals(FileUtils.getListOfFiles(tmpDir.getAbsolutePath(), "").size(), 3);
            List<String> files = FileUtils.getListOfFiles(tmpDir.getAbsolutePath(), "dab");
            assertEquals(files.size(), 2);
            assertTrue(files.contains("dab2"));
            assertTrue(files.contains("dabbb2"));
        } finally { 
            FileUtils.deleteDirectory(tmpDir);
            assertFalse(tmpDir.exists());
        }
    }
}
