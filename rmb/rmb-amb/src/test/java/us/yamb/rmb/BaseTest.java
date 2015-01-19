package us.yamb.rmb;

import org.junit.Before;
import org.junit.Test;

import us.yamb.rmb.builders.RMBBuilder;
import us.yamb.tmb.Broker;

public class BaseTest
{
    
    private Broker broker;
    
    @Before
    public void broker() throws Exception
    {
        broker = new Broker();
        broker.listen("127.0.0.1", 0).get();
    }
    
    @Test
    public void basicMessage() throws Exception
    {
        RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
        rmb.connect().get();
        System.out.println(rmb.id());
        rmb.onmessage(message -> System.out.println(message));
        rmb.message().to(rmb.id()).send();
        
        for (;;)
            Thread.sleep(1000);
    }
    
    @Test
    public void basicResource() throws Exception
    {
        RMB rmb = RMBBuilder.builder().seed(broker.getURI()).build();
        rmb.connect().get();
        System.out.println(rmb.id());
        rmb.add(new BasicResource(rmb));
        rmb.request().to(rmb.id()).method("GET").execute(m -> {
            System.out.println(m.string());
        });
    }
}
