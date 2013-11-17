package org.jrivets.io.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jrivets.io.channels.Channel.State;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class CachedChannelWriter implements ChannelWriter {
    
    private final Logger logger = LoggerFactory.getLogger(CachedChannelWriter.class);
    
    private final MOFixedSizeQueue<byte[]> sendBuffer;

    private ByteBuffer sendBuf;

    private volatile boolean opWrite;
    
    private final TCPChannel channel;
    
    private final Runnable writeKick = new Runnable() {
        public void run() {
            if (!opWrite) {
                channel.enableWriting(true);
            }
        }
    };
    
    public CachedChannelWriter(Channel channel, int queueSize) {
        this.channel = (TCPChannel) channel;
        this.sendBuffer = new MOFixedSizeQueue<byte[]>(queueSize);
    }

    public boolean write(byte[] buffer, boolean blocking) {
        State state = channel.getState();
        if (state != State.OPEN) {
            throw new IllegalStateException("Channel state is not OPEN, but " + state);
        }
        boolean result = true;
        try {
            if (blocking) {
                sendBuffer.put(buffer, writeKick);
            } else {
                result = sendBuffer.offer(buffer);
                writeKick.run();
            }
        } catch (InterruptedException e) {
            logger.warn("Cannot finish write - the thread is interrupted ", channel);
            Thread.currentThread().interrupt();
        }
        return result;
    }
    
    @Override
    public boolean onWriteReady(TCPChannel channel) throws IOException {
        prepareSendBuf();
        while (sendBuf != null) {
            while (true) {
                int pos = sendBuf.position();
                channel.socketChannel.write(sendBuf);
                int count = sendBuf.position() - pos;
                if (count == 0) {
                    break;
                }
            }
            if (sendBuf.hasRemaining()) {
                return true;
            }
            prepareSendBuf();
        }
        return false;
    }

    @Override
    public void onWriteEnabled(boolean writeEnabled) {
        opWrite = writeEnabled;
    }
    
    private void prepareSendBuf() {
        opWrite = false;
        try {
            if (sendBuf != null && sendBuf.hasRemaining()) {
                return;
            }
            byte[] buf = sendBuffer.poll();
            sendBuf = buf != null ? ByteBuffer.wrap(buf) : null;
        } finally {
            opWrite = sendBuf != null;
        }
    }

}
