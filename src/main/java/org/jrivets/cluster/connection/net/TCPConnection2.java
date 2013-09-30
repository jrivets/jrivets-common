package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jrivets.cluster.connection.ConnectedConnectionEvent;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.ReadFromConnectionEvent;
import org.jrivets.event.Subscriber;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class TCPConnection2 implements Connection {

    private final static String NO_INTERESTS_MASK = "---";

    private final Logger logger;

    private static int channelNumber;

    private final int cId = ++channelNumber;

    final SocketChannel socketChannel;

    private final SocketAddress remoteAddress;

    volatile SelectionKey sKey;

    private M1FixedSizeQueue<byte[]> sendBuffer = new M1FixedSizeQueue<byte[]>(500);

    long sendCount;

    private ByteBuffer sendBuf;

    private volatile boolean opWrite;

    private ByteBuffer recvBuf = ByteBuffer.allocate(64 * 1024);

    private final TCPConnectionProivider2 provider;

    private Subscriber subscriber;

    private String interestsMask = NO_INTERESTS_MASK;

    private volatile State state = State.OPENING;

    private long stateSince;

    enum State {
        OPENING, CONNECTING, OPEN, CLOSED
    };

    private Runnable writeKick = new Runnable() {
        public void run() {
            if (!opWrite) {
                provider.readCycle.onCommand(setOpWriteCmd);
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

    TCPConnection2(SocketChannel socketChannel, SocketAddress remoteAddress, TCPConnectionProivider2 provider) {
        this.socketChannel = socketChannel;
        this.sKey = sKey;
        this.provider = provider;
        this.remoteAddress = remoteAddress; // null for server socket
        this.logger = LoggerFactory.getLogger(TCPConnection.class, "(C" + cId + " %1$s): %2$s", new Object() {
            public String toString() {
                return state.name() + " " + interestsMask;
            }
        });
        setState(State.OPENING);
    }

    @Override
    public boolean write(byte[] buffer, boolean blocking) {
        checkState(State.OPEN, "Connection is not open.");
        boolean result = true;
        try {
            if (blocking) {
                sendBuffer.put(buffer, writeKick);
            } else {
                result = sendBuffer.offer(buffer);
                writeKick.run();
            }
        } catch (InterruptedException e) {
            logger.warn("Cannot finish write - the thread is interrupted ");
            Thread.currentThread().interrupt();
        }
        return result;
    }

    @Override
    public void open(final Object connectionEventListener) {
        if (connectionEventListener == null) {
            throw new NullPointerException();
        }
        checkState(State.OPENING, "open the connection.");
        logger.debug("Get open request with ", connectionEventListener, " as a listener");
        provider.readCycle.onCommand(new Runnable() {
            @Override
            public void run() {
                openConnection(connectionEventListener);
            }
        });
    }

    @Override
    public void close() {
        provider.readCycle.onCommand(closeCmd);
    }

    void onConnectReady() throws IOException, InterruptedException {
        socketChannel.finishConnect();
        setState(State.OPEN);
        setInterests(SelectionKey.OP_READ | SelectionKey.OP_WRITE, true);
        sendEvent(new ConnectedConnectionEvent(this));
    }

    void onReadReady() throws IOException {
        int rr = 0;
        try {
            while (true) {
                int count = 0;
                do {
                    count = recvBuf.position();
                    rr = socketChannel.read(recvBuf);
                    count = recvBuf.position() - count;
                } while (rr >= 0 && count > 0);

                boolean over = recvBuf.hasRemaining();

                if (recvBuf.position() > 0) {
                    //byte[] packet = new byte[recvBuf.position()];
                    recvBuf.flip();
                    subscriber.notifySubscriberSilently(new ReadFromConnectionEvent(this, recvBuf));
                    //recvBuf.get(packet);
                    //sendEvent(new ReadFromConnectionEvent(this, packet));
                }
                recvBuf.clear();
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

    void onWriteReady() throws IOException {
        prepareSendBuf();
        while (sendBuf != null) {
            while (true) {
                int pos = sendBuf.position();
                socketChannel.write(sendBuf);
                int count = sendBuf.position() - pos;
                if (count == 0) {
                    break;
                }
                sendCount += count;
            }
            if (sendBuf.hasRemaining()) {
                return;
            }
            prepareSendBuf();
        }
        setInterests(SelectionKey.OP_WRITE, false);
    }

    boolean isServerConnection() {
        return remoteAddress == null;
    }

    private void prepareSendBuf() {
        opWrite = false;
        try {
            if (sendBuf != null && sendBuf.hasRemaining()) {
                return;
            }
            byte[] buf = sendBuffer.poll();
            sendBuf = buf != null ? ByteBuffer.wrap(buf) : null;
        } finally {
            opWrite = sendBuf != null;
        }
    }

    void closeConnection() {
        if (state == State.CLOSED) {
            return;
        }
        // TODO: remove from provider list
        try {
            if (sKey != null) {
                sKey.cancel();
            }
            socketChannel.close();
        } catch (IOException e) {

        } finally {
            setState(State.CLOSED);
        }
    }

    private void openConnection(Object listener) {
        if (state != State.OPENING) {
            logger.debug("Skip openning request due to state.");
            return;
        }
        try {
            socketChannel.configureBlocking(false);
            sKey = socketChannel.register(provider.readCycle.selector, 0);
            this.subscriber = listener == null ? null : new Subscriber(listener);
            if (isServerConnection()) {
                setState(State.OPEN);
                setInterests(SelectionKey.OP_READ | SelectionKey.OP_WRITE, true);
                logger.info("New server socket connection. ", this);
                return;
            }
            setState(State.CONNECTING);
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

    private void setInterests(int mask, boolean set) {
        if (!sKey.isValid()) {
            interestsMask = NO_INTERESTS_MASK;
            return;
        }
        sKey.interestOps(set ? sKey.interestOps() | mask : sKey.interestOps() & (~mask));
        int flags = sKey.interestOps();
        opWrite = (flags & SelectionKey.OP_WRITE) != 0;
        interestsMask = ((flags & SelectionKey.OP_CONNECT) == 0 ? "-" : "c") + (!opWrite ? "-" : "w")
                + ((flags & SelectionKey.OP_READ) == 0 ? "-" : "r");
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

    private void sendEvent(Object event) throws InterruptedException {
        InboundEventHolder holder = new InboundEventHolder(subscriber, event);
        provider.distributor.put(holder);
    }

    @Override
    public String toString() {
        return "{C" + cId + ": " + socketChannel.socket().getLocalSocketAddress() + "=>"
                + socketChannel.socket().getRemoteSocketAddress() + ", " + state + "}";
    }

}
