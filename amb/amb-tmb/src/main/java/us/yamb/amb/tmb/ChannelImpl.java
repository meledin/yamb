package us.yamb.amb.tmb;

import com.ericsson.research.trap.utils.StringUtil;

import us.yamb.amb.Channel;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.amb.callbacks.AsyncResult;
import us.yamb.amb.callbacks.OnChannel;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.amb.spi.ObservableBase;

public class ChannelImpl extends ObservableBase<AMBCallbackInterface, Channel> implements Channel
{

	protected OnChannel	 onchannel;
	private final String	name;
	private final AMBTMB	parent;

	public ChannelImpl(AMBTMB parent, String name)
	{
		this.parent = parent;
		this.name = name;
	}

	public String name()
	{
		return name;
	}

	public Channel send(byte[] data)
	{
		parent.client.publish(name, data);
		return this;
	}

	public Channel send(String data)
	{
		return send(StringUtil.toUtfBytes(data));
	}

	public Channel send(Object json)
	{
		// TODO Auto-generated method stub
		return this;
	}

	public AsyncResult<Boolean> join()
	{
		AsyncResultImpl<Boolean> res = new AsyncResultImpl<Boolean>();

		parent.client.subscribe(name);
		parent.subs.put(name, this);

		res.callback(true);

		return res;
	}

	public void receive(String from, byte[] payload)
	{
		MessageImpl m = new MessageImpl();
		m.setFrom(from);
		m.setTo(name);
		m.setPayload(payload);

		if (onchannel != null)
			onchannel.onchannel(parent, this, m);
	}

	public void leave()
	{
		parent.client.unsubscribe(name);
		parent.subs.remove(name, this);
	}

}