package org.jrivets.journal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ChunkTest {

    private File testFile;

    private Chunk chunk;

    private volatile int readers;

    class ReadWaiter implements Runnable {
        @Override
        public void run() {
            try {
                readers++;
                chunk.waitDataToRead();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                readers--;
            }
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
        chunk = new Chunk(1, 23, testFile, false);
        assertEquals(testFile.length(), 0);
    }

    @Test
    public void appendTest2() throws IOException {
        writeBytes(testFile, 0, 100);
        assertEquals(testFile.length(), 100);
        chunk = new Chunk(1, 23, testFile, true);
        assertEquals(testFile.length(), 100);
    }

    @Test
    public void getReadPositionTest() throws IOException {
        writeBytes(testFile, 0, 10);
        chunk = new Chunk(1, 23, testFile, true);
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
        chunk = new Chunk(1, 23, testFile, true);
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
        chunk = new Chunk(1, 23, testFile, true);
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
        chunk = new Chunk(1, 23, testFile, false);
        assertEquals(chunk.getWritePosition(), 0L);

        chunk.setWritePosition(10L);
        assertEquals(chunk.getWritePosition(), 0);
    }

    @Test
    public void skipTest() throws IOException {
        chunk = new Chunk(1, 23, testFile, false);
        assertEquals(chunk.skip(100L), 100L);
        assertEquals(chunk.getReadPosition(), 100L);
    }

    @Test
    public void reachReadPositionTest() throws IOException {
        chunk = new Chunk(1, 23, testFile, false);
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
        chunk = new Chunk(1, 1, testFile, false);
        assertTrue(chunk.write(1));
        assertFalse(chunk.write(2));
        chunk.setWritePosition(0L);
        assertTrue(chunk.write(2));
        assertFalse(chunk.write(3));
    }

    @Test
    public void batchWriteTest() throws IOException {
        chunk = new Chunk(1, 3, testFile, false);
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
    public void differentIsTest() throws IOException {
        chunk = new Chunk(1, 3, testFile, false);
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
        chunk = new Chunk(1, 3, testFile, false);
        Thread t = new Thread(new ReadWaiter());
        t.start();
        while (readers == 0) {
            Thread.yield();
        }
        chunk.write(1);
        t.join();
    }

    @Test(timeOut = 10000L)
    public void batchReadNotificationTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false);
        Thread t = new Thread(new ReadWaiter());
        t.start();
        while (readers == 0) {
            Thread.yield();
        }
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertEquals(chunk.write(data, 0, 4), 3);
        t.join();
    }

    @Test(timeOut = 10000L)
    public void isDoneReadNotificationTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false);
        Thread t = new Thread(new ReadWaiter());
        t.start();
        while (readers == 0) {
            Thread.yield();
        }
        byte[] data = new byte[] { 1, 2, 3, 4 };
        assertEquals(chunk.write(data, 0, 4), 3);
        t.join();
    }

    @Test(timeOut = 10000L)
    public void isReadyToReadTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 3, testFile, false);
        chunk.write(1);
        chunk.waitDataToRead();
    }

    @Test(timeOut = 10000L)
    public void isReadyToReadWhenDoneTest() throws IOException, InterruptedException {
        chunk = new Chunk(1, 1, testFile, false);
        chunk.write(1);
        chunk.read();
        chunk.waitDataToRead();
    }
    
    @Test
    public void deleteTest() throws IOException {
        chunk = new Chunk(1, 1, testFile, false);
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
