package org.jrivets.journal;

public class ChunkNotFoundException extends Exception {
   
    private static final long serialVersionUID = 7622872083069943924L;
    
    private final int chunkId;
    
    ChunkNotFoundException(int chunkId) {
        this.chunkId = chunkId;
    }
    
    public int getChunkId() {
        return chunkId;
    }
    
}
