package us.yamb.rmb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.builders.RMBBuilder;
import us.yamb.tmb.Broker;

public class BaseTest
{

	private Broker	broker;

	@Before
	public void broker() throws Exception
	{
		broker = new Broker();
		broker.listen("127.0.0.1", 0).get();
	}

	@Test//(timeout = 10000)
	public void basicMessage() throws Exception
	{
		RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
		rmb.connect().get();
		System.out.println(rmb.id());

		AsyncResultImpl<Message> res = new AsyncResultImpl<>();

		rmb.onmessage(message -> res.completed(message));
		rmb.message().to(rmb.id()).data("hello").send();

		Assert.assertEquals("hello", res.get().string());
	}

    @Test//(timeout = 10000)
    public void basicResource() throws Exception
    {
        RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
        rmb.connect().get();
        System.out.println(rmb.id());
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();

        rmb.add(new BasicResource(rmb));
        
        rmb.get(rmb.id()).execute(res);
        
        Assert.assertEquals("Hello!", res.get().string());

    }

    @Test(timeout = 10000)
    public void foo() throws Exception
    {
        RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
        rmb.connect().get();
        System.out.println(rmb.id());
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();

        rmb.add(new BasicResource(rmb));
        rmb.get(rmb.id() + "/foo" + "/foo").execute(res);
        
        Assert.assertEquals("foo", res.get().string());

    }

    @Test(timeout = 10000)
    public void bar() throws Exception
    {
        RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
        rmb.connect().get();
        System.out.println(rmb.id());
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();

        rmb.add(new BasicResource(rmb));
        rmb.get(rmb.id() + "/foo" + "/bar").execute(res);
        
        Assert.assertEquals("bar", res.get().string());

    }
}
