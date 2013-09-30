package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.jrivets.cluster.connection.AcceptedConnectionEvent;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.ConnectionProvider;
import org.jrivets.cluster.connection.net.TCPConnection.State;
import org.jrivets.event.Subscriber;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class TCPConnectionProvider implements ConnectionProvider {

    private final Logger logger;

    private final ServerSocketChannel serverSocketChannel;

    final Selector selector;

    private final Selector acceptSelector;

    private final ExecutorService executor;

    final InboundEventsDistributor distributor;

    private final M1FixedSizeQueue<Runnable> commandQueue;

    private final Subscriber acceptSubscriber;

    private final Map<SelectionKey, TCPConnection> connectionMap = new HashMap<SelectionKey, TCPConnection>();

    private long nextRemoveCheck;

    private boolean active = true;

    private Runnable kickMainSelector = new Runnable() {
        @Override
        public void run() {
            selector.wakeup();
        }
    };

    public TCPConnectionProvider(int listenOn, ExecutorService executor, InboundEventsDistributor distributor,
            Object acceptListener) throws Exception {
        this.executor = executor;
        this.distributor = distributor;
        this.commandQueue = new M1FixedSizeQueue<Runnable>(500);
        this.acceptSubscriber = new Subscriber(acceptListener);

        this.selector = Selector.open();
        if (listenOn > 0) {
            this.acceptSelector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.socket().bind(new InetSocketAddress(listenOn));
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
            this.logger = LoggerFactory.getLogger(TCPConnectionProvider.class, "("
                    + this.serverSocketChannel.socket().getLocalSocketAddress() + ") %2$s", null);
        } else {
            this.acceptSelector = null;
            this.serverSocketChannel = null;
            this.logger = LoggerFactory.getLogger(TCPConnectionProvider.class, "(clients only) %2$s", null);
        }
        logger.info("New provider ", this);
        runAllCycles();
    }

    @Override
    public Connection createClientConnection(SocketAddress remoteAddress) {
        try {
            return newConnection(SocketChannel.open(), remoteAddress);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    void onCommand(Runnable command) {
        try {
            commandQueue.put(command, kickMainSelector);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ie);
        }
    }

    void removeConnectionFromMap(SelectionKey sKey) {
        connectionMap.remove(sKey);
    }

    private boolean isActive() {
        return active;
    }

    private void runAllCycles() throws Exception {
        try {
            if (serverSocketChannel != null) {
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        acceptCycle();
                    }
                });
            }
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    mainCycle();
                }
            });

        } catch (Exception ex) {
            logger.error("Cannot run accept and main cycles ", ex);
            active = false;
            throw ex;
        }
    }

    private void acceptCycle() {
        logger.info("Entering to Accept Cycle");
        try {
            while (isActive() && !Thread.currentThread().isInterrupted()) {
                acceptSelector.select();
                Iterator<SelectionKey> keyIterator = acceptSelector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey sKey = keyIterator.next();
                    keyIterator.remove();
                    if (sKey.isValid() && sKey.isAcceptable()) {
                        acceptNewConnection();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Finishing main cycle by the exception ", e);
        } finally {
            active = false;
        }
    }

    private void mainCycle() {
        logger.info("Entering to Main Cycle");
        try {
            while (isActive() && !Thread.currentThread().isInterrupted()) {
                removeConnectionsByTimeout();
                commandCycle();
                selectCycle();
            }
        } catch (Exception e) {
            logger.error("Finishing main cycle by the exception ", e);
        } finally {
            active = false;
            closeAllConnections();
        }
    }

    private void selectCycle() throws IOException, InterruptedException {
        selector.select();
        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey sKey = keyIterator.next();
            keyIterator.remove();

            TCPConnection connection = connectionMap.get(sKey);
            if (sKey.isValid() && connection == null) {
                throw new IllegalStateException("Selection key with no connection: " + sKey);
            }

            if (sKey.isValid() && sKey.isConnectable()) {
                connection.onConnectReady();
            }

            if (sKey.isValid() && sKey.isReadable()) {
                connection.onReadReady();
            }

            if (sKey.isValid() && sKey.isWritable()) {
                connection.onWriteReady();
            }
        }
    }

    private void commandCycle() {
        Runnable command = commandQueue.poll();
        while (command != null) {
            try {
                command.run();
            } catch (Throwable t) {
                logger.error("Command throws the error ", t);
            }
            command = commandQueue.poll();
        }
    }

    private void acceptNewConnection() {
        try {
            newConnection(serverSocketChannel.accept(), null);
        } catch (Exception ex) {
            logger.error("Cannot accept new connection ", ex);
        }
    }

    private TCPConnection newConnection(final SocketChannel socketChannel, SocketAddress remoteAddress)
            throws IOException {
        socketChannel.configureBlocking(false);
        final TCPConnection connection = new TCPConnection(this, 100, 64*1024, socketChannel, null, remoteAddress);
        onCommand(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.setSKey(socketChannel.register(selector, 0));
                    connectionMap.put(connection.getSKey(), connection);
                    if (connection.isServerConnection()) {
                        logger.info("New connection is accepted ", connection);
                        InboundEventHolder holder = new InboundEventHolder(acceptSubscriber,
                                new AcceptedConnectionEvent(connection));
                        distributor.put(holder);
                    }
                } catch (Exception e) {
                    logger.error("Cannot complete new connection ", connection);
                    connection.closeConnection();
                }
            }
        });
        return connection;
    }

    private void closeAllConnections() {
        logger.info("Closing all connections");
        ArrayList<TCPConnection> connections = new ArrayList<TCPConnection>(connectionMap.values());
        for (TCPConnection connection : connections) {
            connection.closeConnection();
        }
    }

    private void removeConnectionsByTimeout() {
        long now = System.currentTimeMillis();
        if (nextRemoveCheck > now) {
            return;
        }
        nextRemoveCheck = now + TimeUnit.MINUTES.toMillis(1);
        ArrayList<TCPConnection> connections = new ArrayList<TCPConnection>(connectionMap.values());
        for (TCPConnection connection : connections) {
            State state = connection.getState();
            if (state == State.CONNECTING && connection.getStateSince() < (now - TimeUnit.MINUTES.toMillis(2))) {
                logger.warn("Close connection by connection timeout ", connection);
                connection.closeConnection();
            }
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{server=")
                .append(serverSocketChannel == null ? null : serverSocketChannel.socket().getLocalSocketAddress())
                .append(", cmdQueue=").append(commandQueue).append(", distributor=").append(distributor)
                .append(", connectors=").append(connectionMap.size()).append(", active=").append(active).append("}")
                .toString();
    }

}
