package us.yamb.amb.tmb;

import us.yamb.amb.Send;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.amb.spi.ObservableBase;

import com.ericsson.research.trap.utils.StringUtil;

public class SendImpl extends ObservableBase<AMBCallbackInterface, Send> implements Send
{

	private AMBTMB	    parent;
	private MessageImpl	m	= new MessageImpl();

	SendImpl(AMBTMB parent)
	{
		this.parent = parent;

	}

	public Send to(String id)
	{
		m.setTo(id);
		return this;
	}

	public Send from(String id)
	{
		m.setFrom(id);
		return this;
	}

	public Send data(byte[] data)
	{
		m.setPayload(data);
		return this;
	}

	public Send data(String data)
	{
		m.setPayload(StringUtil.toUtfBytes(data));
		return this;
	}

	public Send data(Object data)
	{
		// TODO Auto-generated method stub
		return this;
	}

	public Send confirmed(boolean confirmed)
	{
		return this;
	}

	public void send()
	{
		parent.client.send(m.to, m.payload);
	}

}
