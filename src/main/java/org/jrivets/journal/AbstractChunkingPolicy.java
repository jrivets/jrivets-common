package org.jrivets.journal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.log.Logger;
import org.jrivets.util.container.Pair;

abstract class AbstractChunkingPolicy {

    final long maxCapacity; // max Journal capacity (disk space
    // occupied) (sum of all chunks capacity)

    final long maxChunkSize; // max size of one file-chunk

    final String folderName;

    final String prefixName;

    final boolean neverEof; // Input stream never returns -1 on read()

    protected final Logger logger;

    protected int nextChunkId;

    protected final Lock lock = new ReentrantLock();

    protected final Condition cond = lock.newCondition();

    protected boolean condCharged;

    protected volatile ArrayList<Chunk> chunks = new ArrayList<Chunk>();

    volatile Chunk inputChunk;

    protected volatile Chunk outputChunk;

    protected volatile Chunk markedChunk;

    private long markedPos;

    private int readLimit;

    protected final JournalInfoWriter journalInfoWriter;

    protected AbstractChunkingPolicy(Logger logger, long maxCapacity, long maxChunkSize, String folderName,
            String prefixName, boolean neverEof) throws IOException {
        if (maxCapacity < 0 || maxChunkSize < 0 || maxChunkSize > maxCapacity) {
            throw new IllegalArgumentException("maxCapacity=" + maxCapacity
                    + " should be positive and not less than maxChunkSize=" + maxChunkSize);
        }
        this.logger = logger;
        this.maxCapacity = maxCapacity;
        this.maxChunkSize = maxChunkSize;
        this.folderName = folderName;
        this.prefixName = prefixName;
        this.neverEof = neverEof;
        this.journalInfoWriter = new JournalInfoWriter(new File(folderName, prefixName));
    }

    void mark(int readLimit) {
        logger.debug("mark() readLimit=", readLimit);
        this.readLimit = readLimit;
        if (readLimit <= 0) {
            markedChunk = null;
            return;
        }
        markedChunk = inputChunk;
        markedPos = inputChunk.getReadPosition();
    }

    void reset() throws IOException {
        if (markedChunk == null) {
            logger.warn("Cannot reset position, marker is not set ", this);
            throw new IOException("Reset to invalid mark.");
        }
        inputChunk = markedChunk;
        inputChunk.setReadPosition(markedPos);
        adjustChunksPositions();
        logger.debug("reset() inputChunk=", inputChunk);
    }

    /**
     * If waitNewData == true, it can block invocation thread until the data is
     * available
     * 
     * @param waitNewData
     * @return whether the input stream should have new data to be read.
     * @throws IOException
     */
    boolean syncInput(boolean waitNewData) throws IOException {
        Chunk ic = adjustInputChunk();
        if (waitNewData && neverEof) {
            logger.debug("syncInput(): waiting for ", ic);
            ic.waitDataToRead();
            logger.debug("syncInput(): done with ", ic);
        }
        return ic.isReadyToRead();
    }

