package us.yamb.amb.rabbit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import us.yamb.amb.Channel;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.amb.spi.ChannelBase;
import us.yamb.mb.callbacks.AsyncResult;

import com.ericsson.research.trap.utils.StringUtil;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class ChannelImpl extends ChannelBase
{

	private final String	name;
	private final AMBRabbit	parent;
	private DefaultConsumer	consumer;

	public ChannelImpl(AMBRabbit parent, String name)
	{
		this.parent = parent;
		this.name = name;
	}

	public String name()
	{
		return name;
	}

	public Channel send(byte[] data) throws IOException
	{
		MessageImpl m = new MessageImpl();
		m.payload = data;
		m.from = parent.id;
		m.to = name;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(m);
		
		oos.close();
		
		byte[] bs = bos.toByteArray();
		
		parent.channel.basicPublish("", name, null, bs);

		return this;
	}

	public Channel send(String data) throws IOException
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
		final AsyncResultImpl<Boolean> res = new AsyncResultImpl<Boolean>();

		try
		{
			consumer = new DefaultConsumer(parent.channel)
			{

				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException
				{

					MessageImpl msg;
					try
					{
						
						msg = (MessageImpl) new ObjectInputStream(new ByteArrayInputStream(body)).readObject();

						if (onchannel != null)
							onchannel.onchannel(parent, ChannelImpl.this, msg);
						
						parent.channel.basicAck(envelope.getDeliveryTag(), false);
					}
					catch (ClassNotFoundException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			};
			parent.channel.queueDeclare(name, false, false, false, null);
			parent.channel.basicConsume(name, consumer);
		}
		catch (IOException e)
		{
			res.completed(false);
		}
		res.completed(true);

		return res;
	}

	public void leave()
	{
		// TODO: How do you do this???
	}

}
