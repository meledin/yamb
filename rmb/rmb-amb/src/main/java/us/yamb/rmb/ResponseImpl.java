package us.yamb.rmb;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import us.yamb.mb.util.YContext;
import us.yamb.rmb.Response.ResponseException;
import us.yamb.rmb.impl.RMBChild;
import us.yamb.rmb.impl.RMBRootImpl;
import us.yamb.rmb.impl.builders.SendImpl;

public class ResponseImpl extends SendImpl<Response> implements Response
{
    
    private Message request;
    
    public ResponseImpl()
    {
        Message msg = YContext.object(RMB.CTX_MESSAGE);
        
        if (msg != null)
        {
            to(msg.from());
            from(msg.to());
        }
    }
    
    @Override
    public Response to(Message request)
    {
        this.request = request;
        to(request.from());
        return this;
    }
    
    @Override
    public void send(RMB rmb) throws IOException
    {
        if (request != null)
            from(request.to());
        else
            from(rmb.id());
        
        if (rmb instanceof RMBChild)
            rmb = ((RMBChild) rmb).root();
        
        this.msg = ((RMBRootImpl) rmb)._ambSend();
        
        super.send(null);
        
    }
    
    @Override
    public void throwException() throws ResponseException
    {
        throw new ResponseExceptionImpl(this);
    }
    
    @Override
    public void throwException(Object probableCause) throws ResponseException
    {
        throw new ResponseExceptionImpl(this);
    }
    
}

class ResponseExceptionImpl extends ResponseException
{
    private ResponseImpl resp;
    
    public ResponseExceptionImpl(ResponseImpl resp)
    {
        this.resp = resp;
    }
    
    private static final long serialVersionUID = 1L;
    
    @Override
    public Response response()
    {
        return resp;
    }
    
    public void printStackTrace(PrintStream s)
    {
        super.printStackTrace(s);
        
        if (getCause() == null)
        {
            s.append("Caused by:\n");
            s.append(resp.toString());
        }
    }
    
    public void printStackTrace(PrintWriter s)
    {
        super.printStackTrace(s);
        
        if (getCause() == null)
        {
            s.append("Caused by:\n");
            s.append(resp.toString());
        }
    }
    
}
