package us.yamb.rmb.impl.builders;

import java.io.IOException;

import us.yamb.rmb.Location;
import us.yamb.rmb.RMB;
import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.RMBMessage;

public class SendImpl<T> extends RMBMessage<T>
{

	protected RMBImpl	       parent;
	protected us.yamb.amb.Send	msg;

	protected SendImpl()
	{

	}

	public SendImpl(RMBImpl parent, us.yamb.amb.Send aSend)
	{
		this.parent = parent;
		this.msg = aSend;
	}

	public void send() throws IOException
	{
		send(parent);
	}

	public void send(RMB resp) throws IOException
	{
		if (resp != null)
			from(resp.id());
		msg.confirmed(confirmed());
		msg.data(serialize());
		msg.to(to().getPart(Location.ROOT_ID));
		msg.send(); 
	}
}
