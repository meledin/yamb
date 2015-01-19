package us.yamb.rmb.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import us.yamb.mb.util.JSON;
import us.yamb.mb.util.JSONSerializable;
import us.yamb.rmb.Location;
import us.yamb.rmb.Message;

import com.ericsson.research.trap.utils.StringUtil;

public class RMBMessage<T> implements Message, JSONSerializable
{
    
    public String                  from;
    public String                  to;
    public String                  method = "POST";
    public HashMap<String, String> headers;
    public byte[]                  data;
    public boolean                 confirmed;
    public long                    id;
    private int                    status;
    
    @Override
    public Collection<String> getFieldNames()
    {
        return Arrays.asList("from", "to", "method", "headers", "data", "confirmed");
    }
    
    @Override
    public Location from()
    {
        return new Location(from);
    }
    
    @Override
    public Location to()
    {
        return new Location(to);
    }
    
    @Override
    public String method()
    {
        return method;
    }
    
    @Override
    public String header(String name)
    {
        return headers.get(name);
    }
    
    @Override
    public byte[] bytes()
    {
        return data;
    }
    
    @Override
    public String string()
    {
        return StringUtil.toUtfString(data);
    }
    
    @Override
    public <S> S object(Class<S> baseClass)
    {
        return JSON.fromJSON(string(), baseClass);
    }
    
    @Override
    public boolean confirmed()
    {
        return confirmed;
    }
    
    @Override
    public long id()
    {
        return id;
    }
    
    public byte[] serialize()
    {
        return StringUtil.toUtfBytes(JSON.toJSON(this, "class"));
    }
    
    public static RMBMessage<?> deserialize(byte[] src)
    {
        return JSON.fromJSON(StringUtil.toUtfString(src), RMBMessage.class);
    }
    
    @SuppressWarnings("unchecked")
    public T method(String method)
    {
        this.method = method;
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T to(Location to)
    {
        this.to = to.toString();
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T to(long id)
    {
        this.id = id;
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T data(byte[] data)
    {
        this.data = data;
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T data(String data)
    {
        data(StringUtil.toUtfBytes(data));
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T data(Object data)
    {
        data(JSON.toJSON(data));
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T confirmed(boolean confirmed)
    {
        this.confirmed = confirmed;
        return (T) this;
    }
    
    protected void from(String id)
    {
        from(new Location(id));
    }
    
    protected void from(Location path)
    {
        this.from = path.toString();
    }
    
    public int status()
    {
        return status;
    }
    
    @SuppressWarnings("unchecked")
    public T status(int status)
    {
        this.status = status;
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T to(String id)
    {
        to(new Location(id));
        return (T) this;
    }
}
