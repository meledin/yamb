package us.yamb.amb.builders;

import java.net.URI;

import us.yamb.amb.AMB;
import us.yamb.amb.tmb.AMBTMB;

public class AMBBuilderImpl extends AMBBuilder
{

	String host;
	int port;
    private String id = null;

	@Override
	public AMBBuilder seed(String seedPeerInfo)
	{
		URI wsuri = URI.create(seedPeerInfo);
		host = wsuri.getHost();
		port = wsuri.getPort();
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
		if ("host".equals(key))
			host = value.toString();
		
		if ("port".equals(key))
			port = Integer.parseInt(value.toString());
			
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
		return new AMBTMB(host, port, id);
	}

}
