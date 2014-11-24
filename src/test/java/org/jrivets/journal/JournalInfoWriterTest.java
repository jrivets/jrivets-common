package org.jrivets.journal;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jrivets.util.container.Pair;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JournalInfoWriterTest {

    private File tmpFile;
    
    @BeforeMethod
    public void before() throws IOException {
        tmpFile = File.createTempFile("test", null);
    }
    
    @AfterMethod
    public void after() {
        tmpFile.delete();
    }
    
    @Test
    public void noSuchFile() throws IOException {
        tmpFile.delete();
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        assertEquals(JournalInfo.NULL_INFO, pos.get());
        pos.close();
    }
    
    @Test
    public void emptyFile() throws IOException {
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        assertEquals(JournalInfo.NULL_INFO, pos.get());
        pos.close();
    }
    
    @Test(expectedExceptions = {BufferUnderflowException.class})
    public void wrongFile() throws IOException {
        corruptFile(false);
        new JournalInfoWriter(tmpFile, false);
    }
    
    @Test
    public void corruptedFile() throws IOException {
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        pos.set(newJournalInfo(1, 2, 1, 3, 2, 2, 2));
        pos.close();
        
        corruptFile(true);
        
        try {
            new JournalInfoWriter(tmpFile, false);
        } catch (IllegalStateException ise) {
            return; //ok
        }
        fail("This test should not come to the point.");
    }
    
    @Test
    public void dropOldData() throws IOException {
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        pos.set(newJournalInfo(1, 2, 1, 3, 2, 2, 2));
        pos.close();
        
        corruptFile(true);
        new JournalInfoWriter(tmpFile, true);
    }
    
    @Test
    public void goodFile() throws IOException {
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        pos.set(newJournalInfo(1, 2, 1, 3, 2, 2, 2));
        pos.close();
        
        pos = new JournalInfoWriter(tmpFile, false);
        assertEquals(pos.get(), newJournalInfo(1, 2, 1, 3, 2, 2, 2));
        pos.close();
    }
    
    @Test(expectedExceptions = {IllegalStateException.class})
    public void exclusiveAccess() throws IOException {
        JournalInfoWriter pos = new JournalInfoWriter(tmpFile, false);
        try {
            new JournalInfoWriter(tmpFile, false);
        } finally {
            pos.close();
        }
        fail("Position should have exclusive access to the file");
    }
    
    private void corruptFile(boolean assertSize) throws IOException {
        try (@SuppressWarnings("resource")
        FileChannel fc = new RandomAccessFile(tmpFile, "rw").getChannel()) {
            fc.position(0);
            ByteBuffer bb = ByteBuffer.allocate(22);
            bb.clear();
            bb.putLong(System.currentTimeMillis());
            bb.putInt(123456789);
            bb.flip();
            fc.write(bb);
            if (assertSize) {
                assertEquals(fc.size(), 52);
            }
        } 
    }
    
    private JournalInfo newJournalInfo(int marker, long markerPos, int reader, long readerPos, int writer, long writerPos, int limit) {
        return new JournalInfo(new Pair<Integer, Long>(marker, markerPos),
                new Pair<Integer, Long>(reader, readerPos), new Pair<Integer, Long>(writer, writerPos), 0);
    }
}
