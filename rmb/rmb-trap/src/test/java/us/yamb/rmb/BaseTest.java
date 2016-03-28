package us.yamb.rmb;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.research.trap.utils.JDKLoggerConfig;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.builders.RMBBuilder;

public class BaseTest
{
    
    private RMB server;
    private RMB client;
    
    @Before
    public void broker() throws Exception
    {
        JDKLoggerConfig.initForPrefixes(Level.INFO, "us.yamb");
        server = RMBBuilder.builder().seed("server:").build();
        server.connect().get();
        
        client = RMBBuilder.builder().seed(server.seedInfo()).build();
        client.connect().get();
        
    }
    
    @Test
    //(timeout = 10000)
    public void basicMessage() throws Exception
    {
        AsyncResultImpl<Message> res = new AsyncResultImpl<>();
        
        server.onmessage(message -> res.completed(message));
        client.message().to(server.id()).data("hello").send();
        
        Assert.assertEquals("hello", res.get().string());
    }
    
    @Test
    //(timeout = 10000)
    public void basicResource() throws Exception
    {
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();
        
        server.add(new BasicResource(server));
        
        client.get(server.id()).execute(res, (reason, e) -> {
            if (e != null)
                e.printStackTrace();
            System.err.println(reason);
        });
        
        Assert.assertEquals("Hello!", res.get().string());
        
    }
    
    @Test(timeout = 10000)
    public void foo() throws Exception
    {
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();
        
        server.add(new BasicResource(server));
        client.get(server.id() + "/foo" + "/foo").execute(res);
        
        Assert.assertEquals("foo", res.get().string());
        
    }
    
    @Test(timeout = 10000)
    public void bar() throws Exception
    {
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();
        
        server.add(new BasicResource(server));
        client.get(server.id() + "/foo" + "/bar").execute(res);
        
        Assert.assertEquals("bar", res.get().string());
        
    }
    
    @Test(timeout = 10000)
    public void testConfirmed() throws Exception
    {
        AsyncResultImpl<Reply> res = new AsyncResultImpl<>();
        
        server.add(new BasicResource(server));
        client.get(server.id() + "/slow").confirmed(100).confirmed(true).execute(res);
        
        Assert.assertEquals("slow", res.get().string());
        
    }
}
