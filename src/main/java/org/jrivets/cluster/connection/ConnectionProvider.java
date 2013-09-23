package org.jrivets.cluster.connection;

import org.jrivets.cluster.Address;

public interface ConnectionProvider {

    Connection createClientConnection(Address address);
    
}
