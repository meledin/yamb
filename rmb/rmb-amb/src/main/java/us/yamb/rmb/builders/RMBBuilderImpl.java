package us.yamb.rmb.builders;

import us.yamb.amb.builders.AMBBuilder;
import us.yamb.rmb.RMB;
import us.yamb.rmb.impl.RMBRootImpl;

public class RMBBuilderImpl extends RMBBuilder
{
	
	private AMBBuilder builder;

	public RMBBuilderImpl() throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
		this.builder = AMBBuilder.builder();
    }

	@Override
    public RMBBuilder seed(String seedPeerInfo)
    {
		builder.seed(seedPeerInfo);
	    return this;
    }

    @Override
    public RMBBuilder id(String id)
    {
        builder.id(id);
        return this;
    }

    @Override
    public RMBBuilder handle(byte[] handle)
    {
        builder.handle(handle);
        return this;
    }

	@Override
    public RMBBuilder configure(String key, Object value)
    {
		builder.configure(key, value);
	    return this;
    }

	@Override
    public Object getConfig(String key)
    {
	    return builder.getConfig(key);
    }

	@Override
    public RMB build()
    {
	    return new RMBRootImpl(builder.build());
    }

}
