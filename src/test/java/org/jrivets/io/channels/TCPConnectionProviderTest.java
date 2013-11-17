package org.jrivets.io.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.jrivets.cluster.connection.Connection;
import org.jrivets.event.OnEvent;
import org.jrivets.io.channels.TCPChannel;
import org.jrivets.io.channels.TCPChannelProivider;

public class TCPConnectionProviderTest {

    private AtomicInteger count = new AtomicInteger();

    private AtomicInteger acceptCount = new AtomicInteger();

    private AtomicLong connectedCount = new AtomicLong();
    
    private ConnectionListener listener = new ConnectionListener();

    class AcceptedListener {
        @OnEvent
        void onAccepted(ChannelAcceptEvent ace) {
            ace.getChannel().open(listener);
            ace.getChannel().setChannelReader(new DirectChannelReader());
            acceptCount.incrementAndGet();
        }
    }

    class ConnectionListener {
        long stop;

        byte[] b = new byte[64*1024];

        private AtomicLong readCount = new AtomicLong();

        @OnEvent
        void onConnected(ChannelConnectEvent cce) throws IOException, InterruptedException {
            while (acceptCount.get() < 1) {
                Thread.yield();
            }
            TCPChannel connection = (TCPChannel) cce.getChannel();
            CachedChannelWriter w = new CachedChannelWriter(connection, 500);
            connection.setChannelWriter(w);
            Thread.sleep(500);
            long start = System.currentTimeMillis();
            long count = 4000000000L/b.length;
            long totalWrite = 0;
            for (int i = 0; i < count; i++) {
                w.write(b, true);
                totalWrite += b.length;
            }
            while (readCount.get() < totalWrite) 
                Thread.yield();
            long length = System.currentTimeMillis() - start;
            System.out.println(readCount.get() + " btes receive took " + length + "ms " + (readCount.get()/length)*1000L + " per second.");
            connection.close();
            
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
        void onRead(ReadFromChannelEvent rfce) throws IOException {
            readCount.addAndGet(rfce.getBuffer().limit());
        }

    }

    public void connection2() throws InterruptedException, IOException {
        TCPChannelProivider prov1 = new TCPChannelProivider(17018, new AcceptedListener());
        TCPChannelProivider prov = new TCPChannelProivider(-1, null);
        TCPChannel con2 = prov.createClientConnection(new InetSocketAddress(17018));
        con2.open(listener);
        while(con2.getState() != TCPChannel.State.CLOSED)
            Thread.yield();
        prov.stop();
        prov1.stop();
    }
    
    public static void main(String[] args) throws Exception {
        (new TCPConnectionProviderTest()).connection2();
    }
}
