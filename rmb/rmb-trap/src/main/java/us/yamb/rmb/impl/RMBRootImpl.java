package us.yamb.rmb.impl;

import java.io.IOException;

import com.ericsson.research.trap.TrapEndpoint;
import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.delegates.OnData;
import com.ericsson.research.trap.utils.Configuration;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.util.YContext;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.Send;
import us.yamb.rmb.impl.builders.RequestImpl;
import us.yamb.rmb.impl.builders.SendBuilder;
import us.yamb.rmb.impl.builders.SendImpl;

public abstract class RMBRootImpl extends RMBRoot implements OnData
{
    public static final String TRAPCFG = "TRAPCFG";
    public static final String ID      = "ID";
    public final Configuration options;
    
    public RMBRootImpl(Configuration options)
    {
        this.options = options;
    }
    
    public String id()
    {
        return "/" + name;
    }
    
    @Override
    public Send message(RMBImpl res)
    {
        return new SendBuilder(res, this);
    }
    
    @Override
    public Request request(RMBImpl res)
    {
        return new RequestImpl(res, this);
    }
    
    public abstract void _trapSend(SendImpl<?> obj, AsyncResultImpl<Reply> rv) throws TrapException;
    
    @Override
    public void remove()
    {
        // No effect on root
    }
    
    @Override
    public void trapData(byte[] data, int channel, TrapEndpoint endpoint, Object context)
    {
        
        if (channel == 1)
        {
            RMBTrapMessage msg = RMBTrapMessage.parse(data);
            handleControlMessage(msg, endpoint, context);
            return;
        }
        
        RMBMessage<?> msg = RMBMessage.deserialize(data);
        YContext.push(RMB.CTX_MESSAGE, msg);
        try
        {
            if (!dispatch(msg, 1))
            {
                try
                {
                    if (msg.status() < 100)
                        message().to(msg.from()).status(404).data(msg.to + " not found").send();
                    else if (msg.status() >= 300)
                        System.err.println(msg);
                }
                catch (IOException e)
                {
                }
            }
        }
        catch (Exception e)
        {
            
            try
            {
                e.printStackTrace();
                message().to(msg.from()).status(500).data(e.getMessage()).send();
            }
            catch (IOException e1)
            {
            }
        }
        finally
        {
            YContext.pop(RMB.CTX_MESSAGE, msg);
        }
    }
    
    public abstract void handleControlMessage(RMBTrapMessage msg, TrapEndpoint endpoint, Object context);
    
    public String getJsonifiable()
    {
        return "TBD";
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
    
}
