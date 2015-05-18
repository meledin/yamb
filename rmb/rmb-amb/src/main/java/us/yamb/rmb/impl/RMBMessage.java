package us.yamb.rmb.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import us.yamb.mb.util.JSON;
import us.yamb.mb.util.JSONSerializable;
import us.yamb.rmb.Location;
import us.yamb.rmb.Message;

import com.ericsson.research.trap.utils.StringUtil;

public class RMBMessage<T> extends PackedMessage<T> implements Message, JSONSerializable
{
    
    public String  from;
    public String  to;
    public String  method = "POST";
    public boolean confirmed;
    public long    id;
    private int    status;
    
    @Override
    public Collection<String> getFieldNames()
    {
        return Arrays.asList("from", "to", "method", "headers", "data", "confirmed", "status");
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
        return customHeaders.get(name);
    }
    
    @Override
    public String string()
    {
        return StringUtil.toUtfString(bytes());
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
        this._header(Header.From, from);
        this._header(Header.To, to);
        this._header(Header.Method, method);
        this._header(Header.Confirmed, Boolean.toString(confirmed));
        this._header(Header.Id, Long.toString(id));
        this._header(Header.Status, Integer.toString(status));
        
        try
        {
            return pack();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static RMBMessage<?> deserialize(byte[] src)
    {
        try
        {
            RMBMessage<?> msg = PackedMessage.unpack(src, new RMBMessage<>());
            msg.from = msg.headers.get(Header.From);
            msg.to = msg.headers.get(Header.To);
            msg.method = msg.headers.get(Header.Method);
            msg.confirmed = Boolean.valueOf(msg.headers.get(Header.Confirmed));
            msg.id = Long.parseLong(msg.headers.get(Header.Id));
            msg.status = Integer.parseInt(msg.headers.get(Header.Status));
            return msg;
        }
        catch (RMBException e)
        {
            throw new RuntimeException(e);
        }
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
    public T data(String data)
    {
        if (data != null)
            super.data(StringUtil.toUtfBytes(data));
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T data(Object data)
    {
        if (data == null)
            super.data((byte[])null);
        else if (String.class.isAssignableFrom(data.getClass()))
            data((String) data);
        else if (byte[].class.isAssignableFrom(data.getClass()))
            super.data((byte[]) data);
        else
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
    
    @SuppressWarnings("unchecked")
    public T header(String name, String value)
    {
        customHeaders.put(name, value);
        return (T) this;
    }
    
    /**
     * Converts a message to a human readable representation.
     */
    public String toString()
    {
        StringBuffer out = new StringBuffer("[ ");
        out.append(method);
        
        if (status() > 0)
        {
            out.append("[");
            out.append(status());
            out.append("]");
        }
        
        out.append(" ");
        out.append(from);
        out.append(" -> ");
        out.append(to);
        out.append("\n");
        for (Iterator<String> it = this.customHeaders.values().iterator(); it.hasNext();)
        {
            String key = it.next();
            out.append(key);
            out.append(": ");
            out.append(this.customHeaders.get(key));
            out.append("\r\n");
        }
        boolean binary = false;
        if (this.bytes() != null)
        {
            for (int i = 0; i < this.bytes().length; i++)
            {
                if ((this.bytes()[i] < 32) && (this.bytes()[i] != 9) && (this.bytes()[i] != 10) && (this.bytes()[i] != 13))
                {
                    binary = true;
                    break;
                }
            }
            if (!binary)
            {
                out.append("\r\n");
                
                // (May 10, 10) modified for Android
                // out.append(new String(data, Charset.forName("UTF-8")));
                try
                {
                    out.append(string());
                }
                catch (Exception e)
                {
                    
                }
            }
        }
        out.append("\n]  ");
        out.append(Integer.toString(out.length() - 21));
        out.append(" bytes with ");
        if (this.bytes() != null)
        {
            out.append(this.bytes().length);
            if (binary)
                out.append(" bytes of binary data in the body");
            else
                out.append(" bytes in the body");
        }
        else
            out.append("no data");
        return out.toString();
    }
    
    @Override
    public Map<String, String> headers()
    {
        return customHeaders;
    }
}
