package us.yamb.rmb.impl;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ericsson.research.trap.TrapClient;
import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapFactory;
import com.ericsson.research.trap.delegates.OnClose;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.delegates.OnError;
import com.ericsson.research.trap.delegates.OnOpen;
import com.ericsson.research.trap.utils.Configuration;
import com.ericsson.research.trap.utils.UUID;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Request;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.builders.RequestImpl;
import us.yamb.rmb.impl.builders.SendBuilder;
import us.yamb.rmb.impl.builders.SendImpl;

public class ClientRMBRoot extends RMBRootImpl implements OnData, OnOpen, OnClose, OnError
{
    TrapClient                                  client           = null;
    private AsyncResultImpl<Exception>          connectionResult = new AsyncResultImpl<>();
    private Map<AsyncResultImpl<Reply>, Object> reqErrorHandlers;
    
    public ClientRMBRoot(Configuration options)
    {
        super(options);
        this.name = options.getStringOption(ID, UUID.randomUUID());
        reqErrorHandlers = Collections.synchronizedMap(new WeakHashMap<>());
    }
    
    public String id()
    {
        return "/" + name;
    }
    
    public AsyncResult<Exception> connect()
    {
        try
        {
            client = TrapFactory.createClient(options.toString(), true);
            client.setReconnectTimeout(1000);
            client.setDelegate(this, true);
            client.disableTransport("http");
            client.open();
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
        client.close();
    }
    
    public RMBStatus status()
    {
        switch (client.getState())
        {
            case OPEN:
                return RMBStatus.CONNECTED;
            case OPENING:
                return RMBStatus.CONNECTING;
            case ERROR:
            case CLOSED:
            case CLOSING:
            default:
                return RMBStatus.DISCONNECTED;
                
        }
    }
    
    @Override
    public Send message(RMBImpl res)
    {
        return new SendBuilder(res, this);
    }
    
    @Override
    public String seedInfo()
    {
        return "client:" + options.toString();
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
        return "RMB with ID " + this.id() + " with status " + client.getState() + ". SeedInfo is [" + this.seedInfo() + "]";
    }
    
    @Override
    public void trapError(TrapEndpoint endpoint, Object context)
    {
        connectionResult.errored("Connection errored", null);
        reqErrorHandlers.keySet().forEach((asyncObject) -> asyncObject.errored("Connection closed before reply received", new FileNotFoundException()));
        if (this.ondisconnect != null)
            this.ondisconnect.ondisconnect();
    }
    
    @Override
    public void trapClose(TrapEndpoint endpoint, Object context)
    {
        System.err.println("TRAP CLOSED");
        connectionResult.errored("Connection closed unexpectedly", null);
        reqErrorHandlers.keySet().forEach((asyncObject) -> asyncObject.errored("Connection closed before reply received", new FileNotFoundException()));
        if (this.ondisconnect != null)
            this.ondisconnect.ondisconnect();
    }
    
    @Override
    public void trapOpen(TrapEndpoint endpoint, Object context)
    {
        // Send a control message
        RMBTrapMessage msg = new RMBTrapMessage();
        msg.put(RMBTrapMessage.OP_TYPE, RMBTrapMessage.OPERATION_REGISTER);
        msg.put(RMBTrapMessage.REQUESTED_ID, name);
        try
        {
            client.send(msg, 1, false);
        }
        catch (TrapException e)
        {
            connectionResult.errored("Connection closed unexpectedly", null);
        }
    }
    
    AtomicInteger channel = new AtomicInteger();
    
    @Override
    public void _trapSend(SendImpl<?> obj, AsyncResultImpl<Reply> rv) throws TrapException
    {
        reqErrorHandlers.put(rv, null);
        int ch = channel.getAndAccumulate(1, (a, b) -> ((a + b) % 32) + 2);
        client.send(obj, ch, false);
    }
    
    public void handleControlMessage(RMBTrapMessage msg, TrapEndpoint endpoint, Object context)
    {
        
        String op = msg.get(RMBTrapMessage.OP_TYPE);
        
        switch (op)
        {
            case RMBTrapMessage.OPERATION_REGISTERED:
                // We've been registered! Yay!
                this.name = msg.get(RMBTrapMessage.APPROVED_ID);
                connectionResult.completed(null);
                break;
            
            default:
        }
        
    }
}