    /**
     * Can block invocation thread for indefinite time, until a space is
     * available.
     * 
     * @throws IOException
     */
    void advanceOutputChunk() throws IOException {
        lock.lock();
        try {
            while (!outputChunk.isReadyToWrite()) {
                if (!newChunk()) {
                    waitCond();
                }
            }
        } finally {
            lock.unlock();
            writeJournalInfo();
        }
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    Chunk adjustInputChunk() throws IOException {
        lock.lock();
        try {
            while (inputChunk.isDone()) {
                if (inputChunk == outputChunk) {
                    newChunk();
                    inputChunk = outputChunk;
                    if (!inputChunk.isDone()) {
                        inputChunk.setReadPosition(0L);
                        break;
                    }
                    // Ok, we reached max capacity, but still can return -1 if
                    // it is acceptable
                    if (!neverEof) {
                        return inputChunk;
                    }
                    // Well, we are blocked: we cannot wait for write, because
                    // write is blocked
                    // by maximum capacity and is waiting for read probably.
                    // This is potential dead-locking, so throw exception to let
                    // caller
                    // know about incorrect settings or usage of the journal.
                    logger.warn("adjustInputChunk(): Something goes wrong ", this, ", chunks=", chunks);
                    throw new IllegalStateException(
                            "Input stream is blocked because of no sufficient disk space in the journal. This can be caused by wrong journal configuration.");
                }
                int idx = chunks.indexOf(inputChunk);
                inputChunk = chunks.get(idx + 1);
                inputChunk.setReadPosition(0L);
            }
            if (getToMarkLength() > readLimit) {
                mark(-1);
            }
            return inputChunk;
        } finally {
            lock.unlock();
            writeJournalInfo();
        }
    }

    boolean isClosed() {
        return chunks == null;
    }

    void close() {
        ArrayList<Chunk> chunks = null;
        lock.lock();
        try {
            chunks = this.chunks;
            this.chunks = null;
        } finally {
            lock.unlock();
        }

        if (chunks == null) {
            logger.warn("close(): already closed, ignore the request");
            return;
        }

        logger.info("close(): Closing ", this);
        writeJournalInfo();
        journalInfoWriter.close();

        Iterator<Chunk> it = chunks.iterator();
        while (it.hasNext()) {
            it.next().close();
            it.remove();
        }
    }

    /**
     * Creates new chunk. Must update {@code outputChunk} and {@code chunks}
     * 
     * @return
     * @throws IOException
     */
    protected abstract boolean newChunk() throws IOException;

    private void waitCond() {
        condCharged = true;
        try {
            logger.debug("waitCond()");
            cond.await();
        } catch (InterruptedException e) {
            logger.debug("waitCond() interrupted.");
            Thread.interrupted();
            throw new IllegalStateException("The thread is interrupted", e);
        } finally {
            logger.debug("waitCond() done.");
            condCharged = false;
        }
    }

    protected long getTotalCapacity() {
        long total = 0L;
        for (Chunk chunk : chunks) {
            total += chunk.getCapacity();
        }
        return total;
    }

    protected long getToMarkLength() {
        if (markedChunk == null) {
            return 0L;
        }
        long len = -markedPos;
        int count = chunks.size();
        int idx = chunks.indexOf(markedChunk);
        for (int i = 0; i < count; ++count, idx = (idx + 1) % count) {
            Chunk chunk = chunks.get(idx);
            if (chunk == inputChunk) {
                break;
            }
            len += chunk.getCapacity();
        }
        len += inputChunk.getReadPosition();
        return len;
    }

    protected void writeJournalInfo() {
        try {
            journalInfoWriter.set(getJournalInfo());
        } catch (IOException e) {
            logger.warn("writeJournalInfo(): cannot write journal info ", e);
        }
    }

    private JournalInfo getJournalInfo() {
        Pair<Integer, Long> marker = new Pair<Integer, Long>(markedChunk != null ? markedChunk.getId() : 0, markedPos);
        Pair<Integer, Long> reader = new Pair<Integer, Long>(inputChunk.getId(), inputChunk.getReadPosition());
        Pair<Integer, Long> writer = new Pair<Integer, Long>(outputChunk.getId(), outputChunk.getWritePosition());
        return new JournalInfo(marker, reader, writer, readLimit);
    }

    protected void setJournalInfo(JournalInfo journalInfo) throws IOException {
        inputChunk = null;
        outputChunk = null;
        markedChunk = null;
        readLimit = journalInfo.getReadLimit();
        for (Chunk chunk : chunks) {
            if (chunk.getId() == journalInfo.getMarker().getFirst() && readLimit > 0) {
                markedChunk = chunk;
                markedPos = journalInfo.getMarker().getSecond();
            }

            if (chunk.getId() == journalInfo.getReader().getFirst()) {
                inputChunk = chunk;
                chunk.setReadPosition(journalInfo.getReader().getSecond());
            }

            if (chunk.getId() == journalInfo.getWriter().getFirst()) {
                outputChunk = chunk;
                chunk.setWritePosition(journalInfo.getWriter().getSecond());
            }
        }
        adjustChunksPositions();
    }

    protected int getNextChunkId(int id) {
        return id == Integer.MAX_VALUE ? 0 : id + 1;
    }

    private void adjustChunksPositions() throws IOException {
        lock.lock();
        try {
            for (Chunk chunk : chunks) {
                if (chunk.getReadPosition() != 0L && chunk != inputChunk) {
                    chunk.setReadPosition(0L);
                }
                if (chunk.getWritePosition() != chunk.getCapacity() && chunk != outputChunk) {
                    chunk.setWritePosition(chunk.getCapacity());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Journal{maxCapacity=").append(maxCapacity).append(", maxChunkSize=")
                .append(maxChunkSize).append(", folderName=").append(folderName).append(", prefixName=")
                .append(prefixName).append(", neverEof=").append(neverEof).append(", nextChunkId=").append(nextChunkId)
                .append(", inputChunk=").append(inputChunk).append(", outputChunk=").append(outputChunk)
                .append(", markedChunk=").append(markedChunk).append(", markedPos=").append(markedPos)
                .append(", readLimit=").append(readLimit).append(", journalInfoWriter=").append(journalInfoWriter)
                .append("}").toString();
    }
}
