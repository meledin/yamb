package us.yamb.tmb;

import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;

import us.yamb.tmb.Client.Handler;

import com.ericsson.research.trap.utils.UUID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public class TMBServer
{
	@Test
	public void testPubSub() throws Exception
	{
		Broker b = new Broker();
		b.listen("localhost", 0).get();
		InetSocketAddress addr = b.getAddress();
		System.out.println(addr);

		final SingleCallback<byte[]> cb = new SingleCallback<byte[]>();

		byte[] testData = UUID.randomUUID().getBytes();

		Client c = new Client();
		c.connect(addr.getHostString(), addr.getPort()).get();

		c.subscribe("foo");
		c.setHandler(new Handler()
		{

			public void onDirect(String from, byte[] data)
			{
				// TODO Auto-generated method stub

			}

			public void onClose()
			{
				// TODO Auto-generated method stub

			}

			public void onChannel(String channel, String from, byte[] data)
			{
				cb.callback(data);
			}
		});
		c.publish("foo", testData);

		Assert.assertArrayEquals(testData, cb.get());

	}

	@Test
	public void testDirect() throws Exception
	{
		Broker b = new Broker();
		b.listen("localhost", 0).get();
		InetSocketAddress addr = b.getAddress();
		System.out.println(addr);

		final SingleCallback<byte[]> cb = new SingleCallback<byte[]>();

		byte[] testData = UUID.randomUUID().getBytes();

		Client c = new Client();
		c.connect(addr.getHostString(), addr.getPort()).get();

		c.subscribe("foo");
		c.setHandler(new Handler()
		{

			public void onDirect(String from, byte[] data)
			{
				cb.callback(data);
			}

			public void onClose()
			{
			}

			public void onChannel(String channel, String from, byte[] data)
			{
			}
		});

		Client s = new Client();
		s.connect(addr.getHostString(), addr.getPort()).get();
		s.send(c.getName(), testData);

		Assert.assertArrayEquals(testData, cb.get());

	}

	@Test
	public void testClose() throws Exception
	{
		Broker b = new Broker();
		b.listen("localhost", 0).get();
		InetSocketAddress addr = b.getAddress();
		System.out.println(addr);

		final SingleCallback<Boolean> cb = new SingleCallback<Boolean>();

		Client c = new Client();
		c.connect(addr.getHostString(), addr.getPort()).get();

		c.setHandler(new Handler()
		{

			public void onDirect(String from, byte[] data)
			{
			}

			public void onClose()
			{
				cb.callback(true);
			}

			public void onChannel(String channel, String from, byte[] data)
			{
			}
		});

		c.close();

		Assert.assertTrue(cb.get());

	}
}
