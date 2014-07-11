package org.jrivets.journal;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.jrivets.util.container.Pair;

final class JournalInfoWriter {

    private final FileChannel channel;
    
    private final FileLock fileLock;
    
    private JournalInfo journalInfo = JournalInfo.NULL_INFO;
    
    private ByteBuffer buffer = ByteBuffer.allocateDirect(56);
    
    @SuppressWarnings("resource")
    JournalInfoWriter(File metaFile) throws IOException {
        channel = new RandomAccessFile(metaFile, "rw").getChannel();
        try {
            fileLock = channel.tryLock();
        } catch (Throwable t) {
            IOUtils.closeQuietly(channel);
            throw new IllegalStateException("Cannot obtain exlusive lock on the metadata file " + metaFile);
        }
        readJournalInfo();
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
        write(info.getMarker());
        write(info.getReader());
        write(info.getWriter());
        buffer.putInt(info.getReadLimit());
        buffer.putLong(ts);
        buffer.flip();
        channel.write(buffer, 0L);
    }
    
    private void write(Pair<Integer, Long> pos) {
        buffer.putInt(pos.getFirst());
        buffer.putLong(pos.getSecond());
    }
    
    private void readJournalInfo() throws IOException {
        buffer.clear();
        channel.read(buffer, 0L);
        try {
            buffer.flip();
            long ts = buffer.getLong();
            Pair<Integer, Long> marker = read();
            Pair<Integer, Long> reader = read();
            Pair<Integer, Long> writer = read();
            int readLimit = buffer.getInt();
            if (ts == buffer.getLong()) {
                journalInfo = new JournalInfo(marker, reader, writer, readLimit);
                return;
            }
        } catch (Exception ex) {
        }
        journalInfo = JournalInfo.NULL_INFO;
    }
    
    private Pair<Integer, Long> read() {
        return new Pair<Integer, Long>(buffer.getInt(), buffer.getLong());
    }
    
    @Override
    public String toString() {
        return "{journalInfo=" + journalInfo + "}";
    }
    
}
