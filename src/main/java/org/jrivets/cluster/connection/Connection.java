package org.jrivets.cluster.connection;

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
     * {@link ConnectedConnectionEvent}
     * 
     * @param connectionEventListener
     */
    void open(Object connectionEventListener);

    /**
     * Sets events deliver discipline. If the {@code serialDelivery} is
     * {@code true}, then next event cannot be delivered before handling of
     * previous one is over. It is set to true by default.
     * 
     * @param serialDelivery
     */
    void setSerial(boolean serialDelivery);

    /**
     * Sends data to the connection asynchronously.
     * <p>
     * Outbound packet is placed to internal connection send buffer. The method
     * usually returns immediately, but it can block the invocation thread for
     * some time if the send buffer is full. It never throws, and ignores empty
     * or null packages. The method is tread-safe, so multiple threads can call
     * it simultaneously for different packets.
     * 
     * @param outboundPacket
     *            - packet to be sent.
     */
    void send(OutboundPacket outboundPacket);
    
    /**
     * Outbound packet factory method.
     * 
     * @return
     */
    OutboundPacket newOutboundPacket();

    void close();

}
