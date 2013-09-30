package org.jrivets.cluster.connection.net;

import java.nio.ByteBuffer;

final class InputStreamPacketizer {
    
    private final byte[] inputBuffer;
    
    private final ByteBuffer inputByteBuffer;
    
    private ByteArrayOutputStream packet;
    
    private int readPos;
    
    private int totalRead;
    
    private int size;
    
    InputStreamPacketizer(int inputBufferSize) {
        this.inputByteBuffer = ByteBuffer.allocate(inputBufferSize);
        this.inputBuffer = inputByteBuffer.array();
    }
    
    ByteBuffer getInputByteBuffer() {
        return inputByteBuffer;
    }

    byte[] getPacket() {
        if (packetIsNotReady()) {
            parseBuffer();
            if (packetIsNotReady()) {
                return null;
            }
        }
        return packet.getBuffers().get(0);
    }
    
    void reset() {
        totalRead = 0;
        packet = null;
    }
    
    private boolean packetIsNotReady() {
        return packet == null || packet.size() < packet.capacity();
    }
    
    private void parseBuffer() {
        if (!inputByteBuffer.hasRemaining() && readPos == inputByteBuffer.position()) {
            inputByteBuffer.clear();
            readPos = 0;
        }
        parseSize();
        parsePacket();
    }
    
    private void parseSize() {
        while (totalRead < 4 && readPos < inputByteBuffer.position()) {
            size = (size << 8) | (inputBuffer[readPos++]&0xff);
            totalRead++;
        }
    }
    
    private void parsePacket() {
        if (totalRead < 4) {
            return;
        }
        if (totalRead == 4) {
            packet = new ByteArrayOutputStream(size);
        }
        int length = Math.min(packet.capacity() - packet.size(), inputByteBuffer.position() - readPos);
        if (length > 0) {
            packet.write(inputBuffer, readPos, length);
            totalRead += length;
            readPos += length;
        }
    }
    
}
