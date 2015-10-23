package us.yamb.rmb;

import java.io.IOException;

import us.yamb.rmb.builders.RestMessageBuilder;

/**
 * Use a Send builder to send a message asynchronously. The Send builder will
 * not create a listener for any answers, and the sending identity in
 * {@link Message#from()} will point to the resource that created this builder.
 * 
 * @author Vladimir Katardjiev
 *
 */
public interface Send extends RestMessageBuilder<Send>
{
	/**
	 * Sends the message to its intended destination. This is an asynchronous
	 * method.
	 * 
	 * @throws IOException
	 *             If a direct error occurs in serialisation.
	 */
	public void send() throws IOException;
}
