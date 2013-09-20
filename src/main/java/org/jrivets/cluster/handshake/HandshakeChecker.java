package org.jrivets.cluster.handshake;

import org.jrivets.cluster.connection.ReadFromConnectionEvent;
import org.jrivets.util.container.Pair;

public interface HandshakeChecker {
    
    enum CheckResult {FAIL, NEED_MORE, OK, OK_SERIAL};

    Pair<CheckResult, Object> onHandshakePacket(ReadFromConnectionEvent readEvent);
    
    void onConnected(HandshakeConnection connection);
    
    void onDisconnected(HandshakeConnection hsConnection);
    
}
