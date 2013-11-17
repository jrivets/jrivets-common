package org.jrivets.cluster.connection;

import org.jrivets.io.channels.ReadFromChannelEvent;

public interface ConnectionEndpoint {

    void onRead(ReadFromChannelEvent readEvent);
    
}
