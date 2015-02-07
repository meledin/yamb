package us.yamb.amb.builders;

import us.yamb.amb.AMB;
import us.yamb.amb.tmb.AMBTMB;

public class AMBBuilderImpl extends AMBBuilder
{

    private String id = null;
    private String seedPeerInfo;

	@Override
	public AMBBuilder seed(String seedPeerInfo)
	{
		this.seedPeerInfo = seedPeerInfo;
		return this;
	}

	@Override
	public AMBBuilder id(String id)
	{
		this.id = id;
        return this;
	}

	@Override
	public AMBBuilder configure(String key, Object value)
	{
		return this;
	}

	@Override
	public Object getConfig(String key)
	{
		return null;
	}

	@Override
	public AMB build()
	{
		return new AMBTMB(seedPeerInfo, id);
	}

}
