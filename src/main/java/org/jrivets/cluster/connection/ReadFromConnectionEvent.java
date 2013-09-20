package org.jrivets.cluster.connection;

public final class ReadFromConnectionEvent extends ConnectionEvent {
    
    private byte[] data; 

    ReadFromConnectionEvent(Connection connection, byte[] data) {
        super(connection);
        this.data = data;
    }
}
