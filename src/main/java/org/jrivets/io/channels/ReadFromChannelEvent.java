package org.jrivets.io.channels;

import java.nio.ByteBuffer;

public final class ReadFromChannelEvent extends ChannelEvent {
    
    private final ByteBuffer buffer; 

    public ReadFromChannelEvent(Channel channel, ByteBuffer buffer) {
        super(channel);
        this.buffer = buffer;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }
}
