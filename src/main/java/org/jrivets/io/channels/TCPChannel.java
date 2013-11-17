package org.jrivets.io.channels;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.jrivets.event.Subscriber;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class TCPChannel implements Channel {

    private final static String NO_INTERESTS_MASK = "---";

    private final Logger logger;

    private static int channelNumber;

    private final int cId = ++channelNumber;

    final SocketChannel socketChannel;

    private final SocketAddress remoteAddress;

    volatile SelectionKey sKey;
    
    private ChannelWriter writer;
    
    private ChannelReader reader;

    private final TCPChannelProivider provider;

    Subscriber subscriber;

    private String interestsMask = NO_INTERESTS_MASK;

    private volatile State state = State.OPENING;

    private long stateSince;

    private Runnable setOpWriteCmd = new Runnable() {
        @Override
        public void run() {
            setInterests(SelectionKey.OP_WRITE, true);
        }
    };
    
    private Runnable resetOpWriteCmd = new Runnable() {
        @Override
        public void run() {
            setInterests(SelectionKey.OP_WRITE, false);
        }
    };

    private Runnable closeCmd = new Runnable() {
        @Override
        public void run() {
            closeChannel();
        }
    };

    TCPChannel(SocketChannel socketChannel, SocketAddress remoteAddress, TCPChannelProivider provider) {
        this.socketChannel = socketChannel;
        this.sKey = sKey;
        this.provider = provider;
        this.remoteAddress = remoteAddress; // null for server socket
        this.logger = LoggerFactory.getLogger(Channel.class, "(C" + cId + " %1$s): %2$s", new Object() {
            public String toString() {
                return state.name() + " " + interestsMask;
            }
        });
        setState(State.OPENING);
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

    @Override
    public void setChannelWriter(final ChannelWriter writer) {
        provider.readCycle.onCommand(new Runnable() {
            @Override
            public void run() {
                TCPChannel.this.writer = writer;
                setInterests(SelectionKey.OP_WRITE, writer != null);
                TCPChannel.this.logger.info("Registering new writer ", writer);
            }
        });
    }

    @Override
    public void setChannelReader(final ChannelReader reader) {
        provider.readCycle.onCommand(new Runnable() {
            @Override
            public void run() {
                TCPChannel.this.reader = reader;
                setInterests(SelectionKey.OP_READ, reader != null);
                TCPChannel.this.logger.info("Registering new reader ", reader);
            }
        });
    }

    @Override
    public State getState() {
        return state;
    }

    void onConnectReady() throws IOException, InterruptedException {
        socketChannel.finishConnect();
        setState(State.OPEN);
        setInterests(SelectionKey.OP_READ | SelectionKey.OP_WRITE, true);
        sendEvent(new ChannelConnectEvent(this));
    }

    void onReadReady() throws IOException {
        if (reader == null) {
            setInterests(SelectionKey.OP_READ, false);
            return;
        }
        reader.onReadReady(this);
    }

    void onWriteReady() throws IOException {
        if (writer == null || !writer.onWriteReady(this)) {
            setInterests(SelectionKey.OP_WRITE, false);
            return;
        }
    }

    void closeChannel() {
        if (state == State.CLOSED) {
            return;
        }
        provider.onCloseChannel(this);
        try {
            if (sKey != null) {
                sKey.cancel();
            }
            socketChannel.close();
        } catch (Exception e) {
            logger.debug("When closing the channel ", e);
        } finally {
            setState(State.CLOSED);
        }
    }

    boolean isServerConnection() {
        return remoteAddress == null;
    }

    void enableWriting(boolean writingEnabled) {
        provider.readCycle.onCommand(writingEnabled ? setOpWriteCmd : resetOpWriteCmd);
    }
    
    long getStateSince() {
        return stateSince;
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
            closeChannel();
        }
    }

    private void setInterests(int mask, boolean set) {
        if (sKey == null || !sKey.isValid() || state == State.CLOSED) {
            interestsMask = NO_INTERESTS_MASK;
            return;
        }
        sKey.interestOps(set ? sKey.interestOps() | mask : sKey.interestOps() & (~mask));
        int flags = sKey.interestOps();
        boolean writeEnabled = (flags & SelectionKey.OP_WRITE) != 0;
        if (writer != null) {
            writer.onWriteEnabled(writeEnabled);
        }
        interestsMask = ((flags & SelectionKey.OP_CONNECT) == 0 ? "-" : "c") + (!writeEnabled ? "-" : "w")
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

    private void sendEvent(Object event) {
        provider.notifySubscriber(subscriber, event);
    }

    @Override
    public String toString() {
        return "{C" + cId + ": " + socketChannel.socket().getLocalSocketAddress() + "=>"
                + socketChannel.socket().getRemoteSocketAddress() + ", " + state + "}";
    }

}
