package org.jrivets.event;

/**
 * {@link EventChannel} implementations allow to deliver events between
 * publishers and subscribers. Common use case of the interface when a publisher
 * or sender wants to let other components (subscribers or listeners) know about
 * some event (state change, some data changes etc.).
 * <p>
 * Events are POJO classes with no special requirements.
 * <p>
 * Publisher is any object which invokes <tt>publish()</tt> method.
 * <p>
 * Subscribers are POJO classes with the requirement to define a method for a
 * specific event they listen for. Subscribers should have a method annotated by
 * {@link OnEvent} which is declared with one argument only. Type of the
 * argument defines which events the subscriber is going to receive (the
 * argument type or events with super-type of the argument type).
 * <p>
 * The general contract defined by the interface is that events CAN be delivered
 * to the events subscribers. Different subscribers can listen for different
 * events types. So, for example, a <tt>EventChannel</tt> can have two
 * subscribers which expect events <tt>A</tt> and <tt>B</tt> respectively. Any
 * event can be published to the channel, but only events of type <tt>A</tt>
 * will be delivered to first subscriber and events of type <tt>B</tt> to the
 * second one. This contract doesn't define a method how events going to be
 * delivered (synchronously, asynchronously), an order etc., leaving such
 * details to concrete implementation.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public interface EventChannel {

    /**
     * Publishes an object(event) into the channel. Depending on implementation
     * the method can block invoker by different reasons. The positive result
     * indicates the event is delivered or going to be delivered if it is not
     * delivered yet. <tt>false</tt> value indicates that the event has not be
     * delivered at least to one subscriber and it is not going to be.
     * 
     * @param e
     * @returns true if the event is successfully published (delivered, or there
     *          is a promise to deliver it), or false otherwise
     */
    boolean publish(Object e);

    /**
     * Adds a subscriber to the <tt>EventChannel</tt> for processing published
     * events. The subscriber has to contain at least one method annotated by
     * {@link OnEvent} which receives one argument. The method will throw
     * {@link IllegalArgumentException} if the subscriber doesn't have
     * appropriate method(s).
     * 
     * @param subscriber
     */
    void addSubscriber(Object subscriber);

    /**
     * Removes subscriber from delivery list. Does nothing if the provided
     * subscriber has not been subscribed before for the channel events.
     * 
     * @param subscriber
     */
    void removeSubscriber(Object subscriber);

}
