package us.yamb.rmb.impl;

import us.yamb.amb.AMB;
import us.yamb.amb.Channel;
import us.yamb.amb.Message;
import us.yamb.amb.callbacks.OnChannel;
import us.yamb.amb.callbacks.OnConnect;
import us.yamb.amb.callbacks.OnMessage;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.builders.SendImpl;

public class RMBRoot extends RMBImpl implements OnConnect, OnChannel, OnMessage
{

	private AMB	amb;

	public RMBRoot(AMB amb)
	{
		this.amb = amb;
		amb.setCallback(this);
	}

	public String id()
	{
		if (name == null)
			throw new IllegalStateException("Cannot get ID of RMB that is not connected");
		
		return "/"+name;
	}

	public AsyncResult<Exception> connect()
	{
		return amb.connect();
	}

	public void disconnect()
	{
		amb.disconnect();
	}

	public RMBStatus status()
    {
		switch(amb.status())
		{
			case CONNECTED:
				return RMBStatus.CONNECTED;
			case CONNECTING:
				return RMBStatus.CONNECTING;
			case DISCONNECTED:
			default:
				return RMBStatus.DISCONNECTED;
			
		}
    }

	public void onconnect(AMB amb)
    {
		this.name = amb.id();
    }

	@Override
	public Send message(RMBImpl res)
    {
	    return new SendImpl(res, amb.message());
    }

	@Override
	public String seedInfo() {
		return amb.seedInfo();
	}

    @Override
    public void onmessage(AMB amb, Message message)
    {
        RMBMessage<?> msg = RMBMessage.deserialize(message.bytes());
        dispatch(msg, 1);
    }

    @Override
    public void onchannel(AMB amb, Channel channel, Message message)
    {
        // TODO Auto-generated method stub
        
    }
}
