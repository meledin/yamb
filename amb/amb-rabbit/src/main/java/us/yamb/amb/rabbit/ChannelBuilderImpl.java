package us.yamb.amb.rabbit;

import us.yamb.amb.Channel;
import us.yamb.amb.builders.ChannelBuilder;

public class ChannelBuilderImpl implements ChannelBuilder
{

	private String name;
	private AMBRabbit parent;
	
	ChannelBuilderImpl(AMBRabbit parent)
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
