package us.yamb.impl;

import us.yamb.amb.AMBStatus;
import us.yamb.amb.Send;
import us.yamb.amb.builders.ChannelBuilder;
import us.yamb.amb.builders.PipeBuilder;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request;

public class RMBImpl implements RMB
{

	public String id()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB rest()
	{
		return this;
	}

	public ChannelBuilder channel(String channelId)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send send()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Request request()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public PipeBuilder pipe()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public AMBStatus status()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void connect()
	{
		// TODO Auto-generated method stub

	}

	public void disconnect()
	{
		// TODO Auto-generated method stub

	}

	public void setCallback(AMBCallbackInterface callback)
	{
		// TODO Auto-generated method stub

	}

	public void unsetCallback(Class<? extends AMBCallbackInterface> callbackType)
	{
		// TODO Auto-generated method stub

	}

	public String seedInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB create()
	{
		return null;
	}

	public RMB create(String id)
	{
		return null;
	}

	public ChannelBuilder channel()
    {
	    // TODO Auto-generated method stub
	    return null;
    }

	public void setCallback(Class<? extends AMBCallbackInterface> callbackType, Object object, String function, Class<?>... fnArgs) throws NoSuchMethodException
    {
	    // TODO Auto-generated method stub
	    
    }

}
