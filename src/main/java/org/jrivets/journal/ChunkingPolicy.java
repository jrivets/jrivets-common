package org.jrivets.journal;

import java.io.File;
import java.io.IOException;

import org.jrivets.log.LoggerFactory;

/**
 * Base chunking policy.
 * <p>
 * Stores journal information in multiple files (chunks) on disk, which
 * summarized size should not exceed {@code maxCapacity}. New file chunks will
 * not be bigger than {@code maxChunkSize}, but they can be less, so summarized
 * size of all chunks will not exceed {@code maxCapacity}.
 * <p>
 * This policy can be initialized with existing chunks on the disk. Existing
 * chunks can be bigger than {@code maxChunkSize}, but new ones will always be
 * with the size equals or less {@code maxChunkSize}. In case of summarized size
 * of all existing chunks exceeds the maximum capacity on the moment of
 * initialization, new chunks will not be created until existing chunks capacity
 * will be reduced to {@code maxCapacity} due to journal read and deletes.
 * 
 * @author Dmitry Spasibenko
 * 
 */
final class ChunkingPolicy extends AbstractChunkingPolicy {
    
    private final boolean singleWrite;

    ChunkingPolicy(long maxCapacity, long maxChunkSize, String folderName, String prefixName, boolean cleanAfterOpen, boolean singleWrite) throws IOException {
        super(LoggerFactory.getLogger(ChunkingPolicy.class, "(" + prefixName + ") %2$s", null), maxCapacity, maxChunkSize, folderName, prefixName, cleanAfterOpen);
        this.singleWrite = singleWrite;
        init(cleanAfterOpen);
        logger.info("New ChunkingPolicy: ", this);
    }

    private void init(boolean cleanAfterOpen) throws IOException {
        JournalInfo journalInfo = cleanAfterOpen ? JournalInfo.NULL_INFO : journalInfoWriter.get();
        if (JournalInfo.NULL_INFO.equals(journalInfo)) {
            newChunk();
            setJournalInfo(journalInfo);
            return;
        }
        int startId = journalInfo.getReadLimit() > 0 ? journalInfo.getMarker().getFirst() : journalInfo.getReader().getFirst();
        for (int id = startId; id <= journalInfo.getWriter().getFirst(); id++) {
            File file = new File(folderName, prefixName + id);
            if (!file.exists()) {
                throw new IllegalStateException("Could not find file-chunk " + file);
            }
            long capacity = file.length();
            Chunk chunk = new Chunk(id, capacity, file, true, singleWrite);
            chunks.add(chunk);
        }
        nextChunkId = getNextChunkId(journalInfo.getWriter().getFirst());
        setJournalInfo(journalInfo);
    }

    @Override
    void mark(int readLimit) {
        super.mark(readLimit);
        cleanUpChunks();
    }

    @Override
    Chunk adjustInputChunk() throws IOException {
        Chunk result = super.adjustInputChunk();
        cleanUpChunks();
        return result;
    }
    
    /**
     * updates outputChunk
     * 
     * @return
     * @throws IOException
     */
    @Override
    protected boolean newChunk() throws IOException {
        File file = new File(folderName, prefixName + nextChunkId);
        long total = getTotalCapacity();
        long capacity = Math.min(maxCapacity - total, maxChunkSize);
        logger.debug("newChunk(): total=", total, ", capacity=", capacity);
        if (capacity <= 0) {
            return false;
        }
        if (outputChunk != null) {
            outputChunk.closeOut(); // previous one is not going to be used anymore
        }
        outputChunk = new Chunk(nextChunkId, capacity, file, false, singleWrite);
        chunks.add(outputChunk);
        nextChunkId = getNextChunkId(nextChunkId);
        logger.debug("newChunk(): New chunk is creaged ", outputChunk, ", nextChunkId=", nextChunkId);
        return true;
    }
    
    private void cleanUpChunks() {
        logger.debug("cleanUpChunks(): ", chunks.size(), " chunks before.");
        while (chunks.get(0) != markedChunk && chunks.get(0) != inputChunk) {
            chunks.get(0).delete();
            chunks.remove(0);
        }
        logger.debug("cleanUpChunks(): ", chunks.size(), " chunks after.");
    }
}
