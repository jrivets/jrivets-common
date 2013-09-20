package org.jrivets.cluster.connection;

public interface ConnectionEndpoint {

    void onRead(ReadFromConnectionEvent readEvent);
    
}
