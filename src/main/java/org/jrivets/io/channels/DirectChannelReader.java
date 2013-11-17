package org.jrivets.io.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jrivets.log.Logger;
import org.jrivets.log.LoggerFactory;

public final class DirectChannelReader implements ChannelReader {
    
    private final Logger logger = LoggerFactory.getLogger(DirectChannelReader.class);
    
    private ByteBuffer recvBuf = ByteBuffer.allocateDirect(64 * 1024);

    @Override
    public void onReadReady(TCPChannel channel) throws IOException {
        int rr = 0;
        try {
            while (true) {
                int count = 0;
                do {
                    count = recvBuf.position();
                    rr = channel.socketChannel.read(recvBuf);
                    count = recvBuf.position() - count;
                } while (rr >= 0 && count > 0);

                boolean over = recvBuf.hasRemaining();
                if (recvBuf.position() > 0) {
                    recvBuf.flip();
                    channel.subscriber.notifySubscriberSilently(new ReadFromChannelEvent(channel, recvBuf));
                }
                recvBuf.clear();
                if (over) {
                    return;
                }
            }
        } catch (Exception ex) {
            logger.debug("Exception while reading package from ", channel, ", will close the connection ", ex);
            rr = -2;
        } finally {
            if (rr < 0) {
                channel.closeChannel();
            }
        }

    }

}
