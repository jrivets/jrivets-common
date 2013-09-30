package org.jrivets.cluster.connection;

public class ClosedConnectionEvent extends ConnectionEvent {

    public ClosedConnectionEvent(Connection connection) {
        super(connection);
    }

}
