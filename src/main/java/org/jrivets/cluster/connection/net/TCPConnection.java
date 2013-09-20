package org.jrivets.cluster.connection.net;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.OutboundPacket;

final class TCPConnection implements Connection {

    private final TCPConnectionProvider provider;
    
    private final BlockingQueue<OutboundPacket> sendBuffer;
    
    private final Lock lock = new ReentrantLock();
    
    private final SocketChannel channel;
    
    private final int interestSet;
    
    TCPConnection(TCPConnectionProvider provider, BlockingQueue<OutboundPacket> sendBuffer, SocketChannel channel, boolean serverSocket) {
        this.provider = provider;
        this.sendBuffer = sendBuffer;
        this.channel = channel;
        if (serverSocket) {
            this.interestSet = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
        } else {
            this.interestSet = SelectionKey.OP_CONNECT |SelectionKey.OP_READ | SelectionKey.OP_WRITE;            
        }
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
        //sendBuffer.put(outboundPacket);
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
