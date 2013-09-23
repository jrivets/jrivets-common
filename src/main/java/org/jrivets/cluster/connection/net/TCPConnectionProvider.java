package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.jrivets.cluster.Address;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.ConnectionProvider;
import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class TCPConnectionProvider implements ConnectionProvider {

    private final Logger logger;

    private final ServerSocketChannel serverSocketChannel;

    private final Selector selector;

    private final ExecutorService executor;
    
    public TCPConnectionProvider(int listenOn, ExecutorService executor)
            throws IOException {
        this.logger = LoggerFactory.getLogger(TCPConnectionProvider.class);
        this.executor = executor;
        
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(listenOn));
        this.selector = Selector.open();
        this.serverSocketChannel.configureBlocking(false);
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                mainCycle();
            }
        });
    }

    @Override
    public Connection createClientConnection(Address address) {
        return null;
    }

    private boolean isActive() {
        return true;
    }

    private void mainCycle() {
        try {
            while (isActive()) {
                selectCycle();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void selectCycle() throws IOException {
        selector.select();
        Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
        while (keyIterator.hasNext()) {
            SelectionKey sKey = keyIterator.next();
            keyIterator.remove();
        }
    }
    
}
