package us.yamb.rmb.impl;

import us.yamb.rmb.RMB;
import us.yamb.rmb.Request;
import us.yamb.rmb.Send;
import us.yamb.rmb.builders.ChannelBuilder;
import us.yamb.rmb.builders.PipeBuilder;
import us.yamb.rmb.callbacks.RMBCallbackInterface;
import us.yamb.rmb.impl.builders.SendImpl;

public abstract class RMBImpl implements RMB
{
	
	protected String name;
	
	public ChannelBuilder channel()
	{
		// TODO Auto-generated method stub
		return null;
	}

	protected abstract Send message(RMBImpl res);
	
	public Send message()
	{
		return message(this);
	}

	public String seedInfo()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB setCallback(RMBCallbackInterface callback)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB setCallback(Class<? extends RMBCallbackInterface> callbackType, Object object, String function) throws NoSuchMethodException
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB unsetCallback(Class<? extends RMBCallbackInterface> callbackType)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB create()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public RMB create(String id)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void add(Object restObject)
	{
		// TODO Auto-generated method stub

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

}
