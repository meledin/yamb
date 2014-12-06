package us.yamb.amb.tmb;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import us.yamb.amb.AMB;
import us.yamb.amb.Message;
import us.yamb.amb.builders.AMBBuilder;
import us.yamb.amb.callbacks.OnMessage;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.tmb.Broker;

import com.ericsson.research.trap.utils.UID;

public class CoreTests
{
	
	private Broker broker;

	@Before
	public void setUp() throws Exception
	{
		broker = new Broker();
		broker.listen("localhost", 0).get();
	}
	
	@After
	public void tearDown() throws Exception
	{
		broker.close();
	}
	
	@Test
	public void testBase() throws Exception
	{
		AMB amb = AMBBuilder.builder().seed(broker.getURI()).build();
		
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
}
