package org.jrivets.cluster.connection;

public abstract class ConnectionEvent {
    
    private final Connection connection;
    
    protected ConnectionEvent(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
    
}
