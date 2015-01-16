package us.yamb.rmb.impl.builders;

import us.yamb.rmb.Path;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.RMBImpl;

public class SendImpl implements Send
{

	private RMBImpl parent;
	private String method;

	public SendImpl(RMBImpl parent, us.yamb.amb.Send aSend)
    {
		this.parent = parent;
    }

	public Send method(String method)
	{
		this.method = method;
		return this;
	}

	public Send to(Path to)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send to(String id)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send data(byte[] data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send data(String data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send data(Object data)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Send confirmed(boolean confirmed)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
