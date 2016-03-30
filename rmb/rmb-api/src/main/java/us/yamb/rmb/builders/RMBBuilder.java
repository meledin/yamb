package us.yamb.rmb.builders;

import us.yamb.rmb.RMB;

/**
 * Constructor object for YAMB instances
 */
public abstract class RMBBuilder
{
    /**
     * Seed the message bus with some information on where to connect to a broker/peer. This seed information may need to be
     * broker specific.
     * 
     * @param seedPeerInfo
     *            A string containing an initial seed value to allow the message bus to connect.
     * @return self for chaining
     */
    public abstract RMBBuilder seed(String seedPeerInfo);
    
    /**
     * Request a certain <i>id</i> for this node, to allow easier identification. It is optional for the message bus to actually
     * honor the request, and the actual ID of this message bus client should be inspected once
     * 
     * @param id
     * @return
     */
    public abstract RMBBuilder id(String id);
    
    /**
     * Configures further, optional parameters. The values of these parameters depends on the implementation.
     * 
     * @param key
     *            The key to configure
     * @param value
     *            The value to set
     * @return This builder, for chaining.
     */
    public abstract RMBBuilder configure(String key, Object value);
    
    /**
     * Fetches the configuration set
     * 
     * @param key
     *            The key to fetch the value for
     * @return The value associated with <i>key</i>
     */
    public abstract Object getConfig(String key);
    
    /**
     * Builds the RMB instance.
     * 
     * @return A new RMB instance, created according to the builder specification
     */
    public abstract RMB build();
    
    /**
     * Creates a new RMB Builder
     * 
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public static RMBBuilder builder() throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
        return (RMBBuilder) Class.forName(RMBBuilder.class.getCanonicalName() + "Impl").newInstance();
    }
    
    /**
     * Add a unique handle to the request. This is a byte array passed to the upstream source, which may be used to authenticate
     * the id request.
     * 
     * @param handle
     * @return
     */
    public abstract RMBBuilder handle(byte[] handle);
}