package org.jrivets.cluster.handshake;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jrivets.cluster.TimerTask;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.ConnectionEndpoint;
import org.jrivets.cluster.connection.OutboundPacket;
import org.jrivets.cluster.handshake.HandshakeChecker.CheckResult;
import org.jrivets.event.OnEvent;
import org.jrivets.io.channels.ChannelConnectEvent;
import org.jrivets.io.channels.ReadFromChannelEvent;
import org.jrivets.util.container.Pair;

public final class HandshakeConnection { 
    
    private final Connection connection;
    
    private final ScheduledExecutorService scheduler;
    
    private final HandshakeChecker hsChecker;
    
    private final Lock lock = new ReentrantLock();
    
    private ConnectionEndpoint endpoint;
    
    private volatile State state = State.NEW;
    
    private TimerTask timerTask;
    
    enum State {NEW, CONNECTING, CONNECTED, END};
    
    enum CloseReason {CONNECTION_TIMEOUT, HS_FAIL, EXTERNAL_EVENT, INTERNAL_ERROR};
    
    HandshakeConnection(Connection connection, HandshakeChecker hsChecker, boolean isServer, ScheduledExecutorService scheduler) {
        this.connection = connection;
        this.hsChecker = hsChecker;
        this.scheduler = scheduler;
        if (isServer) {
            setStateConnecting();
        }
        connection.open(this);
    }

    public void close() {
        closeWithReason(CloseReason.EXTERNAL_EVENT);
    }
    
    /**
     * Client Connection notification about just established connection
     * 
     * @param connectedEvent
     */
    @OnEvent
    void onConnect(ChannelConnectEvent connectedEvent) {
        if (setStateConnecting()) {
            // we have got connected, so allow client to initiate communication.
            onHandshakePacket(null);
            return;
        } 
        if (closeWithReason(CloseReason.INTERNAL_ERROR)) {
            // Put something to log with lot of info here - ERROR
        }
    }
        
    @OnEvent 
    void onRead(ReadFromChannelEvent readEvent) {
        if (state == State.CONNECTED) {
            endpoint.onRead(readEvent);
            return;
        }
        if (state != State.CONNECTING) {
            //TODO: print error with as much info as possible.
            return;
        }
        onHandshakePacket(readEvent);
    }
    
    private void onHandshakePacket(ReadFromChannelEvent readEvent) {
        Pair<CheckResult, Object> hsResult = hsChecker.onHandshakePacket(readEvent);
        switch (hsResult.getFirst()) {
        case NEED_MORE:
            if (timerTask != null) timerTask.reschedule();
            //TODO: old one - connection.send((OutboundPacket) hsResult.getSecond());
            break;
        case FAIL:
            closeWithReason(CloseReason.HS_FAIL);
            break;
        case OK:
        case OK_SERIAL:
            if (setState(State.CONNECTING, State.CONNECTED)) {
                endpoint = (ConnectionEndpoint) hsResult.getSecond();
                hsChecker.onConnected(this);
                //TODO: refactor: connection.setSerial(hsResult.getFirst() == CheckResult.OK_SERIAL); // last thing what we do here
                break;
            } // yes, we go ahead here
        default: 
            closeWithReason(CloseReason.INTERNAL_ERROR);
        }
    }

    private Runnable getCloseTask() {
        return new Runnable() {
            @Override
            public void run() {
                closeWithReason(CloseReason.CONNECTION_TIMEOUT);
            }
        };
    }
     
    private boolean closeWithReason(CloseReason reason) {
        if (!setState(State.END)) {
            return false;
        }
        connection.close();
        hsChecker.onDisconnected(this);
        return true;
    }
    
    private boolean setStateConnecting() {
        return setState(State.NEW, State.CONNECTING, getCloseTask(), TimeUnit.SECONDS.toMillis(30));
    }
    
    private boolean setState(State newState) {
        return setState(null, newState, null, 0L);
    }
    
    private boolean setState(State expectedState, State newState) {
        return setState(expectedState, newState, null, 0L);
    }
    
    private boolean setState(State expectedState, State newState, Runnable r, long timeoutMillis) {
        lock.lock();
        try {
            if (expectedState != null && expectedState != state) {
                //TODO: put some bad words into log, because it never should happen.
                return false;
            }
            if (state == newState) {
                return false;
            }
            state = newState;
            scheduleByTimeout(r, timeoutMillis);
        } finally {
            lock.unlock();
        }
        return true;
    }
    
    private void scheduleByTimeout(Runnable r, long timeoutMillis) {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = r == null ? null : new TimerTask(scheduler, r, timeoutMillis);
    }
}