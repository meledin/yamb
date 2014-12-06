package us.yamb.tmb;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSURI;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public class Client
{

	public interface Handler
	{
		void onDirect(String from, byte[] data);

		void onChannel(String channel, String from, byte[] data);

		void onClose();
	}

	public void setHandler(Handler handler)
	{
		this.handler = handler;
	}

	private WSListener	            wsl	 = new WSListener()
	                                     {

		                                     public void notifyOpen(WSInterface socket)
		                                     {
			                                     Message m = new Message();
			                                     m.to = (name != null ? name : UID.randomUID());
			                                     m.op = Message.Operation.HELLO;
			                                     send(m);
		                                     }

		                                     public void notifyClose()
		                                     {
			                                     if (!Client.this.notify(false))
				                                     handler.onClose();
		                                     }

		                                     public void notifyMessage(String utf8String)
		                                     {
			                                     // Unused
		                                     }

		                                     public void notifyMessage(byte[] data)
		                                     {
			                                     try
			                                     {
				                                     ByteBuffer readBuf = ByteBuffer.wrap(data);

				                                     Message m = null;

				                                     while ((m = Message.deserialize(readBuf)) != null)
					                                     Client.this.handle(m, this);
			                                     }
			                                     catch (Throwable t)
			                                     {
				                                     t.printStackTrace();
			                                     }
		                                     }

		                                     public void notifyPong(byte[] payload)
		                                     {

		                                     }

		                                     public void notifyError(Throwable t)
		                                     {
			                                     t.printStackTrace();
			                                     if (!Client.this.notify(false))
				                                     handler.onClose();
		                                     }
	                                     };

	private WSInterface	            sock;
	private SingleCallback<Boolean>	connectCb;
	private Handler	                handler;
	private String	                name	= null;

	public Client()
	{
	}

	public Client(String name)
	{
		this.name = name;
	}

	public Callback<Boolean> connect(String host, int port) throws IllegalArgumentException, IOException
	{
		sock = WSFactory.createWebSocketClient(new WSURI("ws://" + host + ":" + port + "/ws"), wsl, WSFactory.VERSION_RFC_6455, null);
		sock.open();
		connectCb = new SingleCallback<Boolean>();
		return connectCb;
	}

	protected void handle(Message m, WSListener wsListener)
	{

		switch (m.op)
		{
			case Message.Operation.HELLO:
				name = m.to;
				notify(true);
				break;

			case Message.Operation.PUB:
				handler.onChannel(m.channel, m.from, m.payload);
				break;

			case Message.Operation.SEND:
				handler.onDirect(m.from, m.payload);
				break;

			default:
				return; // No such method
		}
	}

	boolean notify(boolean value)
	{
		if (connectCb == null)
			return false;

		connectCb.callback(value);
		connectCb = null;
		return true;
	}

	public void send(String to, byte[] data)
	{
		Message m = new Message();
		m.op = Message.Operation.SEND;
		m.to = to;
		m.payload = data;
		send(m);
	}

	public void subscribe(String channel)
	{
		Message m = new Message();
		m.op = Message.Operation.SUB;
		m.channel = channel;
		send(m);
	}

	public void unsubscribe(String channel)
	{
		Message m = new Message();
		m.op = Message.Operation.UNSUB;
		m.channel = channel;
		send(m);
	}

	public void publish(String channel, byte[] data)
	{
		Message m = new Message();
		m.op = Message.Operation.PUB;
		m.channel = channel;
		m.payload = data;
		send(m);
	}

	private void send(Message m)
	{
		try
		{
			sock.send(m.serialize());
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getName()
	{
		return name;
	}

	public void close()
	{
		Message m = new Message();
		m.op = Message.Operation.BYE;
		send(m);
	}
}
