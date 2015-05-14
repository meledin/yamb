package us.yamb.rmb;

import java.io.IOException;

import us.yamb.rmb.impl.RMBImpl;
import us.yamb.rmb.impl.builders.SendImpl;

public class ResponseImpl extends SendImpl<Response> implements Response
{
    
    private Message request;
    
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
        
        this.msg = ((RMBImpl) rmb)._ambSend();
        
        super.send(null);
         
    }

    @Override
    public void throwException() throws ResponseException
    {
        throw new ResponseException() {
            private static final long serialVersionUID = 1L;

            @Override
            public Response response()
            {
                return ResponseImpl.this;
            }
        };
    }
    
}
