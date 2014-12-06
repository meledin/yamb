package us.yamb.amb.builders;

import us.yamb.amb.AMB;

/**
 * Constructor object for YAMB instances
 */
public abstract class AMBBuilder
{
	/**
	 * Seed the message bus with some information on where to connect to a
	 * broker/peer. This seed information may need to be broker specific.
	 * 
	 * @param seedPeerInfo
	 *            A string containing an initial seed value to allow the message
	 *            bus to connect.
	 * @return self for chaining
	 */
	public abstract AMBBuilder seed(String seedPeerInfo);

	/**
	 * Request a certain <i>id</i> for this node, to allow easier
	 * identification. It is optional for the message bus to actually honor the
	 * request, and the actual ID of this message bus client should be inspected
	 * once
	 * 
	 * @param id
	 * @return
	 */
	public abstract AMBBuilder id(String id);

	public abstract AMBBuilder configure(String key, Object value);

	public abstract Object getConfig(String key);

	public abstract AMB build();

	public static AMBBuilder builder() throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		return (AMBBuilder) Class.forName(AMBBuilder.class.getCanonicalName() + "Impl").newInstance();
	}
}