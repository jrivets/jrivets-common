package org.jrivets.io.channels;

public interface Channel {
    
    enum State {
        OPENING, CONNECTING, OPEN, CLOSED
    };

    void open(Object channelEventsListener);
    
    void close();
    
    State getState();
    
    void setChannelWriter(ChannelWriter writer);
    
    void setChannelReader(ChannelReader reader);

}
