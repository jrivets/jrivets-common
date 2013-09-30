package org.jrivets.cluster.connection.net;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jrivets.cluster.connection.ClosedConnectionEvent;
import org.jrivets.cluster.connection.ConnectedConnectionEvent;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.event.Subscriber;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class TCPConnection implements Connection {

    private final static String NO_INTERESTS_MASK = "---";

    private final Logger logger;

    private static int channelNumber;

    private final int cId = ++channelNumber;

    private final M1FixedSizeQueue<byte[]> sendBuffer;

    private final TCPConnectionProvider provider;

    private final SocketChannel socketChannel;

    private SelectionKey sKey;

    private ByteBuffer outboundPacket; // Only from main thread
    
    private volatile boolean writable;

    private final ByteBuffer inboundPacket;

    private Subscriber subscriber;

    private volatile State state = State.OPENING;

    private long stateSince;

    private final SocketAddress remoteAddress;

    private String interestsMask = NO_INTERESTS_MASK;

    private Runnable writeKick = new Runnable() {
        public void run() {
            if (!writable) {
                provider.onCommand(setOpWriteCmd);
                
            }
        }
    };
    
    private Runnable setOpWriteCmd = new Runnable() {
        @Override
        public void run() {
            setInterests(SelectionKey.OP_WRITE, true);
        }
    };

    private Runnable closeCmd = new Runnable() {
        @Override
        public void run() {
            closeConnection();
        }
    };

    enum State {
        OPENING, CONNECTING, OPEN, CLOSED
    };

    TCPConnection(TCPConnectionProvider provider, int sendBufferQueueSize, int readBufferSize,
            SocketChannel socketChannel, SelectionKey sKey, SocketAddress remoteAddress) throws ClosedChannelException {
        this.provider = provider;
        this.sendBuffer = new M1FixedSizeQueue<byte[]>(sendBufferQueueSize);
        this.inboundPacket = ByteBuffer.allocateDirect(readBufferSize);
        this.socketChannel = socketChannel;
        this.sKey = sKey;
        this.remoteAddress = remoteAddress; // null for server socket
        this.logger = LoggerFactory.getLogger(TCPConnection.class, "(C" + cId + " %1$s): %2$s", new Object() {
            public String toString() {
                return state.name() + " " + interestsMask;
            }
        });
        setState(State.OPENING);
    }

    @Override
    public void open(final Object connectionEventListener) {
        checkState(State.OPENING, "open the connection.");
        logger.debug("Get open request with", connectionEventListener, " as listener");
        provider.onCommand(new Runnable() {
            @Override
            public void run() {
                openConnection(connectionEventListener);
            }
        });
    }

    @Override
    public boolean write(byte[] data, boolean blocking) {
        checkState(State.OPEN, "send data in the state.");
        if (data != null) {
            try {
                if (blocking) {
                    sendBuffer.put(data, writeKick);
                    return true;
                }
                if (sendBuffer.offer(data)) {
                    writeKick.run();
                    return true;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ie);
            }
        }
        return false;
    }

    @Override
    public void close() {
        provider.onCommand(closeCmd);
    }

    // =========================================================================
    // Main cycle notifications
    // =========================================================================
    /**
     * Notification about channel readiness for writing. It must be invoked from
     * main cycle thread only.
     * 
     * @param key
     */
    void onWriteReady() {
        checkOutboundPacket();
        while (outboundPacket != null) {
            int wr = 0;
            try {
                while (true) {
                    int pos = outboundPacket.position();
                    wr = socketChannel.write(outboundPacket);
                    if (outboundPacket.position() - pos == 0) {
                        break;
                    }
                }
            } catch (Exception ex) {
                logger.debug("Exception while writing to channel, will close it ", ex);
                wr = -2;
            } finally {
                if (wr < 0) {
                    closeConnection();
                    return;
                }
            }
            if (outboundPacket.hasRemaining()) {
                // cannot write anymore for now
                return;
            }
            checkOutboundPacket();
        }
        setInterests(SelectionKey.OP_WRITE, false);
    }

    /**
     * Notification about readiness for reading. It must be invoked from main
     * cycle thread only.
     * 
     * @throws InterruptedException
     */
    void onReadReady() throws InterruptedException {
        int rr = 0;
        try {
            while (true) {
                int count = 0;
                do {
                    count = inboundPacket.position();
                    rr = socketChannel.read(inboundPacket);
                    count = inboundPacket.position() - count;
                } while (rr >= 0 && count > 0);

                boolean over = inboundPacket.hasRemaining();

                if (inboundPacket.position() > 0) {
                    byte[] packet = new byte[inboundPacket.position()];
                    inboundPacket.flip();
                    inboundPacket.get(packet);
                    inboundPacket.clear();
                    //sendEvent(new ReadFromConnectionEvent(this, packet));
                }
                if (over) {
                    return;
                }
            }
        } catch (Exception ex) {
            logger.debug("Exception while reading package, will close the connection ", ex);
            rr = -2;
        } finally {
            if (rr < 0) {
                closeConnection();
            }
        }
    }

    void onConnectReady() throws InterruptedException {
        if (state != State.CONNECTING) {
            logger.debug("Ignore onConnectReady() notification due to state. ");
            return;
        }
        try {
            socketChannel.finishConnect();
            setState(State.OPEN);
            logger.info("Complete client connection ", this);
            setInterests(SelectionKey.OP_READ | SelectionKey.OP_WRITE, true);
            sendEvent(new ConnectedConnectionEvent(this));
        } catch (InterruptedException ie) {
            throw ie;
        } catch (Exception ex) {
            logger.debug("fail to finish connection, close it.", ex);
            closeConnection();
        }
    }

    /**
     * Notification invoked by {@link InboundEventHolder} from worker thread
     * signaling the event completion. This is used for serial mode, to enable
     * further read processing.
     * <p>
     * This method should never throw to avoid some side-effects to
     * {@link InboundEventHolder}.
     */
    void onEventCompletion() {
        try {
            if (state == State.OPEN && (sKey.interestOps() & SelectionKey.OP_READ) == 0 && sKey.isValid()) {
                setInterests(SelectionKey.OP_READ, true);
            }
        } catch (Exception ex) {
            // Ignore it, probably channel is already closed
        }
    }

    SelectionKey getSKey() {
        return sKey;
    }

    void setSKey(SelectionKey sKey) {
        this.sKey = sKey;
    }

    long getStateSince() {
        return stateSince;
    }

    State getState() {
        return state;
    }

    boolean isServerConnection() {
        return remoteAddress == null;
    }

    /**
     * Closing the connection, must be called in main cycle thread only.
     */
    void closeConnection() {
        if (state == State.CLOSED) {
            return;
        }
        provider.removeConnectionFromMap(sKey);
        try {
            sendEvent(new ClosedConnectionEvent(this));
            sKey.cancel();
            sKey.channel().close();
        } catch (Exception ex) {
            logger.debug("Exception while closing channel ", ex);
        } finally {
            provider.removeConnectionFromMap(sKey);
            logger.info("Close connection ", this);
            setState(State.CLOSED);
        }
    }

    private void openConnection(Object listener) {
        if (state != State.OPENING) {
            logger.debug("Skip openning request due to state.");
            return;
        }
        this.subscriber = new Subscriber(listener);
        if (remoteAddress == null) {
            setState(State.OPEN);
            setInterests(SelectionKey.OP_READ | SelectionKey.OP_WRITE, true);
            logger.info("New server socket connection. ", this);
            return;
        }
        setState(State.CONNECTING);
        try {
            if (socketChannel.connect(remoteAddress)) {
                onConnectReady();
                return;
            }
            setInterests(SelectionKey.OP_CONNECT, true);
        } catch (Exception ex) {
            logger.debug("fail to invoke connect, close the socket.", ex);
            closeConnection();
        }
    }

    private void sendEvent(Object event) throws InterruptedException {
        InboundEventHolder holder = new InboundEventHolder(subscriber, event);
        provider.distributor.put(holder);
    }

    private void checkState(State requiredState, String opName) {
        if (state != requiredState) {
            throw new IllegalStateException("Wrong connection state: " + state + ", cannot " + opName);
        }
    }

    private void setState(State newState) {
        this.state = newState;
        this.stateSince = System.currentTimeMillis();
    }

    private void checkOutboundPacket() {
        writable = false;
        try {
            if (outboundPacket != null && outboundPacket.hasRemaining()) {
                return;
            }
            outboundPacket = null;
            byte[] buf = sendBuffer.size() > 0 ? sendBuffer.poll() : null;
            if (buf == null) {
                return;
            }
            outboundPacket = ByteBuffer.wrap(buf);
        } finally {
            writable = outboundPacket != null;
        }
    }

    private void setInterests(int mask, boolean set) {
        if (!sKey.isValid()) {
            interestsMask = NO_INTERESTS_MASK;
            return;
        }
        sKey.interestOps(set ? sKey.interestOps() | mask : sKey.interestOps() & (~mask));
        int flags = sKey.interestOps();
        writable = (flags & SelectionKey.OP_WRITE) != 0;
        interestsMask = ((flags & SelectionKey.OP_CONNECT) == 0 ? "-" : "c")
                + (!writable ? "-" : "w")
                + ((flags & SelectionKey.OP_READ) == 0 ? "-" : "r");
    }

    @Override
    public String toString() {
        return "{C" + cId + ": " + socketChannel.socket().getLocalSocketAddress() + "=>"
                + socketChannel.socket().getRemoteSocketAddress() + ", " + state + "}";
    }
}
