package org.jrivets.io.channels;

import java.io.IOException;

interface ChannelWriter {

    /**
     * Channel writing readiness notification. Implementation may write to the
     * socket and as a result provides whether writing is enabled or not
     * 
     * @param socketChannel
     * @return
     */
    boolean onWriteReady(TCPChannel channel) throws IOException;
    
    /**
     * Channel invokes the method every time when write operation enabled or disabled.
     * @param writeEnabled
     */
    void onWriteEnabled(boolean writeEnabled);

}
