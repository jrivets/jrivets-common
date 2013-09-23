package org.jrivets.cluster.connection.net;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.OutboundPacket;
import org.jrivets.collection.RingBuffer;

final class TCPConnection implements Connection {

    private final RingBuffer<OutboundStreamPacketizer> sendBuffer; // should block write ops!!!
    
    private final TCPConnectionProvider provider;
    
    private SocketChannel socketChannel;
    
    private SelectionKey sKey;
    
    private OutboundStreamPacketizer currentPacket; // Only from main thread

    TCPConnection(TCPConnectionProvider provider, int sendBufferSize) throws ClosedChannelException {
        this.provider = provider;
        this.sendBuffer = new RingBuffer<OutboundStreamPacketizer>(sendBufferSize);
    }

    @Override
    public void open(Object connectionEventListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSerial(boolean serialDelivery) {
        // TODO Auto-generated method stub

    }

    @Override
    public void send(OutboundPacket outboundPacket) {
        OutboundStreamPacketizer osp = (OutboundStreamPacketizer) outboundPacket;
        osp.switchToSending();
        if (osp.getBuffer() != null) {
            sendBuffer.add(osp);
            sKey.interestOps(sKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    @Override
    public OutboundPacket newOutboundPacket() {
        return new OutboundStreamPacketizer(new ByteArrayOutputStream());
    }

    @Override
    public void close() {
        //TODO: do it from main Cycle
    }
    
    //=========================================================================
    // Main cycle notifications
    //=========================================================================
    void onWriteReady(SelectionKey key) {
        for (ByteBuffer sendBuffer = getBufferToSend(); sendBuffer != null; sendBuffer = getBufferToSend()) {
           //TODO: handle the exception and uncomment while (socketChannel.write(sendBuffer) > 0);
            if (sendBuffer.hasRemaining()) {
                // cannot write anymore for now
                return;
            }
        }
        key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
    }
    
    private ByteBuffer getBufferToSend() {
        while(true) {
            while (currentPacket == null && sendBuffer.size() > 0) {
                currentPacket = sendBuffer.removeFirst();
            }
            if (currentPacket == null) {
                return null;
            }
            if (currentPacket.getBuffer() != null) {
                return currentPacket.getBuffer();
            }
            currentPacket = null;
        }
    }
}
