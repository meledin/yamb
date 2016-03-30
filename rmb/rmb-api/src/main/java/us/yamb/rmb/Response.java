package us.yamb.rmb;

import java.io.IOException;

import us.yamb.rmb.builders.RestMessageBuilder;

/**
 * The Response interface can be used to instantiate a response quickly and
 * easily. Responses are much like any other builder.
 * <p>
 * Response builders are special because, unlike other builders, they can send
 * the message in three different (and unique) ways:
 * <ul>
 * <li>The first is to use {@link #send(RMB)} to send it using a specified RMB
 * instance. The message will be sent from that instance.
 * <li>The second is to use {@link #throwException()} to throw it as an
 * exception. The calling RMB instance will catch, and send as a reply
 * <li>The final is, when called from an object annotated using the REST
 * annotations and instantiated using {@link RMB#add(Object)}, whereby the reply
 * can simply be given as a return value.
 * 
 * @author Vladimir Katardjiev
 *
 */
public interface Response extends RestMessageBuilder<Response>
{

	/**
	 * Throw a ResponseException to immediately return a Response from RMB.
	 * 
	 * @author Vladimir Katardjiev
	 *
	 */
	public abstract class ResponseException extends RuntimeException
	{
		private static final long	serialVersionUID	= 1L;

		/**
		 * RMB will call this method to get the response to send. This method
		 * MAY NOT return null.
		 * 
		 * @return The response to send.
		 */
		public abstract Response response();
	}

	public interface Status
	{
		public static final int	OK		     = 200;
		public static final int	CLIENT_ERROR	= 400;
		public static final int	SERVER_ERROR	= 500;
	}

	public Response to(Message request);

	public Response status(int status);

	public void send(RMB rmb) throws IOException;

	/**
	 * Throws this response as an exception. RMB should catch it and send it as
	 * a response to the other party.
	 * 
	 * @throws ResponseException
	 */
	public void throwException() throws ResponseException;

	/**
	 * Creates a Response with an OK status code.
	 * 
	 * @return A Response builder ready to rock!
	 */
	public static Response ok()
	{
		return create().status(Status.OK);
	}

	/**
	 * Creates a response addressed to <i>request.from()</i>.
	 * 
	 * @param request
	 *            The message to reply to
	 * @return A Response builder ready to rock!
	 */
	public static Response create(Message request)
	{
		return create().status(Status.OK).to(request);
	}

	/**
	 * Creates an empty response.
	 * 
	 * @return A Response builder ready to rock!
	 */
	public static Response create()
	{

		try
		{
			return (Response) Class.forName(Response.class.getCanonicalName() + "Impl").newInstance();
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

    void throwException(Object probableCause) throws ResponseException;
}
