package us.yamb.mb;

import us.yamb.mb.callbacks.AsyncResult;

public interface MBMethods<MBStatus, ChannelBuilder, Send>
{
	public String id();

	/**
	 * Create a ChannelBuilder to join a Channel. This is a Publish/Subscribe
	 * abstraction. Channels can be joined, left, sent to and received from.
	 * 
	 * @return A ChannelBuilder to create the Channel.
	 */
	public ChannelBuilder channel();

	/**
	 * Creates a new Send object, to send a message on the bus.
	 * 
	 * @return
	 */
	public Send message();

	public MBStatus status();

	/**
	 * Connects the message bus.
	 * 
	 * @return A callback that will return <i>null</i> if the connection was
	 *         successfully established. If an error occurred during
	 *         establishment, however, it will return the exception given.
	 */
	public AsyncResult<Exception> connect();

	public void disconnect();

	/**
	 * Accessor for seed info that can be provided to another peer. The string
	 * is an opaque element that can be used to add a new node to the message
	 * bus. This may or may not be identical to the seed info that was provided
	 * earlier.
	 * 
	 * @return A string that can be used to seed a new node on the message bus.
	 */
	public String seedInfo();

}
