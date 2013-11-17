package org.jrivets.io.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jrivets.event.Subscriber;
import org.jrivets.io.channels.Channel.State;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

final class TCPChannelProivider {

    private final Logger logger;

    private final Map<SelectableChannel, TCPChannel> channelMap = new HashMap<SelectableChannel, TCPChannel>();

    private final AcceptSelectCycle acceptCycle;

    final ReadSelectCycle readCycle;

    private final SubscriberNotifier subscriberNotifier;

    private final Subscriber acceptSubscriber;

    class ReadSelectCycle extends SelectorCycle {
        
        private long nextRemoveCheck;

        protected ReadSelectCycle() throws IOException {
            super(LoggerFactory.getLogger(ReadSelectCycle.class));
        }

        @Override
        protected void onSelect(SelectionKey sKey) {
            TCPChannel connection = null;
            try {
                connection = channelMap.get(sKey.channel());

                if (sKey.isValid() && sKey.isConnectable()) {
                    connection.onConnectReady();
                }

                if (sKey.isValid() && sKey.isReadable()) {
                    connection.onReadReady();
                }

                if (sKey.isValid() && sKey.isWritable()) {
                    connection.onWriteReady();
                }
            } catch (Exception e) {
                logger.error("Exception while notifying connection, close it ", e);
                if (connection != null) {
                    connection.closeChannel();
                }
            }
        }

        TCPChannel createNewChannel(SocketChannel channel, final SocketAddress remoteAddress) {
            final TCPChannel newChannel = new TCPChannel(channel, remoteAddress,
                    TCPChannelProivider.this);
            onCommand(new Runnable() {
                @Override
                public void run() {
                    channelMap.put(newChannel.socketChannel, newChannel);
                    if (newChannel.isServerConnection()) {
                        notifySubscriber(acceptSubscriber, new ChannelAcceptEvent(newChannel));
                    }
                }
            });
            return newChannel;
        }
        
        @Override
        protected void miscCycle() {
            long now = System.currentTimeMillis();
            if (nextRemoveCheck > now) {
                return;
            }
            nextRemoveCheck = now + TimeUnit.MINUTES.toMillis(1);
            ArrayList<TCPChannel> channels = new ArrayList<TCPChannel>(channelMap.values());
            for (TCPChannel channel : channels) {
                State state = channel.getState();
                if (state == State.CONNECTING && channel.getStateSince() < (now - TimeUnit.MINUTES.toMillis(2))) {
                    logger.warn("Close channel by connection timeout ", channel);
                    channel.closeChannel();
                }
                if (state == State.OPENING && channel.getStateSince() < (now - TimeUnit.MINUTES.toMillis(10))) {
                    logger.warn("Close channel by opening timeout ", channel);
                    channel.closeChannel();
                }
            }
        }
        
        @Override
        protected void finalized() {
            logger.info("Closing all channels");
            ArrayList<TCPChannel> channels = new ArrayList<TCPChannel>(channelMap.values());
            for (TCPChannel channel : channels) {
                channel.closeChannel();
            }
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
                    readCycle.createNewChannel(serverSocketChannel.accept(), null);
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

    TCPChannelProivider(int listenOn, Object acceptListener) throws IOException {
        this(listenOn, new ExecutorSubscriberNotifier(Executors.newCachedThreadPool()), acceptListener);
    }
    
    TCPChannelProivider(int listenOn, SubscriberNotifier subscriberNotifier, Object acceptListener) throws IOException {
        if (subscriberNotifier == null) {
            throw new NullPointerException();
        }
        this.subscriberNotifier = subscriberNotifier;
        if (listenOn > 0) {
            this.acceptSubscriber = new Subscriber(acceptListener);
            this.acceptCycle = new AcceptSelectCycle(listenOn);
            this.acceptCycle.start();
            this.logger = LoggerFactory.getLogger(TCPChannelProivider.class, "("
                    + acceptCycle.serverSocketChannel.socket().getLocalSocketAddress() + ") %2$s", null);
        } else {
            this.acceptSubscriber = null;
            this.acceptCycle = null;
            this.logger = LoggerFactory.getLogger(TCPChannelProivider.class, "(clients only) %2$s", null);
        }
        this.readCycle = new ReadSelectCycle();
        this.readCycle.start();
    }

    public void stop() {
        if (acceptCycle != null) {
            acceptCycle.stop();
        }
        readCycle.stop();
    }
    
    TCPChannel createClientConnection(SocketAddress remoteAddress) {
        try {
            return readCycle.createNewChannel(SocketChannel.open(), remoteAddress);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    void onCloseChannel(TCPChannel channel) {
        channelMap.remove(channel.socketChannel);
    }

    void notifySubscriber(final Subscriber subscriber, final Object event) {
        if (subscriber == null) {
            logger.debug("Cannot notify subscirber=null by event=", event, new Exception());
            return;
        }
        if (event == null) {
            logger.debug("Cannot notify subscirber=", subscriber, " by event=null", new Exception());
            return;
        }
        
        subscriberNotifier.notifySubscriber(subscriber, event);
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{server=").append(acceptCycle).append(", rwCycle=").append(readCycle)
                .append(", sNotifier=").append(subscriberNotifier).append(", connectors=").append(channelMap.size()).append("}").toString();
    }

}
