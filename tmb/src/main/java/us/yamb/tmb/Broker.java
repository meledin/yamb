package us.yamb.tmb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

import com.ericsson.research.transport.ws.WSAcceptListener;
import com.ericsson.research.transport.ws.WSFactory;
import com.ericsson.research.transport.ws.WSInterface;
import com.ericsson.research.transport.ws.WSListener;
import com.ericsson.research.transport.ws.WSServer;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public class Broker implements WSAcceptListener
{
	private static Broker	                                             broker;
	private WSServer	                                                 socket;
	private ConcurrentLinkedQueue<Client>	                             connecting	= new ConcurrentLinkedQueue<Client>();
	private static final ConcurrentLinkedQueue<Client>	                 nullQueue	= new ConcurrentLinkedQueue<Broker.Client>();
	private ConcurrentSkipListMap<String, ConcurrentLinkedQueue<Client>>	subs	= new ConcurrentSkipListMap<String, ConcurrentLinkedQueue<Client>>();
	private ConcurrentSkipListMap<String, Client>	                     clients	= new ConcurrentSkipListMap<String, Broker.Client>();
	private SingleCallback<Boolean>	                                     cb;

	public static void main(String[] args) throws IOException, InterruptedException
	{
		broker = new Broker();
		broker.listen(args[0], Integer.parseInt(args[1]));

		for (;;)
			Thread.sleep(1000);
	}

	public Callback<Boolean> listen(String host, int port)
	{
		this.cb = new SingleCallback<Boolean>();
		try
		{
			this.socket = WSFactory.createWebSocketServer(host, port, this, null);
		}
		catch (IOException e)
		{
			cb.callback(false);
		}
		return cb;
	}

	public void close()
	{
		socket.close();

		for (Client c : connecting)
			c.cleanup();
		connecting = null;
		subs = null;
		for (Client c : clients.values())
			c.cleanup();
		clients = null;
		cb = null;
	}

	public void notifyAccept(WSInterface socket)
	{

		Client client = new Client(socket);
		connecting.add(client);
	}

	public InetSocketAddress getAddress()
	{
		return socket.getAddress();
	}

	public String getURI()
	{
		return "ws://" + socket.getAddress().getHostString() + ":" + socket.getAddress().getPort();
	}

	public void notifyReady(WSServer server)
	{
		cb.callback(true);
	}

	public void notifyError(Throwable t)
	{
		// t.printStackTrace();
		if (cb != null)
			cb.callback(false);
	}

	public void handle(Message m, Client c)
	{
		switch (m.op)
		{
			case Message.Operation.HELLO:
				if (clients.putIfAbsent(m.to, c) == null)
				{
					c.name = m.to;
				}
				else
				    clients.put(c.name, c);
				Message resp = new Message();
				resp.op = Message.Operation.HELLO;
				resp.to = c.name;
				c.send(resp);
				break;

			case Message.Operation.BYE:
				c.cleanup();
				break;

			case Message.Operation.SUB:
				ConcurrentLinkedQueue<Client> clientList = subs.get(m.channel);
				if (clientList == null)
				{
					subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
					clientList = subs.get(m.channel);
				}
				clientList.add(c);
				break;

			case Message.Operation.PUB:
				clientList = subs.get(m.channel);
				if (clientList == null)
				{
					subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
					clientList = subs.get(m.channel);
				}

				for (Client sc : clientList)
					sc.send(m);

				break;

			case Message.Operation.UNSUB:
				clientList = subs.get(m.channel);
				if (clientList == null)
				{
					subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
					clientList = subs.get(m.channel);
				}
				clientList.remove(c);
				break;

			case Message.Operation.SEND:
				Client client = clients.get(m.to);
				if (client != null)
					client.send(m);
				break;

			default:
				return; // No such method
		}
	}

	class Client implements WSListener
	{
		private WSInterface		             sock;
		public ConcurrentLinkedQueue<String>	subs	= new ConcurrentLinkedQueue<String>();
		public String		                 name		= UID.randomUID();

		public Client(WSInterface socket)
		{
			this.sock = socket;
			sock.setReadListener(this);

		}

		public void cleanup()
		{
			for (String sub : subs)
				Broker.this.subs.getOrDefault(sub, nullQueue).remove(this);

			clients.remove(name);
			connecting.remove(this);

			if (sock != null)
				sock.close();
			sock = null;
		}

		void readBuf(ByteBuffer data, ByteArrayOutputStream out)
		{

			byte[] buf = new byte[4096];

			while (data.remaining() > 0)
			{
				int read = Math.min(data.remaining(), buf.length);
				data.get(buf);
				out.write(buf, 0, read);
			}
		}

		public void send(Message m)
		{
			try
			{
				sock.send(m.serialize());
			}
			catch (IOException e)
			{
				e.printStackTrace();
				notifyClose();
			}
		}

		public void notifyOpen(WSInterface socket)
		{

		}

		public void notifyClose()
		{
			Message m = new Message();
			m.op = Message.Operation.BYE;
			m.from = this.name;
			Broker.this.handle(m, this);
		}

		public void notifyMessage(String utf8String)
		{
			// Cannot accept string payloads
		}

		public void notifyMessage(byte[] data)
		{
			try
			{

				ByteBuffer readBuf = ByteBuffer.wrap(data);

				Message m = null;

				while ((m = Message.deserialize(readBuf)) != null)
					Broker.this.handle(m, this);
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
		}

		public void notifyPong(byte[] payload)
		{
			// TODO Auto-generated method stub

		}

		public void notifyError(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
