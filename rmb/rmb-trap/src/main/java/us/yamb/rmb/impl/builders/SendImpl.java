package us.yamb.rmb.impl.builders;

import java.io.IOException;

import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.RMBMessage;
import us.yamb.rmb.impl.RMBRootImpl;

import com.ericsson.research.trap.TrapException;
import com.ericsson.research.trap.TrapObject;

public class SendImpl<T> extends RMBMessage<T> implements TrapObject
{
    
    protected RMBImpl     parent;
    protected RMBRootImpl rmbRoot;
    
    protected SendImpl()
    {
        
    }
    
    public SendImpl(RMBImpl parent, RMBRootImpl rmbRoot)
    {
        this.parent = parent;
        this.rmbRoot = rmbRoot;
    }
    
    public void send() throws IOException
    {
        send(parent, null);
    }
    
    public void send(RMB resp, AsyncResultImpl<Reply> rv) throws IOException
    {
        if (resp != null)
            from(resp.id());
        
        if (to.startsWith(rmbRoot.id()))
        {
            rmbRoot.dispatch(this, 1);
            return;
        }
        
        try
        {
            rmbRoot._trapSend(this, rv);
        }
        catch (TrapException e)
        {
            throw new IOException(e);
        }
    }
    
    @Override
    public byte[] getSerializedData()
    {
        return serialize();
    }
}
