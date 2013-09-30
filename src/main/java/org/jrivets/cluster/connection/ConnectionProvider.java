package org.jrivets.cluster.connection;

import java.net.SocketAddress;

public interface ConnectionProvider {

    Connection createClientConnection(SocketAddress address);
    
}
