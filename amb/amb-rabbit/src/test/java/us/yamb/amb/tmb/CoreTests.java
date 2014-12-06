package us.yamb.amb.tmb;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import us.yamb.amb.AMB;
import us.yamb.amb.Channel;
import us.yamb.amb.Message;
import us.yamb.amb.builders.AMBBuilder;
import us.yamb.amb.callbacks.OnChannel;
import us.yamb.amb.callbacks.OnMessage;
import us.yamb.amb.spi.AsyncResultImpl;

import com.ericsson.research.trap.utils.UID;

public class CoreTests
{
	

	@Before
	public void setUp() throws Exception
	{
	}
	
	@After
	public void tearDown() throws Exception
	{
	}
	
	@Test
	public void testBase() throws Exception
	{
		AMB amb = AMBBuilder.builder().seed("rabbitmq://localhost:5672").build();
		
		String testValue = UID.randomUID();
		final AsyncResultImpl<String> res = new AsyncResultImpl<String>();
		
		amb.setCallback(new OnMessage()
		{
			
			public void onmessage(AMB amb, Message message)
			{
				System.out.println(message.string());
				res.callback(message.string());
			}
		});
		
		amb.connect().get();
		amb.message().to(amb.id()).data(testValue).send();
		
		Assert.assertEquals(testValue, res.get());
		
		amb.disconnect();
		
	}
	
	@Test
	public void testChannel() throws Exception
	{
		AMB amb = AMBBuilder.builder().seed("rabbitmq://localhost:5672").build();
		
		String testValue = UID.randomUID();
		final AsyncResultImpl<String> res = new AsyncResultImpl<String>();
		
		amb.connect().get();
		
		Channel channel = amb.channel().name("hello").build();
		channel.setCallback(new OnChannel()
		{
			public void onchannel(AMB amb, Channel channel, Message message)
			{
				res.callback(message.string());
			}
		});
		
		channel.join().get();
		channel.send(testValue);
		
		//Assert.assertEquals(testValue, res.get());
		
		for (;;)
			Thread.sleep(10);
		
		//amb.disconnect();
		
	}
}
