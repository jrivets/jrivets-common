package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jrivets.cluster.connection.AcceptedConnectionEvent;
import org.jrivets.cluster.connection.ConnectedConnectionEvent;
import org.jrivets.cluster.connection.Connection;
import org.jrivets.cluster.connection.ReadFromConnectionEvent;
import org.jrivets.event.OnEvent;

public class TCPConnectionProviderTest {

    private AtomicInteger count = new AtomicInteger();

    private AtomicInteger acceptCount = new AtomicInteger();

    private AtomicLong connectedCount = new AtomicLong();
    
    private ConnectionListener listener = new ConnectionListener();

    class AcceptedListener {
        @OnEvent
        void onAccepted(AcceptedConnectionEvent ace) {
            ace.getConnection().open(listener);
            acceptCount.incrementAndGet();
        }
    }

    class ConnectionListener {
        long stop;

        byte[] b = new byte[64*1024];

        private AtomicLong readCount = new AtomicLong();

        @OnEvent
        void onConnected(ConnectedConnectionEvent cce) throws IOException, InterruptedException {
            while (acceptCount.get() < 1) {
                Thread.yield();
            }
            TCPConnection2 connection = (TCPConnection2) cce.getConnection();
            Thread.sleep(500);
            long start = System.currentTimeMillis();
            long count = 4000000000L/b.length;
            long totalWrite = 0;
            for (int i = 0; i < count; i++) {
                connection.write(b, true);
                totalWrite += b.length;
            }
            while (readCount.get() < totalWrite) 
                Thread.yield();
            long length = System.currentTimeMillis() - start;
            System.out.println(readCount.get() + " btes receive took " + length + "ms " + (readCount.get()/length)*1000L + " per second.");
            connection.closeConnection();
            
//            long stop = System.currentTimeMillis() + 2000L;
//            while (stop > System.currentTimeMillis()) {
//                connection.write(b, true);
//                connectedCount.incrementAndGet();
//            }
//            for (int i = 0; i < 1; i++)
//            System.out.println("Client: " + (connectedCount.get()*b.length/2) + " bytes sent " + connection.recvCount/2 + " btes received");
//            cce.getConnection().close();
        }

        @OnEvent
        void onRead(ReadFromConnectionEvent rfce) throws IOException {
            readCount.addAndGet(rfce.getBuffer().limit());
        }

    }

    public void connection2() throws InterruptedException, IOException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        InboundEventsDistributor distributor = new InboundEventsDistributor(executor, "", 1, 10, 100, 1);
        TCPConnectionProivider2 prov = new TCPConnectionProivider2(17018, distributor, new AcceptedListener());
        prov = new TCPConnectionProivider2(-1, distributor, null);
        TCPConnection2 con2 = prov.createClientConnection(new InetSocketAddress(17018));
        con2.open(listener);
        while(true)
            Thread.yield();
    }
    
    public static void main(String[] args) throws Exception {
        (new TCPConnectionProviderTest()).connection2();
    }
}
