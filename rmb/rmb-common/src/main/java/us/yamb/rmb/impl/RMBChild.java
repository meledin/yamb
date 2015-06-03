package us.yamb.rmb.impl;

import java.util.regex.Pattern;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMB;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.Request;
import us.yamb.rmb.Send;

public class RMBChild extends RMBImpl
{
    
    private RMBImpl parent;
    
    public RMBChild(RMBImpl parent, String name, boolean regexp)
    {
        this.parent = parent;
        this.name = name;
        
        if (regexp)
        {
            Pattern pattern = Pattern.compile(name);
            pathMatcher = arg -> pattern.matcher(arg).matches();
        }
        else if ("**".equals(name))
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
    public synchronized RMB create(String id, boolean regexp)
    {
        if (id.startsWith("/"))
            return parent.create(id, regexp);
        return super.create(id, regexp);
    }
    
    @Override
    public Request request(RMBImpl res)
    {
        
        if (res == this && this.name.startsWith("("))
            res = parent;
        
        return parent.request(res);
    }
    
    @Override
    public void remove()
    {
        parent.children.remove(name);
    }
    
    public RMBRoot root()
    {
        return parent.root();
    }
    
}
