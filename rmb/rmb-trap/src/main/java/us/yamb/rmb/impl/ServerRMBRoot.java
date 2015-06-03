package us.yamb.rmb.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Request;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.builders.RequestImpl;
import us.yamb.rmb.impl.builders.SendBuilder;
import us.yamb.rmb.impl.builders.SendImpl;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.TrapListener;
import com.ericsson.research.trap.delegates.OnAccept;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.utils.Configuration;
import com.ericsson.research.trap.utils.UUID;

public class ServerRMBRoot extends RMBRootImpl implements OnData, OnClose, OnError, OnAccept
{
    
    TrapListener                       listener = null;
    TrapEndpoint                       ep       = null;
    Map<String, TrapEndpoint>          clients  = new HashMap<>();
    
    private AsyncResultImpl<Exception> connectionResult;
    
    public ServerRMBRoot(Configuration options)
    {
        super(options);
        this.name = options.getStringOption(ID, "server");
    }
    
    public String id()
    {
        return "/" + name;
    }
    
    public AsyncResult<Exception> connect()
    {
        connectionResult = new AsyncResultImpl<>();
        try
        {
            listener = TrapFactory.createListener(options.toString());
            listener.setDelegate(this, true);
            listener.listen(this);
            connectionResult.completed(null);
        }
        catch (TrapException e)
        {
            connectionResult.errored(null, e);
            connectionResult.completed(e);
        }
        return connectionResult;
    }
    
    public void disconnect()
    {
        listener.close();
    }
    
    public RMBStatus status()
    {
        if (listener == null)
            return RMBStatus.DISCONNECTED;
        else
            return RMBStatus.CONNECTED;
    }
    
    @Override
    public Send message(RMBImpl res)
    {
        return new SendBuilder(res, this);
    }
    
    @Override
    public String seedInfo()
    {
        return "client:" + listener.getClientConfiguration();
    }
    
    @Override
    public Request request(RMBImpl res)
    {
        return new RequestImpl(res, this);
    }
    
    @Override
    public void remove()
    {
        // No effect on root
    }
    
    public String toString()
    {
        return "RMB with ID " + this.id() + ". SeedInfo is [" + this.seedInfo() + "]";
    }
    
    @Override
    public void trapError(TrapEndpoint endpoint, Object context)
    {
        clients.remove(context);
    }
    
    @Override
    public void trapClose(TrapEndpoint endpoint, Object context)
    {
        clients.remove(context);
    }
    
    AtomicInteger channel = new AtomicInteger();
    
    @Override
    public void _trapSend(SendImpl<?> obj, AsyncResultImpl<Reply> replyErrorHandler) throws TrapException
    {
        
        String dst = obj.to().parts[1];
        int ch = channel.getAndAccumulate(1, (a, b) -> ((a + b) % 32) + 2);
        TrapEndpoint client = clients.get(dst);
        
        if (client == null)
            return;
        client.send(obj, ch, true);
    }
    
    @Override
    public void handleControlMessage(RMBTrapMessage msg, TrapEndpoint endpoint, Object context)
    {
        String op = msg.get(RMBTrapMessage.OP_TYPE);
        
        switch (op)
        {
            case RMBTrapMessage.OPERATION_REGISTER:
                
                boolean same = clients.remove(context, endpoint);
                if (!same)
                {
                    endpoint.close();
                    throw new RuntimeException("Race condition: context conflict!");
                }
                
                String id = msg.get(RMBTrapMessage.REQUESTED_ID);
                clients.computeIfPresent(id, (id2, existing) -> {
                    existing.close();
                    return null;
                });
                endpoint.setDelegateContext(id);
                clients.put(id, endpoint);
                
                RMBTrapMessage reply = new RMBTrapMessage();
                reply.put(RMBTrapMessage.OP_TYPE, RMBTrapMessage.OPERATION_REGISTERED);
                reply.put(RMBTrapMessage.APPROVED_ID, id);
                
                try
                {
                    endpoint.send(reply, 1, true);
                }
                catch (TrapException e)
                {
                    clients.remove(id, endpoint);
                    e.printStackTrace();
                }
                
                break;
            
            default:
        }
    }
    
    @Override
    public void incomingTrapConnection(TrapEndpoint endpoint, TrapListener listener, Object context)
    {
        String ctx = UUID.randomUUID();
        endpoint.setDelegate(this, true);
        clients.put(ctx, endpoint);
        endpoint.setDelegateContext(ctx);
    }
    
    public TrapListener getListener()
    {
        return listener;
    }
}
