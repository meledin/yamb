package us.yamb.amb.tmb;

import us.yamb.amb.Channel;
import us.yamb.amb.builders.ChannelBuilder;

public class ChannelBuilderImpl implements ChannelBuilder
{

	private String name;
	private AMBTMB parent;
	
	ChannelBuilderImpl(AMBTMB parent)
	{
		this.parent = parent;
		
	}

	public ChannelBuilder name(String name)
    {
	    this.name = name;
	    return this;
    }

	public Channel build()
    {
	    return new ChannelImpl(parent, name);
    }

}
