package us.yamb.tmb;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnFailedSending;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.utils.Callback;
import com.ericsson.research.trap.utils.UID;
import com.ericsson.research.trap.utils.impl.SingleCallback;

public class Client implements OnOpen, OnClose, OnData, OnError, OnFailedSending
{
    
    public interface Handler
    {
        void onDirect(String from, byte[] data);
        
        void onChannel(String channel, String from, byte[] data);
        
        void onClose();
    }
    
    public void setHandler(Handler handler)
    {
        this.handler = handler;
    }
    
    private SingleCallback<Boolean> connectCb;
    private Handler                 handler;
    private String                  name = null;
    
    private TrapClient              client;
    
    public Client()
    {
    }
    
    public Client(String name)
    {
        this.name = name;
    }
    
    public Callback<Boolean> connect(String src) throws TrapException
    {
        client = TrapFactory.createClient(src, true);
        client.setDelegate(this, true);
        client.open();
        connectCb = new SingleCallback<Boolean>();
        return connectCb;
    }
    
    protected void handle(Message m)
    {
        
        switch (m.op)
        {
            case Message.Operation.HELLO:
                name = m.to;
                notify(true);
                break;
            
            case Message.Operation.PUB:
                handler.onChannel(m.channel, m.from, m.payload);
                break;
            
            case Message.Operation.SEND:
                handler.onDirect(m.from, m.payload);
                break;
            
            default:
                return; // No such method
        }
    }
    
    boolean notify(boolean value)
    {
        if (connectCb == null)
            return false;
        
        connectCb.callback(value);
        connectCb = null;
        return true;
    }
    
    public void send(String to, byte[] data)
    {
        Message m = new Message();
        m.op = Message.Operation.SEND;
        m.to = to;
        m.payload = data;
        send(m);
    }
    
    public void subscribe(String channel)
    {
        Message m = new Message();
        m.op = Message.Operation.SUB;
        m.channel = channel;
        send(m);
    }
    
    public void unsubscribe(String channel)
    {
        Message m = new Message();
        m.op = Message.Operation.UNSUB;
        m.channel = channel;
        send(m);
    }
    
    public void publish(String channel, byte[] data)
    {
        Message m = new Message();
        m.op = Message.Operation.PUB;
        m.channel = channel;
        m.payload = data;
        send(m);
    }
    
    private AtomicInteger channel = new AtomicInteger();
    
    private void send(Message m)
    {
        try
        {
            client.send(m.serialize(), channel.incrementAndGet() % 32 + 1, true);
        }
        catch (TrapException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String getName()
    {
        return name;
    }
    
    public void close()
    {
        Message m = new Message();
        m.op = Message.Operation.BYE;
        send(m);
    }

    @Override
    public void trapError(TrapEndpoint endpoint, Object context)
    {
        if (!Client.this.notify(false))
            handler.onClose();        
    }

    @Override
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        try
        {
            ByteBuffer readBuf = ByteBuffer.wrap(data);
            
            Message m = null;
            
            while ((m = Message.deserialize(readBuf)) != null)
                Client.this.handle(m);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }        
    }

    @Override
    public void trapClose(TrapEndpoint endpoint, Object context)
    {
        if (!Client.this.notify(false))
            handler.onClose();        
    }

    @Override
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        Message m = new Message();
        m.to = (name != null ? name : UID.randomUID());
        m.op = Message.Operation.HELLO;
        send(m);        
    }

    @Override
    public void trapFailedSending(Collection<?> datas, TrapEndpoint endpoint, Object context)
    {
        throw new RuntimeException();
    }
}
