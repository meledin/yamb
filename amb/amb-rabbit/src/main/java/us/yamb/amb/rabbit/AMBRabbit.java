package us.yamb.amb.rabbit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import us.yamb.amb.AMB;
import us.yamb.amb.AMBStatus;
import us.yamb.amb.Send;
import us.yamb.amb.builders.ChannelBuilder;
import us.yamb.amb.spi.AMBase;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.callbacks.AsyncResult;

import com.ericsson.research.trap.utils.UUID;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class AMBRabbit extends AMBase implements AMB
{

	String	   host;
	int	       port;
	String	   id	= UUID.randomUUID();

	Channel	   channel;
	Connection	connection;

	public AMBRabbit(String host, int port)
	{
		this.host = host;
		this.port = port;
	}

	public String id()
	{
		return id;
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

			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(host);
			if (port != 0)
				factory.setPort(port);
			connection = factory.newConnection();
			channel = connection.createChannel();
			status = AMBStatus.CONNECTED;
			res.callback(null);

			channel.queueDeclare(id, false, false, false, null);
			channel.basicConsume(id, new DefaultConsumer(channel)
			{

				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException
				{

					MessageImpl msg;
					try
					{
						msg = (MessageImpl) new ObjectInputStream(new ByteArrayInputStream(body)).readObject();

						if (onmessage != null)
							onmessage.onmessage(AMBRabbit.this, msg);
						channel.basicAck(envelope.getDeliveryTag(), false);
					}
					catch (ClassNotFoundException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});

		}
		catch (Exception e)
		{
			status = AMBStatus.DISCONNECTED;
			res.callback(e);
		}

		return res;
	}

	public void disconnect()
	{
		status = AMBStatus.DISCONNECTED;
		try
		{
			channel.close();
			connection.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		channel = null;
	}

	public String seedInfo()
	{
		return "rabbitmq://" + host + ":" + port;
	}

	public void onClose()
	{
		status = AMBStatus.DISCONNECTED;
		if (ondisconnect != null)
			ondisconnect.ondisconnect(this);
	}

}
