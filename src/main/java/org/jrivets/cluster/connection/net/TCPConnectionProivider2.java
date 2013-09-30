package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.jrivets.cluster.connection.AcceptedConnectionEvent;
import org.jrivets.event.Subscriber;
import org.jrivets.log.LoggerFactory;

final class TCPConnectionProivider2 {

    private final Map<SelectableChannel, TCPConnection2> connectionMap = new HashMap<SelectableChannel, TCPConnection2>();

    private final AcceptSelectCycle acceptCycle;

    final ReadSelectCycle readCycle;

    final InboundEventsDistributor distributor;

    private final Subscriber acceptSubscriber;

    class ReadSelectCycle extends SelectorCycle {

        protected ReadSelectCycle() throws IOException {
            super(LoggerFactory.getLogger(ReadSelectCycle.class));
        }

        @Override
        protected void onSelect(SelectionKey sKey) {
            try {
                TCPConnection2 connection = connectionMap.get(sKey.channel());

                if (sKey.isValid() && sKey.isConnectable()) {
                    connection.onConnectReady();
                }

                if (sKey.isValid() && sKey.isReadable()) {
                    connection.onReadReady();
                }

                if (sKey.isValid() && sKey.isWritable()) {
                    connection.onWriteReady();
                }
            } catch (Exception ioe) {
                // TODO: fix me...
            }
        }

        TCPConnection2 createNewConnection(SocketChannel channel, final SocketAddress remoteAddress) {
            final TCPConnection2 newConnection = new TCPConnection2(channel, remoteAddress,
                    TCPConnectionProivider2.this);
            onCommand(new Runnable() {
                @Override
                public void run() {
                    // TODO: move it to another method
                    connectionMap.put(newConnection.socketChannel, newConnection);
                    if (newConnection.isServerConnection()) {
                        InboundEventHolder holder = new InboundEventHolder(acceptSubscriber,
                                new AcceptedConnectionEvent(newConnection));
                        try {
                            distributor.put(holder);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            return newConnection;
        }

    }

    class AcceptSelectCycle extends SelectorCycle {

        private final ServerSocketChannel serverSocketChannel;

        AcceptSelectCycle(int listenOn) throws IOException {
            super(LoggerFactory.getLogger(AcceptSelectCycle.class, "(p:" + listenOn + ") %2$s", null));
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.socket().bind(new InetSocketAddress(listenOn));
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, SelectionKey.OP_CONNECT);
        }

        @Override
        protected void onSelect(SelectionKey sKey) {
            try {
                if (sKey.isValid() && sKey.isAcceptable()) {
                    readCycle.createNewConnection(serverSocketChannel.accept(), null);
                }
            } catch (IOException ioe) {
                logger.error("Cannot accept new channel ", ioe);
            }
        }

        @Override
        public String toString() {
            return "{listen=" + serverSocketChannel.socket().getLocalSocketAddress() + "}";
        }
    }

    TCPConnectionProivider2(int listenOn, InboundEventsDistributor distributor, Object acceptListener)
            throws IOException {
        if (listenOn > 0) {
            this.acceptSubscriber = new Subscriber(acceptListener);
            this.acceptCycle = new AcceptSelectCycle(listenOn);
            this.acceptCycle.start();
        } else {
            this.acceptSubscriber = null;
            this.acceptCycle = null;
        }
        this.distributor = distributor;
        this.readCycle = new ReadSelectCycle();
        this.readCycle.start();
    }

    TCPConnection2 createClientConnection(SocketAddress remoteAddress) {
        try {
            return readCycle.createNewConnection(SocketChannel.open(), remoteAddress);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{server=").append(acceptCycle).append(", rwCycle=").append(readCycle)
                .append(", distributor=").append(distributor).append(", connectors=").append(connectionMap.size())
                .append("}").toString();
    }

}
