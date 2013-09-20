package org.jrivets.cluster.connection;

public final class ConnectedConnectionEvent extends ConnectionEvent {

    ConnectedConnectionEvent(Connection connection) {
        super(connection);
    }
}
