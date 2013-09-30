package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jrivets.cluster.connection.OutboundPacket;


/**
 * Outbound packet to send data through a connection to remote party
 * <p>
 * The implementation is not thread safe for performance purposes. The use case
 * of the class is as follows: one thread, which is going to send some data
 * prepare the data and store it into the packet. Normally new data is added to
 * the end of packet, but sender would want to add new data before already
 * written one. {@link addAtHead()} method is used for the purpose. Packet
 * should be switched to sending mode as soon as the sender is done with adding
 * data. No new data is allowed in sending mode. Another thread can actually
 * send data through a connection using ByteBuffer, which is available in
 * sending mode only. All packet data is send followed by 4-bytes length of the
 * packet, which always goes first.
 * 
 * @author Dmitry Spasibenko
 * 
 */
final class OutboundStreamPacketizer implements OutboundPacket {

    private final ArrayList<ByteArrayOutputStream> chunks = new ArrayList<ByteArrayOutputStream>(5);

    private int chunkIdx;

    private int bufferIdx;

    private ByteBuffer byteBuffer;

    private boolean sending;

    OutboundStreamPacketizer(ByteArrayOutputStream chunk) {
        addAtHead(chunk);
    }

    void addAtHead(ByteArrayOutputStream chunk) {
        checkNotSending();
        if (chunk == null) {
            throw new NullPointerException();
        }
        chunks.add(chunk);
    }

    @Override
    public ByteArrayOutputStream getOutputStream() {
        checkNotSending();
        return chunks.get(0);
    }

    void switchToSending() {
        int size = 0;
        for (ByteArrayOutputStream s : chunks) {
            size += s.size();
        }
        sending = size > 0;
        if (sending) {
            addSizeValue(size);
            chunkIdx = chunks.size() - 1;
            bufferIdx = 0;
            byteBuffer = null;
        }
    }

    ByteBuffer getBuffer() {
        if (!sending) {
            return null;
        }
        if (byteBuffer == null || !byteBuffer.hasRemaining()) {
            switchToNextBuffer();
        }
        return byteBuffer;
    }

    private void switchToNextBuffer() {
        while (chunkIdx >= 0) {
            ByteArrayOutputStream stream = chunks.get(chunkIdx);
            ArrayList<byte[]> buffers = stream.getBuffers();
            while (bufferIdx < buffers.size()) {
                byte[] buffer = buffers.get(bufferIdx);
                int length = bufferIdx < buffers.size() - 1 ? buffer.length : stream.getPosition();
                ++bufferIdx;
                if (length > 0) {
                    byteBuffer = ByteBuffer.wrap(buffer, 0, length);
                    return;
                }
            }
            --chunkIdx;
            bufferIdx = 0;
        }
        byteBuffer = null;
    }

    private void checkNotSending() {
        if (sending) {
            throw new IllegalStateException("Packet is in sending state.");
        }
    }

    private void addSizeValue(int size) {
        ByteArrayOutputStream sizeBuf = new ByteArrayOutputStream(4);
        try {
            sizeBuf.write((byte) ((size >> 24) & 0xff));
            sizeBuf.write((byte) ((size >> 16) & 0xff));
            sizeBuf.write((byte) ((size >> 8) & 0xff));
            sizeBuf.write((byte) (size & 0xff));
            chunks.add(sizeBuf);
        } catch (IOException e) {
            throw new IllegalStateException("Should never happen with the code", e);
        }
    }
}
