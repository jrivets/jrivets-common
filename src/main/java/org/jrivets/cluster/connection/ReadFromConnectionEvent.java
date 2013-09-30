package org.jrivets.cluster.connection;

import java.nio.ByteBuffer;

public final class ReadFromConnectionEvent extends ConnectionEvent {
    
    private final ByteBuffer buffer; 

    public ReadFromConnectionEvent(Connection connection, ByteBuffer buffer) {
        super(connection);
        this.buffer = buffer;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }
}
