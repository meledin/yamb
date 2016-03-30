package us.yamb.tmb;

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
		System.out.println(b.getURI());

		final SingleCallback<byte[]> cb = new SingleCallback<byte[]>();

		byte[] testData = UUID.randomUUID().getBytes();

		Client c = new Client();
		c.connect(b.getURI()).get();

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

		final SingleCallback<byte[]> cb = new SingleCallback<byte[]>();

		byte[] testData = UUID.randomUUID().getBytes();

		Client c = new Client();
        c.connect(b.getURI()).get();

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
        s.connect(b.getURI()).get();
		s.send(c.getName(), testData);

		Assert.assertArrayEquals(testData, cb.get());

	}

	@Test
	public void testClose() throws Exception
	{
		Broker b = new Broker();
		b.listen("localhost", 0).get();

		final SingleCallback<Boolean> cb = new SingleCallback<Boolean>();

		Client c = new Client();
        c.connect(b.getURI()).get();

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
