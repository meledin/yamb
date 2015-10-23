package us.yamb.rmb;

import us.yamb.tmb.Broker;

public class JSTestServer
{
    private static Broker broker;
    
    public static void main(String[] args) throws InterruptedException
    {
        broker = new Broker();
        broker.listen("127.0.0.1", 4442).get();
        System.out.println(broker.getURI());
        
        for (;;)
            Thread.sleep(1000);
    }
}
