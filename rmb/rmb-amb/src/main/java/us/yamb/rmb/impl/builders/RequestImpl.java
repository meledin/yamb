package us.yamb.rmb.impl.builders;

import java.io.IOException;

import com.ericsson.research.trap.utils.ThreadPool;

import us.yamb.amb.Send;
import us.yamb.amb.spi.AsyncResultImpl;
import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;
import us.yamb.rmb.Location;
import us.yamb.rmb.Message;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request;
import us.yamb.rmb.Request.Response;
import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.RMBMessage;

public class RequestImpl extends SendImpl<Request> implements Request
{
    
    private RMB  rmb;
    private long timeout = 30000;
    
    public RequestImpl(RMBImpl rmb, Send aSend)
    {
        super(rmb, aSend);
        this.rmb = rmb;
    }
    
    @Override
    public AsyncResult<Response> execute() throws IOException
    {
        RMB resp = rmb.create();
        AsyncResultImpl<Response> rv = new AsyncResultImpl<Request.Response>();
        resp.onmessage(msg -> rv.callback(new ResponseImpl(msg)));
        ThreadPool.executeAfter(() -> {
            if (rv.called)
                return;
            
            // What to do if we weren't called?
            rv.callback(new ResponseImpl(new InterruptedException("Timeout exceeded without response")));
            
        }, timeout);
        send(resp);
        return rv;
    }
    
    @Override
    public void execute(AsyncResultCallback<Response> callback) throws IOException
    {
        execute().setCallback(callback);
    }
    
    @Override
    public Request timeout(long msec)
    {
        timeout = msec;
        return this;
    }
    
}

class ResponseImpl implements Response
{
    private Exception err = null;
    private Message   msg;
    
    public ResponseImpl(Exception err)
    {
        this.err = err;
        msg = new RMBMessage<Message>();
    }
    
    public ResponseImpl(Message msg)
    {
        this.msg = msg;
        
    }
    
    @Override
    public Exception error()
    {
        return err;
    }
    
    @Override
    public Location from()
    {
        return msg.from();
    }
    
    @Override
    public Location to()
    {
        return msg.to();
    }
    
    @Override
    public String method()
    {
        return msg.method();
    }
    
    @Override
    public String header(String name)
    {
        return msg.header(name);
    }
    
    @Override
    public byte[] bytes()
    {
        return msg.bytes();
    }
    
    @Override
    public String string()
    {
        return msg.string();
    }
    
    @Override
    public <T> T object(Class<T> baseClass)
    {
        return msg.object(baseClass);
    }
    
    @Override
    public boolean confirmed()
    {
        return msg.confirmed();
    }
    
    @Override
    public long id()
    {
        return msg.id();
    }
    
}
