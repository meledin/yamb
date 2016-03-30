package us.yamb.tmb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapJS;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnFailedSending;
import com.ericsson.research.trap.spi.nhttp.handlers.Resource;
import com.ericsson.research.trap.utils.Base64;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public class Broker implements OnAccept, OnError, OnFailedSending
{
    private static Broker                                                broker;
    private static Resource                                              trapjs;
    private static Resource                                              brokerjs;
    private static Resource                                              testhtml;
    private ConcurrentLinkedQueue<Client>                                connecting = new ConcurrentLinkedQueue<Client>();
    private static final ConcurrentLinkedQueue<Client>                   nullQueue  = new ConcurrentLinkedQueue<Broker.Client>();
    private ConcurrentSkipListMap<String, ConcurrentLinkedQueue<Client>> subs       = new ConcurrentSkipListMap<String, ConcurrentLinkedQueue<Client>>();
    private ConcurrentSkipListMap<String, Client>                        clients    = new ConcurrentSkipListMap<String, Broker.Client>();
    private SingleCallback<Boolean>                                      cb;
    private TrapListener                                                 server;
    
    public static void main(String[] args) throws IOException, InterruptedException
    {
        broker = new Broker();
        broker.listen(args.length > 1 ? args[0] : "127.0.0.1", args.length > 2 ? Integer.parseInt(args[1]) : 1443);
        
        trapjs = new Resource(() -> TrapJS.getFull());
        brokerjs = new Resource(() -> Broker.getFull());
        testhtml = new Resource(() -> Broker.class.getClassLoader().getResourceAsStream("test.html"));
        
        broker.getServer().getHostingTransport("http").addHostedObject(trapjs, "trap-full.js");
        broker.getServer().getHostingTransport("http").addHostedObject(brokerjs, "tmb.js");
        broker.getServer().getHostingTransport("http").addHostedObject(testhtml, "test.html");
        
        System.out.println(broker.getURI());
        
        for (;;)
            Thread.sleep(1000);
    }
    
    public static InputStream getFull()
    {
        return Broker.class.getClassLoader().getResourceAsStream("tmb.js");
    }
    
    public Callback<Boolean> listen(String host, int port)
    {
        this.cb = new SingleCallback<Boolean>();
        try
        {
            this.server = TrapFactory.createListener();
            server.setOption("host", host);
            server.configureTransport("http", "port", "" + port);
            server.listen(this);
            server.getClientConfiguration();
            cb.callback(true);
        }
        catch (TrapException e)
        {
            cb.callback(false);
        }
        return cb;
    }
    
    public void close()
    {
        server.close();
        
        for (Client c : connecting)
            c.cleanup();
        connecting = null;
        subs = null;
        for (Client c : clients.values())
            c.cleanup();
        clients = null;
        cb = null;
    }
    
    public String getURI()
    {
        return server.getClientConfiguration();
    }
    
    public void handle(Message m, Client c)
    {
        switch (m.op)
        {
            case Message.Operation.HELLO:
                c.handle = m.payload;
                if (clients.putIfAbsent(m.to, c) == null)
                {
                    c.name = m.to;
                }
                else
                {
                    Client existing = clients.get(m.to);
                    
                    if (Arrays.equals(existing.handle, c.handle))
                    {
                        clients.put(m.to, c);
                        c.name = m.to;
                    }
                    else
                        clients.put(c.name, c);
                }
                Message resp = new Message();
                resp.op = Message.Operation.HELLO;
                resp.to = c.name;
                c.send(resp);
                break;
            
            case Message.Operation.BYE:
                c.cleanup();
                break;
            
            case Message.Operation.SUB:
                ConcurrentLinkedQueue<Client> clientList = subs.get(m.channel);
                if (clientList == null)
                {
                    subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
                    clientList = subs.get(m.channel);
                }
                clientList.add(c);
                break;
            
            case Message.Operation.PUB:
                clientList = subs.get(m.channel);
                if (clientList == null)
                {
                    subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
                    clientList = subs.get(m.channel);
                }
                
                for (Client sc : clientList)
                    sc.send(m);
                
                break;
            
            case Message.Operation.UNSUB:
                clientList = subs.get(m.channel);
                if (clientList == null)
                {
                    subs.putIfAbsent(m.channel, new ConcurrentLinkedQueue<Broker.Client>());
                    clientList = subs.get(m.channel);
                }
                clientList.remove(c);
                break;
            
            case Message.Operation.SEND:
                Client client = clients.get(m.to);
                if (client != null)
                    client.send(m);
                break;
            
            default:
                return; // No such method
        }
    }
    
    class Client implements OnClose, OnData, OnError
    {
        public ConcurrentLinkedQueue<String> subs   = new ConcurrentLinkedQueue<String>();
        public String                        name   = UID.randomUID();
        public byte[]                        handle = null;
        private TrapEndpoint                 ep;
        
        public Client(TrapEndpoint endpoint)
        {
            this.ep = endpoint;
            this.ep.setDelegate(this, true);
            this.ep.setReconnectTimeout(1000);
            this.ep.getChannel(1).setInFlightBytes(256 * 1024);
        }
        
        public void cleanup()
        {
            for (String sub : subs)
                Broker.this.subs.getOrDefault(sub, nullQueue).remove(this);
            
            clients.remove(name);
            connecting.remove(this);
            
            if (ep != null)
                ep.close();
            ep = null;
        }
        
        void readBuf(ByteBuffer data, ByteArrayOutputStream out)
        {
            
            byte[] buf = new byte[4096];
            
            while (data.remaining() > 0)
            {
                int read = Math.min(data.remaining(), buf.length);
                data.get(buf);
                out.write(buf, 0, read);
            }
        }
        
        AtomicInteger channel = new AtomicInteger();
        
        public void send(Message m)
        {
            try
            {
                ep.send(m.serialize(), channel.incrementAndGet() % 32 + 1, true);
            }
            catch (TrapException e)
            {
                e.printStackTrace();
                trapClose(null, null);
            }
        }
        
        @Override
        public void trapError(TrapEndpoint endpoint, Object context)
        {
            Message m = new Message();
            m.op = Message.Operation.BYE;
            m.from = this.name;
            Broker.this.handle(m, this);
        }
        
        @Override
        public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
        {
            try
            {
                
                ByteBuffer readBuf = ByteBuffer.wrap(data);
                
                Message m = null;
                
                while ((m = Message.deserialize(readBuf)) != null)
                    Broker.this.handle(m, this);
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }
        
        @Override
        public void trapClose(TrapEndpoint endpoint, Object context)
        {
            Message m = new Message();
            m.op = Message.Operation.BYE;
            m.from = this.name;
            Broker.this.handle(m, this);
        }
        
        public Map<String, Object> getJsonifiable()
        {
            ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
            
            map.put("description", "Broker Client");
            map.put("name", name);
            map.put("handle", new String(Base64.encode(handle)));
            map.put("status", ep.getState());
            map.put("subs", subs.toString());
            
            return map;
        }
    }
    
    @Override
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        Client client = new Client(endpoint);
        connecting.add(client);
    }
    
    @Override
    public void trapError(TrapEndpoint endpoint, Object context)
    {
        if (cb != null)
            cb.callback(false);
    }
    
    public TrapListener getServer()
    {
        return server;
    }
    
    @Override
    public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context)
    {
        throw new RuntimeException();
    }
    
    public Map<String, Object> getJsonifiable()
    {
        ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();
        
        clients.values().parallelStream().forEach((client) -> {
            map.put(client.name, client.getJsonifiable());
        });
        
        return map;
    }
    
}
