package org.jrivets.io.channels;

public abstract class ChannelEvent {
    
    private final Channel channel;
    
    protected ChannelEvent(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }
    
}
