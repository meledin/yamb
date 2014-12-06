package us.yamb.rmb;

import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.mb.Observable;

/**
 * A pipe allows for sending and receiving sequential messages in a direct,
 * point to point approach. Unlike a channel, a pipe is between two nodes on the
 * message bus, and is used for a shorthand instead of building identical Send
 * messages.
 * <p>
 * Implementations may optimise a Pipe under the hood, knowing that a
 * significant number of messages will be sent with the same parameters. Thus,
 * if it is expected that many messages will be sent with the same parameters, a
 * Pipe should be built.
 */
public interface Pipe extends Observable<AMBCallbackInterface, Pipe>
{
	public Pipe send(byte[] data);

	public Pipe send(String data);

	public Pipe send(Object json);

	public Pipe close();
}
