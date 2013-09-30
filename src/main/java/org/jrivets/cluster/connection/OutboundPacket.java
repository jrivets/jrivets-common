package org.jrivets.cluster.connection;

import java.io.OutputStream;

public interface OutboundPacket {

    public OutputStream getOutputStream();
}
