package us.yamb.rmb.impl;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Send;

public class RMBChild extends RMBImpl
{

	private RMBImpl	parent;

	public RMBChild(RMBImpl parent, String name)
	{
		this.parent = parent;
		this.name = name;
	}

	public String id()
	{
		return parent.id() + "/" + name;
	}

	public AsyncResult<Exception> connect()
	{
		return parent.connect();
	}

	public void disconnect()
	{
		parent.disconnect();
	}

	public RMBStatus status()
    {
	    return parent.status();
    }

	@Override
    protected Send message(RMBImpl res)
    {
	    return parent.message(res);
    }

}
