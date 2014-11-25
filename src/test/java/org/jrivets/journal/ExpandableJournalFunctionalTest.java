package org.jrivets.journal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExpandableJournalFunctionalTest {

    private final static String PREFIX = "expandable";

    private Journal journal;

    @BeforeMethod
    public void setup() throws IOException, ChunkNotFoundException {
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        for (File file : files) {
            file.delete();
        }
        
        journal = new JournalBuilder().withMaxCapacity(100).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
    }

    @AfterMethod
    public void tearDown() {
        journal.close();
    }

    @Test
    public void chunksTest() throws IOException {
        byte[] array = getShuffledByteArray(53);
        journal.getOutputStream().write(array, 0, array.length);
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        assertEquals(files.size(), array.length/10 + (array.length%10 > 0 ? 1 : 0) + 1);
    }
    
    @Test
    public void markTest() throws IOException {
        byte[] array = getShuffledByteArray(50);
        journal.getOutputStream().write(array, 0, array.length);
        journal.getInputStream().mark(50);
        
        byte[] read = new byte[array.length];
        assertEquals(read.length, journal.getInputStream().read(read, 0, read.length));
        assertTrue(Arrays.equals(array, read));
        assertEquals(journal.getInputStream().read(), -1);
        assertEquals(-1, journal.getInputStream().read(read, 0, read.length));
        journal.getInputStream().reset();
        read[0]++;
        assertEquals(journal.getInputStream().read(read, 0, read.length), read.length);
        assertTrue(Arrays.equals(array, read));
    }

    @Test
    public void markTest2() throws IOException {
        byte[] array = getOrderedByteArray(50);
        journal.getOutputStream().write(array, 0, array.length);
        
        byte[] read = new byte[array.length];
        assertEquals(22, journal.getInputStream().read(read, 0, 22));
        assertEquals(read[21], 21);
        journal.getInputStream().mark(100);        
        assertEquals(journal.getInputStream().read(read, 0, read.length), read.length - 22);
        assertEquals(read[0], 22);
        
        journal.getInputStream().reset();
        assertEquals(journal.getInputStream().read(), 22);
        assertEquals(journal.getInputStream().read(read, 0, read.length), read.length - 23);
        
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        assertEquals(files.size(), 5);
    }

    @Test
    public void fullMarkTest() throws IOException {
        byte[] array = getOrderedByteArray(100);
        journal.getOutputStream().write(array, 0, array.length);
        journal.getInputStream().mark(array.length + 3);
        assertEquals(array.length, journal.getInputStream().read(array));
        assertEquals(journal.getInputStream().read(), -1);
    }
    
    @Test(expectedExceptions={IOException.class})
    public void resetOverflowTest() throws IOException {
        byte[] array = getShuffledByteArray(50);
        journal.getOutputStream().write(array, 0, array.length);
        journal.getInputStream().mark(5);
        
        byte[] read = new byte[array.length];
        assertEquals(read.length, journal.getInputStream().read(read, 0, read.length));
        journal.getInputStream().reset();
        fail("Reset should throw");
    }
    
    @Test
    public void eofTest() throws IOException {
        assertEquals(journal.getInputStream().read(), -1);
        
        byte[] array = getShuffledByteArray(50);
        journal.getOutputStream().write(array, 0, array.length);
        
        byte[] read = new byte[array.length];
        assertEquals(read.length, journal.getInputStream().read(read, 0, read.length));
        assertTrue(Arrays.equals(array, read));
        assertEquals(journal.getInputStream().read(), -1);
        assertEquals(journal.getInputStream().read(read, 0, read.length), -1);
    }
    
    @Test(timeOut=10000L)
    public void noEofTest() throws IOException, ChunkNotFoundException {
        journal.close();
        journal = new JournalBuilder().withMaxCapacity(100).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        final byte[] read = new byte[1];
        final AtomicBoolean b = new AtomicBoolean();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                b.set(true);
                try {
                    journal.getInputStream().read(read, 0, 1, 10000L);
                } catch (IOException e) {
                } finally {
                    b.set(false);
                }
            }
        }).start();
        
        while (!b.get()) Thread.yield();
        journal.getOutputStream().write(123);
        while (b.get()) Thread.yield();
        assertEquals(read[0], 123);
    }
    
    @Test
    public void continuousReadTest() throws IOException {
        byte[] array = getShuffledByteArray(100);
        int read = 0;
        for (int i = 0; i < 10; i++) {
            journal.getOutputStream().write(array, 0, array.length);
            while (journal.getInputStream().read() != -1) {
                read++;
            }
        }
        assertEquals(read, array.length*10);
    }
    
    @Test
    public void openCloseEmptyTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        byte[] in = new byte[array.length];
        journal.getInputStream().read(in);
        assertFalse(in == array);
        assertTrue(Arrays.equals(array, in));
        assertEquals(journal.getInputStream().read(), -1);
        journal.close();
        
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        assertEquals(files.size(), 2);
        journal = new JournalBuilder().withMaxCapacity(100).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        assertEquals(journal.getInputStream().read(), -1);
        journal.getOutputStream().write(123);
        assertEquals(journal.getInputStream().read(), 123);
    }

    @Test
    public void openCloseSomeTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        journal.getInputStream().mark(100);
        journal.close();
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        assertEquals(files.size(), 11);

        journal = new JournalBuilder().withMaxCapacity(101).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        byte[] in = new byte[array.length];
        assertEquals(100, journal.getInputStream().read(in));
        assertFalse(in == array);
        assertTrue(Arrays.equals(array, in));
        assertEquals(journal.getInputStream().read(), -1);
        journal.close();       
    }

    @Test
    public void openCloseMarkSomeTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        
        byte[] in = new byte[array.length];
        assertEquals(15, journal.getInputStream().read(in, 0, 15));
        
        journal.getInputStream().mark(100);
        journal.close();
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX);
        assertEquals(files.size(), 10);

        journal = new JournalBuilder().withMaxCapacity(100).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        assertEquals(85, journal.getInputStream().read(in));
        assertFalse(in == array);
        assertFalse(Arrays.equals(array, in));
        assertEquals(journal.getInputStream().read(), -1);
        journal.close();       
    }
    
    @Test
    public void readAllTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        byte[] in = new byte[array.length];
        assertEquals(100, journal.getInputStream().read(in));
        journal.close();

        journal = new JournalBuilder().withMaxCapacity(100).withMaxChunkSize(10).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        assertFalse(in == array);
        assertTrue(Arrays.equals(array, in));
        assertEquals(journal.getInputStream().read(), -1);
        journal.close();       
    }
    
    @Test
    public void shrinkMaxSizeTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        journal.close();

        journal = new JournalBuilder().withMaxCapacity(20).withMaxChunkSize(5).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        byte[] in = new byte[array.length];
        assertEquals(100, journal.getInputStream().read(in));
        journal.close();       
    }
    
    @Test(expectedExceptions = {ChunkNotFoundException.class})
    public void lostChunkTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        journal.close();
        
        Collection<File> files = IOUtils.getFiles(IOUtils.temporaryDirectory, PREFIX + "1");
        assertEquals(files.size(), 1);
        files.iterator().next().delete();
        
        journal = new JournalBuilder().withMaxCapacity(20).withMaxChunkSize(5).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
    }
    
    @Test
    public void cleanUpChunkTest() throws IOException, ChunkNotFoundException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array);
        journal.close();
        
        journal = new JournalBuilder().withMaxCapacity(20).withMaxChunkSize(5).withPrefixName(PREFIX).cleanAfterOpen()
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        
        assertEquals(-1, journal.getInputStream().read(array));
    }
    
    @Test(expectedExceptions = {FileNotFoundException.class})
    public void wrongFolderTest() throws IOException, ChunkNotFoundException {
        journal.close();
        journal = new JournalBuilder().withMaxCapacity(20).withMaxChunkSize(5).withPrefixName(PREFIX).cleanAfterOpen()
                .withFolderName(IOUtils.temporaryDirectory + "1234ka983kjf13hkjahd").buildExpandable();
    }

    @Ignore
    @Test
    public void stressTest() throws IOException, ChunkNotFoundException {
        journal.close();
        long capacity = 1000000000L;
        long chunk = capacity/10;
        int piece = (int) chunk/10000;
        journal = new JournalBuilder().withMaxCapacity(capacity).withMaxChunkSize(chunk).withPrefixName(PREFIX)
                .withFolderName(IOUtils.temporaryDirectory).buildExpandable();
        long start = System.currentTimeMillis();
        long size = 0;
        while (size < capacity) {
            byte[] data = getShuffledByteArray(piece);
            journal.getOutputStream().write(data);
            size += data.length;
        }
        
        while (true) {
            byte[] data = new byte[piece];
            int read = journal.getInputStream().read(data);
            if (read == -1) {
                break;
            }
            assertEquals(data.length, read);
        }
        long total = (System.currentTimeMillis() - start);
        long piecesPerSec = (capacity/piece)/(total/1000L);
        System.out.println("Total read/write 1G: " + total + "ms pieces/sec=" + piecesPerSec + " Megabytes per second=" + (capacity/(total/1000L)));
    }
    
    @Test
    public void availableForReadTest() throws IOException {
        byte[] array = getShuffledByteArray(55);
        FileSystemJournal fsJournal = (FileSystemJournal) journal;
        fsJournal.getOutputStream().write(array, 0, array.length);
        assertEquals(fsJournal.policy.chunks.size(), 6);
        
        assertEquals(fsJournal.getInputStream().available(), 55);
        assertEquals(fsJournal.available(), 55);
        fsJournal.getInputStream().read();
        fsJournal.getInputStream().read();
        assertEquals(fsJournal.getInputStream().available(), 53);
        assertEquals(fsJournal.available(), 53);
        
        fsJournal.getInputStream().read(array, 0, 12, 0L);
        assertEquals(fsJournal.getInputStream().available(), 41);
        assertEquals(fsJournal.available(), 41);
        
        fsJournal.getInputStream().mark(100);
        fsJournal.getInputStream().read(array, 0, 14, 0L);
        assertEquals(fsJournal.getInputStream().available(), 27);
        assertEquals(fsJournal.available(), 41);
    }
    
    @Test
    public void readTimeoutEmptyTest() throws IOException {
        byte[] array = getShuffledByteArray(100);
        journal.getOutputStream().write(array, 0, 50);
        journal.getInputStream().read(array, 0, 50);
        readTimeoutTest();
        
        journal.getOutputStream().write(array, 0, 100);
        journal.getInputStream().read(array, 0, 100);
        readTimeoutTest();
    }
    
    private void readTimeoutTest() throws IOException { 
        byte[] array = new byte[10];
        long start = System.currentTimeMillis();
        assertEquals(journal.getInputStream().read(array, 0, 10), -1);
        assertTrue(System.currentTimeMillis() - start < 50L);
        
        start = System.currentTimeMillis();
        assertEquals(journal.getInputStream().read(array, 0, 10, 50L), -1);
        assertTrue(System.currentTimeMillis() - start >= 50L);
    }
    
    private byte[] getShuffledByteArray(int size) {
        byte[] array = new byte[size];
        new Random().nextBytes(array);
        return array;
    }
    
    private byte[] getOrderedByteArray(int size) {
        byte[] array = new byte[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) i;
        }
        return array;
    }
}
