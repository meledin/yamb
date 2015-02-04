package us.yamb.rmb;

import java.io.IOException;

import us.yamb.rmb.builders.RestMessageBuilder;

public interface Response extends RestMessageBuilder<Response>
{
    
    public abstract class ResponseException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        public abstract Response response();
    }
    
    public interface Status
    {
        public static final int OK           = 200;
        public static final int CLIENT_ERROR = 200;
        public static final int SERVER_ERROR = 200;
    }
    
    public Response to(Message request);
    
    public Response status(int status);
    
    public void send(RMB rmb) throws IOException;
    
    /**
     * Throws this response as an exception. RMB should catch it and send it as a response to the other party.
     * 
     * @throws ResponseException
     */
    public void throwException() throws ResponseException;
    
    public static Response ok()
    {
        return create().status(Status.OK);
    }
    
    public static Response create(Message request)
    {
        return create().status(Status.OK).to(request);
    }
    
    public static Response create()
    {
        
        try
        {
            return (Response) Class.forName(Response.class.getCanonicalName() + "Impl").newInstance();
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
