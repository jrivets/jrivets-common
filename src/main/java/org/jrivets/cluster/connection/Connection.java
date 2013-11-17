package org.jrivets.cluster.connection;

import org.jrivets.io.channels.ChannelConnectEvent;

/**
 * Represents a connection between Cluster Node and a Peer (remote Node in
 * relation to the connection owner)
 * 
 * @author Dmitry Spasibenko (dmitry.spasibenko@mulesoft.com)
 * 
 */
public interface Connection {

    /**
     * Enables data flow for the connection and assigns the connection listener.
     * The method can be called once per connection after its creation.
     * <p>
     * For client connection the call initiates connecting procedure to server,
     * in case of connected successfully will send
     * {@link ChannelConnectEvent}
     * 
     * @param connectionEventListener
     */
    void open(Object connectionEventListener);

    /**
     * Writes data to channel. Can block invocation thread for undefined time in
     * case of {@link blocking} parameter is true.
     * 
     * @param data
     * @param blocking
     * @return whether the operation was succeeded and data will be or have been
     *         actually sent through the channel
     */
    boolean write(byte[] data, boolean blocking);

    void close();

}
