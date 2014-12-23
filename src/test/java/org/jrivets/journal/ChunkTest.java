package org.jrivets.journal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jrivets.util.SyncUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ChunkTest {

    private File testFile;

    private Chunk chunk;

    class ReadWaiter implements Runnable {
        
        private volatile int readers;
        
        @Override
        public void run() {
            try {
                readers++;
                chunk.waitDataToRead(Long.MAX_VALUE);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                readers--;
            }
        }
        
        void waitForReader() {
            while (readers == 0) {
                Thread.yield();
            }
            SyncUtils.sleepQuietly(10L);
        }
    };

    @BeforeMethod
    public void init() throws IOException {
        testFile = File.createTempFile("chunkTest", ".tmp");
        testFile.deleteOnExit();
        chunk = null;
    }

    @AfterMethod
    public void closeChunk() {
        if (chunk != null) {
            chunk.close();
        }
    }

    @Test
    public void appendTest() throws IOException {
        writeBytes(testFile, 0, 100);
        assertEquals(testFile.length(), 100);
        chunk = new Chunk(1, 23, testFile, false, false);
        assertEquals(testFile.length(), 0);
    }

    @Test
    public void appendTest2() throws IOException {
        writeBytes(testFile, 0, 100);
        assertEquals(testFile.length(), 100);
        chunk = new Chunk(1, 23, testFile, true, false);
        assertEquals(testFile.length(), 100);
    }

    @Test
    public void getReadPositionTest() throws IOException {
        writeBytes(testFile, 0, 10);
        chunk = new Chunk(1, 23, testFile, true, false);
        assertEquals(chunk.getReadPosition(), 0);
        assertEquals(chunk.read(), 0);
        assertEquals(chunk.read(), 1);
        assertEquals(chunk.getReadPosition(), 2);

        byte b[] = new byte[10];
        assertEquals(chunk.read(b, 0, 2), 2);
        assertEquals(b[0], 2);
        assertEquals(b[1], 3);
        assertEquals(chunk.getReadPosition(), 4);

        assertEquals(chunk.read(b, 0, 10), 6);
        assertEquals(chunk.getReadPosition(), 10);

        for (int i = 0; i < 6; i++) {
            assertEquals(b[i], 4 + i);
        }

        assertEquals(chunk.read(), -1);
        assertEquals(chunk.getReadPosition(), 10);
    }

    @Test
    public void setReadPositionTest() throws IOException {
        writeBytes(testFile, 0, 10);
        chunk = new Chunk(1, 23, testFile, true, false);
        assertEquals(chunk.getReadPosition(), 0);
        assertEquals(chunk.read(), 0);
        assertEquals(chunk.read(), 1);
        assertEquals(chunk.getReadPosition(), 2);

        chunk.setReadPosition(1L);
        assertEquals(chunk.getReadPosition(), 1);
        assertEquals(chunk.read(), 1);
        assertEquals(chunk.getReadPosition(), 2);

        chunk.setReadPosition(-100L);
        assertEquals(chunk.getReadPosition(), 0);

        chunk.setReadPosition(100L);
        assertEquals(chunk.getReadPosition(), 100);
        assertEquals(chunk.read(), -1);

        chunk.setReadPosition(9);
        assertEquals(chunk.getReadPosition(), 9);
        assertEquals(chunk.read(), 9);
    }

    @Test
    public void writePositionTest() throws IOException {
        writeBytes(testFile, 0, 10);
        chunk = new Chunk(1, 23, testFile, true, false);
        assertEquals(chunk.getWritePosition(), 10);

        chunk.setWritePosition(-10L);
        assertEquals(chunk.getWritePosition(), 0);

        chunk.setWritePosition(12L);
        assertEquals(chunk.getWritePosition(), 10);

        chunk.write(123);
        assertEquals(chunk.getWritePosition(), 11);
        chunk.setReadPosition(10L);
        assertEquals(chunk.read(), 123);
        assertEquals(chunk.getReadPosition(), 11);
    }

    @Test
    public void writePositionTest2() throws IOException {
        writeBytes(testFile, 0, 10);
        chunk = new Chunk(1, 23, testFile, false, false);
        assertEquals(chunk.getWritePosition(), 0L);

        chunk.setWritePosition(10L);
        assertEquals(chunk.getWritePosition(), 0);
    }

    @Test
    public void skipTest() throws IOException {
        chunk = new Chunk(1, 23, testFile, false, false);
        assertEquals(chunk.skip(100L), 100L);
        assertEquals(chunk.getReadPosition(), 100L);
    }

    @Test
    public void reachReadPositionTest() throws IOException {
        chunk = new Chunk(1, 23, testFile, false, false);
        chunk.setReadPosition(2);
        assertEquals(chunk.read(), -1);

        chunk.write(1);
        chunk.write(2);
        chunk.write(3);
        assertEquals(chunk.getWritePosition(), 3);
        assertEquals(chunk.getReadPosition(), 2);
        assertEquals(chunk.read(), 3);
        assertEquals(chunk.getReadPosition(), 3);
    }

    @Test
    public void writeTest() throws IOException {
        chunk = new Chunk(1, 1, testFile, false, false);
        assertTrue(chunk.write(1));
        assertFalse(chunk.write(2));
        chunk.setWritePosition(0L);
        assertTrue(chunk.write(2));
        assertFalse(chunk.write(3));
    }

    @Test
    public void batchWriteTest() throws IOException {
        chunk = new Chunk(1, 3, testFile, false, false);
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertEquals(chunk.write(data, 0, 4), 3);
        assertEquals(1, chunk.read());
        assertEquals(2, chunk.read());
        assertEquals(3, chunk.read());
        assertEquals(-1, chunk.read());

        chunk.setWritePosition(2L);
        chunk.setReadPosition(2L);

        assertEquals(chunk.write(data, 2, 2), 1);
        assertEquals(3, chunk.read());
        assertEquals(-1, chunk.read());

        assertTrue(chunk.isDone());
        assertFalse(chunk.isReadyToRead());
        assertFalse(chunk.isReadyToWrite());
    }
    
    @Test
    public void singleWriteTest() throws IOException {
        chunk = new Chunk(1, 3, testFile, false, true);
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertEquals(chunk.getCapacity(), 3);
        assertEquals(chunk.write(data, 0, 4), 4);
        assertEquals(1, chunk.read());
        assertEquals(2, chunk.read());
        assertEquals(3, chunk.read());
        assertEquals(4, chunk.read());
        assertEquals(chunk.getCapacity(), 4);

        chunk.setWritePosition(3L);
        chunk.setReadPosition(3L);

        assertEquals(chunk.write(data, 2, 2), 2);
        assertEquals(chunk.getCapacity(), 5);
        assertEquals(3, chunk.read());
        assertEquals(4, chunk.read());

        assertTrue(chunk.isDone());
        assertFalse(chunk.isReadyToRead());
        assertFalse(chunk.isReadyToWrite());
    }

    @Test
    public void differentIsTest() throws IOException {
        chunk = new Chunk(1, 3, testFile, false, false);
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertTrue(chunk.isReadyToWrite());
        assertEquals(chunk.write(data, 0, 4), 3);

        assertFalse(chunk.isDone());
        assertTrue(chunk.isReadyToRead());
        assertFalse(chunk.isReadyToWrite());

        assertEquals(chunk.read(data, 0, 4), 3);
        assertTrue(chunk.isDone());
        assertFalse(chunk.isReadyToRead());
        assertFalse(chunk.isReadyToWrite());

    }

    @Test(timeOut = 10000L)
    public void readNotificationTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false, false);
        ReadWaiter rw = new ReadWaiter();
        Thread t = new Thread(rw);
        t.start();
        rw.waitForReader();
        chunk.write(1);
        t.join();
    }

    @Test(timeOut = 5000L)
    public void batchReadNotificationTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false, false);
        ReadWaiter rw = new ReadWaiter();
        Thread t = new Thread(rw);
        t.start();
        rw.waitForReader();
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertEquals(chunk.write(data, 0, 4), 3);
        t.join();
    }

    @Test(timeOut = 10000L)
    public void isReadyToReadTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false, false);
        chunk.write(1);
        chunk.waitDataToRead(5000L);
    }

    @Test(timeOut = 10000L)
    public void isReadyToReadWhenDoneTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 2, testFile, false, false);
        chunk.write(1);
        chunk.read();
        chunk.read();
        
        long start = System.currentTimeMillis();
        chunk.waitDataToRead(0L);
        assertTrue(System.currentTimeMillis() - start < 50L);
        
        start = System.currentTimeMillis();
        chunk.waitDataToRead(50L);
        assertTrue(System.currentTimeMillis() - start >= 50L);
    }
    
    @Test
    public void deleteTest() throws IOException {
        chunk = new Chunk(1, 1, testFile, false, false);
        assertTrue(testFile.exists());
        chunk.delete();
        assertFalse(testFile.exists());
    }

    private void writeBytes(File file, int start, int count) throws IOException {
        FileOutputStream os = new FileOutputStream(file);
        while (count-- > 0) {
            os.write(start++);
        }
        os.close();
    }

}
