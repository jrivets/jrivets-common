package org.jrivets.cluster.handshake;

import org.jrivets.io.channels.ReadFromChannelEvent;
import org.jrivets.util.container.Pair;

public interface HandshakeChecker {
    
    enum CheckResult {FAIL, NEED_MORE, OK, OK_SERIAL};

    Pair<CheckResult, Object> onHandshakePacket(ReadFromChannelEvent readEvent);
    
    void onConnected(HandshakeConnection connection);
    
    void onDisconnected(HandshakeConnection hsConnection);
    
}
