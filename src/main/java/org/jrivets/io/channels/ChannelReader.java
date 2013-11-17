package org.jrivets.io.channels;

import java.io.IOException;

interface ChannelReader {

    void onReadReady(TCPChannel channel) throws IOException;
    
}
