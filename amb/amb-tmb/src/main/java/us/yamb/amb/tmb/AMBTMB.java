package us.yamb.amb.tmb;

import java.util.concurrent.ConcurrentSkipListMap;

import us.yamb.amb.AMB;
import us.yamb.amb.AMBStatus;
import us.yamb.amb.Send;
import us.yamb.amb.builders.ChannelBuilder;
import us.yamb.amb.spi.AMBase;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.tmb.Client;
import us.yamb.tmb.Client.Handler;

import com.ericsson.research.trap.utils.Callback.SingleArgumentCallback;

public class AMBTMB extends AMBase implements AMB, Handler
{

	Client	                                   client;

	ConcurrentSkipListMap<String, ChannelImpl>	subs	= new ConcurrentSkipListMap<String, ChannelImpl>();
    private String seed;

	public AMBTMB(String seed, String name)
	{
		this.seed = seed;
		
		if (name != null)
		client = new Client(name);
		else
		    client = new Client();
		client.setHandler(this);
	}

	public String id()
	{
		return client.getName();
	}

	public ChannelBuilder channel()
	{
		return new ChannelBuilderImpl(this);
	}

	public Send message()
	{
		return new SendImpl(this);
	}

	public AsyncResult<Exception> connect()
	{
		this.status = AMBStatus.CONNECTING;
		final AsyncResultImpl<Exception> res = new AsyncResultImpl<Exception>();

		try
		{
			client.connect(seed).setCallback(new SingleArgumentCallback<Boolean>()
			{

				public void receiveSingleArgumentCallback(Boolean result)
				{
					if (result)
					{
						status = AMBStatus.CONNECTED;
						onconnect.onconnect(AMBTMB.this);
						res.completed(null);
					}
					else
					{
						status = AMBStatus.DISCONNECTED;
						res.completed(new Exception("Could not connect..."));
					}
				}
			});
		}
		catch (Exception e)
		{
			status = AMBStatus.DISCONNECTED;
			res.completed(e);
		}

		return res;
	}

	public void disconnect()
	{
		status = AMBStatus.DISCONNECTED;
		client.close();
	}

	public String seedInfo()
	{
		return seed;
	}

	public void onDirect(String from, byte[] data)
	{
		MessageImpl m = new MessageImpl();
		m.from = from;
		m.to = client.getName();
		m.payload = data;

		if (onmessage != null)
			onmessage.onmessage(this, m);
	}

	public void onChannel(String channel, String from, byte[] data)
	{
		ChannelImpl chan = subs.get(channel);
		
		if (chan != null)
			chan.receive(from, data);
	}

	public void onClose()
	{
		status = AMBStatus.DISCONNECTED;
		if (ondisconnect != null)
			ondisconnect.ondisconnect(this);
	}

}
