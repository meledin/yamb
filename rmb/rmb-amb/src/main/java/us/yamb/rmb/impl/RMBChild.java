package us.yamb.rmb.impl;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMB;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Request;
import us.yamb.rmb.Send;

public class RMBChild extends RMBImpl
{
    
    private RMBImpl parent;
    
    public RMBChild(RMBImpl parent, String name)
    {
        this.parent = parent;
        this.name = name;
        
        if ("**".equals(name))
            pathMatcher = arg -> true;
    }
    
    public String id()
    {
        return parent.id() + "/" + name;
    }
    
    public AsyncResult<Exception> connect()
    {
        return parent.connect();
    }
    
    public void disconnect()
    {
        parent.disconnect();
    }
    
    public RMBStatus status()
    {
        return parent.status();
    }
    
    @Override
    public Send message(RMBImpl res)
    {
        return parent.message(res);
    }
    
    @Override
    public String seedInfo()
    {
        return parent.seedInfo();
    }
    
    @Override
    public synchronized RMB create(String id)
    {
        if (id.startsWith("/"))
            return parent.create(id);
        return super.create(id);
    }

    @Override
    public Request request(RMBImpl res)
    {
        return parent.request(res);
    }

    @Override
    public us.yamb.amb.Send _ambSend()
    {
        return parent._ambSend();
    }

	@Override
    public void remove()
    {
		parent.children.remove(name);
    }
    
}
