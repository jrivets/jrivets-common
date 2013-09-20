package org.jrivets.cluster.connection;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public final class OutboundPacket {

    private final ArrayList<ByteArrayOutputStream> chunks = new ArrayList<ByteArrayOutputStream>(5);
    
    private int chunkIdx;
    
    private int bufferIdx;
    
    private ByteBuffer byteBuffer;
    
    public OutboundPacket(ByteArrayOutputStream chunk) {
        addAtHead(chunk);
    }
    
    public void addAtHead(ByteArrayOutputStream chunk) {
        if (chunk == null) {
            throw new NullPointerException();
        }
        chunks.add(chunk);
    }
    
    public ByteArrayOutputStream getOutputStream() {
        return chunks.get(0);
    }
    
    public ByteBuffer getBuffer() {
        if (byteBuffer == null || !byteBuffer.hasRemaining()) {
            switchToNextBuffer();
        }
        return byteBuffer;
    }
    
    private void switchToNextBuffer() {
        if (byteBuffer == null) {
            chunkIdx = chunks.size() - 1;
        }
        while (chunkIdx >= 0) {
            ByteArrayOutputStream stream = chunks.get(chunkIdx); 
            ArrayList<byte[]> buffers = stream.getBuffers();
            if (bufferIdx < buffers.size()) {
                byte[] buffer = buffers.get(bufferIdx);
                int length = bufferIdx < buffers.size()-1 ? buffer.length : stream.getPosition();
                byteBuffer = ByteBuffer.wrap(buffer, 0, length);
                ++bufferIdx;
                return;
            }
            --chunkIdx;
            bufferIdx=0;
        }
        byteBuffer = null;
    }
}
