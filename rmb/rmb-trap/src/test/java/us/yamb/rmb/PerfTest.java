package us.yamb.rmb;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ericsson.research.trap.TrapTransports;
import com.ericsson.research.trap.spi.transports.ListenerHttpTransport;
import com.ericsson.research.trap.spi.transports.ServerWebSocketTransport;
import com.ericsson.research.trap.spi.transports.WebSocketTransport;
import com.ericsson.research.trap.utils.JDKLoggerConfig;

import us.yamb.rmb.builders.RMBBuilder;

@RunWith(Parameterized.class)
public class PerfTest
{
    
    private RMB server;
    private RMB client;
    
    private CountDownLatch latch;
    private RMB            foobar;
    
    @Parameters
    public static Iterable<Object[]> params()
    {
        return Arrays.asList(new Object[10][]);
    }
    
    @Before
    public void broker() throws Exception
    {
        TrapTransports.setUseAutodiscoveredTransports(false);
        TrapTransports.addTransportClass(WebSocketTransport.class);
        TrapTransports.addTransportClass(ListenerHttpTransport.class);
        TrapTransports.addTransportClass(ServerWebSocketTransport.class);
        
        JDKLoggerConfig.initForPrefixes(Level.INFO, "us.yamb");
        server = RMBBuilder.builder().seed("server:").build();
        server.connect().get();
        
        client = RMBBuilder.builder().seed(server.seedInfo()).build();
        client.connect().get();
        
        for (int i=0; i<500; i++)
        {
            server.create();
        }
        RMB tmp = server.create();
        foobar = tmp.create().onmessage(msg -> latch.countDown());
        
        for (int i=0; i<500; i++)
        {
            server.create();
            tmp.create();
        }
        
    }
    
    @Test
    //(timeout = 10000)
    public void basicMessage() throws Exception
    {
        int N_COUNT = 10000;
        latch = new CountDownLatch(N_COUNT);
        for (int i=0; i<N_COUNT; i++)
            client.message().method("POST").to(foobar.id()).data("hello").send();
        latch.await();
    }
    
}
