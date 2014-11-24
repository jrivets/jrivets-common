package org.jrivets.journal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;
import org.jrivets.util.container.Pair;

final class JournalInfoWriter {

    private final FileChannel channel;
    
    private final FileLock fileLock;
    
    private final File metaFile;
    
    private final Logger logger = LoggerFactory.getLogger(JournalInfoWriter.class);
    
    private JournalInfo journalInfo = JournalInfo.NULL_INFO;
    
    private ByteBuffer buffer = ByteBuffer.allocateDirect(52);
    
    @SuppressWarnings("resource")
    JournalInfoWriter(File metaFile, boolean cleanAfterOpen) throws IOException {
        this.channel = new RandomAccessFile(metaFile, "rw").getChannel();
        this.metaFile = metaFile;
        try {
            fileLock = channel.tryLock();
        } catch (Throwable t) {
            logger.error("Cannot obtain lock for the meta-file: ", this);
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException("Cannot obtain exlusive lock on the metadata file " + metaFile);
        }
        
        if (!cleanAfterOpen) {
            readJournalInfo();
        }
        writeJournalInfo(journalInfo); // arrange space for the data
    }
    
    JournalInfo get() {
        return journalInfo;
    }
    
    void set(JournalInfo info) throws IOException {
        if (info.equals(journalInfo)) {
            return;
        }
        writeJournalInfo(info);
        journalInfo = info;
    }
    
    void close() {
        IOUtils.releaseQuietly(fileLock);
        IOUtils.closeQuietly(channel);
    }
    
    private void writeJournalInfo(JournalInfo info) throws IOException {
        long ts = System.currentTimeMillis();
        buffer.clear();
        buffer.putLong(ts);
        buffer.putInt(hashCode(ts, info.hashCode()));
        write(info.getMarker());
        write(info.getReader());
        write(info.getWriter());
        buffer.putInt(info.getReadLimit());
        buffer.flip();
        channel.write(buffer, 0L);
    }
    
    private int hashCode(long l, int h) {
        return Long.hashCode(l)*19 + h;
    }
    
    private void write(Pair<Integer, Long> pos) {
        buffer.putInt(pos.getFirst());
        buffer.putLong(pos.getSecond());
    }
    
    private void readJournalInfo() throws IOException {
        buffer.clear();
        channel.read(buffer, 0L);
        buffer.flip();
        if (!buffer.hasRemaining()) {
            journalInfo = JournalInfo.NULL_INFO;            
            logger.info("No information in meta-file, will use an empty one ", this);
            return;
        }
        
        long ts = buffer.getLong();
        int jHash = buffer.getInt();
        Pair<Integer, Long> marker = read();
        Pair<Integer, Long> reader = read();
        Pair<Integer, Long> writer = read();
        int readLimit = buffer.getInt();
        journalInfo = new JournalInfo(marker, reader, writer, readLimit);
        
        int expectedHash = hashCode(ts, journalInfo.hashCode());
        logger.debug("Read from file: ts=", ts, ", jHash=", jHash, "(expectedHash=", expectedHash, "), journalInfo=", journalInfo);
        if (jHash != expectedHash) {
            logger.error("Corrupted information in meta-file (wrong hash), stop processing: ", this);
            JournalInfo ji = journalInfo;
            journalInfo = null;
            throw new IllegalStateException("The meta-file is corrupted " + ji);
        }
        logger.info("Found meta-inforamtion in meta-file: ", this);
    }
    
    private Pair<Integer, Long> read() {
        return new Pair<Integer, Long>(buffer.getInt(), buffer.getLong());
    }
    
    @Override
    public String toString() {
        return "{metaFile=" + metaFile.getAbsoluteFile() + ", journalInfo=" + journalInfo + "}";
    }
    
}
