package us.yamb.rmb;

import us.yamb.mb.MBMethods;
import us.yamb.mb.Observable;
import us.yamb.rmb.builders.ChannelBuilder;
import us.yamb.rmb.builders.PipeBuilder;
import us.yamb.rmb.callbacks.OnDelete;
import us.yamb.rmb.callbacks.OnDisconnect;
import us.yamb.rmb.callbacks.OnGet;
import us.yamb.rmb.callbacks.OnHead;
import us.yamb.rmb.callbacks.OnMessage;
import us.yamb.rmb.callbacks.OnPipe;
import us.yamb.rmb.callbacks.OnPost;
import us.yamb.rmb.callbacks.OnPut;
import us.yamb.rmb.callbacks.RMBCallbackInterface;

/**
 * REST Message Bus extends the message bus functionality to apply RESTful
 * semantics.
 * 
 * @author Vladimir Katardjiev
 *
 */
public interface RMB extends MBMethods<RMBStatus, ChannelBuilder, Send>, Observable<RMBCallbackInterface, RMB>
{

	/**
	 * Create a new instance as a subresource of the current resource. The new
	 * instance will have a random ID. A sample returned URI might be (assuming
	 * this RMB instance is /foo)
	 * <p>
	 * 
	 * <pre>
	 * /foo/1a2b4c
	 * </pre>
	 * 
	 * @return A new instance with a random ID.
	 */
	public RMB create();

	/**
	 * Create a new instance as a subresource of the current resource. The new
	 * instance will have the given ID. A sample returned URI might be (assuming
	 * this RMB instance is /foo and the supplied argument is "bar")
	 * <p>
	 * 
	 * <pre>
	 * /foo/bar
	 * </pre>
	 * 
	 * @param id
	 *            The identifier to use
	 * @return
	 */
	public RMB create(String id);
	
	/**
	 * Creates a resource whose ID is a regular expression, and will not be split on slashes.
	 * @param id
	 * @param regexp
	 * @return
	 */
	public RMB create(String id, boolean regexp);

	/**
	 * Instruments an object as a REST object, reading the applicable
	 * annotations and providing the appropriate callbacks.
	 * 
	 * @param restObject
	 *            The object to instrument.
	 */
	public void add(Object restObject);

	/**
	 * Creates a new Request object, which sends a message and expects a single
	 * reply.
	 * 
	 * @return
	 */
	public Request request();

	/**
	 * Create a {@link PipeBuilder} to make a {@link Pipe}. Pipes are
	 * high-performance conduits to send identical messages repeatedly. When
	 * sending messages over a pipe, the implementation may perform
	 * optimisations knowing that there will be a series of messages going over
	 * the same path. Inversely, for one-time (or sporadic) messages, making a
	 * pipe may require more time than is necessary.
	 * 
	 * @return
	 */
	public PipeBuilder pipe();

	/**
	 * Create a GET request to the given location. The parameter will be
	 * automatically parsed as a {@link Location} object.
	 * 
	 * @param to
	 *            The location to send a GET to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request get(String to);

	/**
	 * Create a GET request to the given location.
	 * 
	 * @param to
	 *            The location to send a GET to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request get(Location to);

	/**
	 * Create a PUT request to the given location. The parameter will be
	 * automatically parsed as a {@link Location} object.
	 * 
	 * @param to
	 *            The location to send a PUT to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request put(String to);

	/**
	 * Create a PUT request to the given location.
	 * 
	 * @param to
	 *            The location to send a PUT to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request put(Location to);

	/**
	 * Create a POST request to the given location. The parameter will be
	 * automatically parsed as a {@link Location} object.
	 * 
	 * @param to
	 *            The location to send a POST to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request post(String to);

	/**
	 * Create a POST request to the given location.
	 * 
	 * @param to
	 *            The location to send a POST to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request post(Location to);

	/**
	 * Create a DELETE request to the given location. The parameter will be
	 * automatically parsed as a {@link Location} object.
	 * 
	 * @param to
	 *            The location to send a DELETE to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request delete(String to);

	/**
	 * Create a DELETE request to the given location.
	 * 
	 * @param to
	 *            The location to send a DELETE to
	 * @return A {@link Request} builder, to construct a request.
	 */
	public Request delete(Location to);

	/**
	 * Sets a handler that will be called when the connection to the message
	 * broker is broken. This can be used for cleanup/error handling purposes.
	 * 
	 * @param cb
	 * @return
	 */
	public RMB ondisconnect(OnDisconnect cb);

	/**
	 * Sets a handler to be called when a DELETE message is received by this
	 * node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB ondelete(OnDelete cb);

	/**
	 * Sets a handler to be called when a GET message is received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onget(OnGet cb);

	/**
	 * Sets a handler to be called when a HEAD message is received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onhead(OnHead cb);

	/**
	 * Sets a handler to be called when a message with an unknown method, or
	 * with a method that was not handled by a more specific handler, is
	 * received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onmessage(OnMessage cb);

	/**
	 * Sets a handler to be called when a PIPE request is received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onpipe(OnPipe cb);

	/**
	 * Sets a handler to be called when a POST message is received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onpost(OnPost cb);

	/**
	 * Sets a handler to be called when a PUT message is received by this node.
	 * 
	 * @param cb
	 *            The handler to call
	 * @return This object, for chaining
	 */
	public RMB onput(OnPut cb);

	/**
	 * Remove this RMB instance from the tree. This will disconnect any children
	 * of this instance, if any. This method is a no-op if called on the root
	 * RMB instance.
	 */
	public void remove();

}
