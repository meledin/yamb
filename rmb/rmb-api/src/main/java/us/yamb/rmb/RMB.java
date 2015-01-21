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
	 * Instruments an object as a REST object, reading the applicable
	 * annotations and providing the appropriate callbacks.
	 * 
	 * @param restObject
	 *            The object to instrument.
	 */
	public void add(Object restObject);

	/**
	 * Creates a new Request object, which sends a message and expects a single reply.
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

    public Request get(String to);
    public Request get(Location to);

    public Request put(String to);
    public Request put(Location to);

    public Request post(String to);
    public Request post(Location to);

    public Request delete(String to);
    public Request delete(Location to);
	
    public RMB ondelete(OnDelete cb);
    public RMB ondisconnect(OnDisconnect cb);
    public RMB onget(OnGet cb);
    public RMB onhead(OnHead cb);
    public RMB onmessage(OnMessage cb);
    public RMB onpipe(OnPipe cb);
    public RMB onpost(OnPost cb);
    public RMB onput(OnPut cb);

}
