package us.yamb.rmb;

import java.io.IOException;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;
import us.yamb.rmb.builders.RestMessageBuilder;

/**
 * Build a request when a unique answer is expected for this outgoing message. A Request builder will create a unique RMB
 * instance and use that as the {@link Message#from()} identifier of the sent message.
 * 
 * @author vladi
 */
public interface Request extends RestMessageBuilder<Request>
{
    
    public interface Reply extends Message
    {
        public Exception error();
    }
    
    AsyncResult<Reply> execute() throws IOException;
    
    void execute(AsyncResultCallback<Reply> callback) throws IOException;
    
    /**
     * Performs an asynchronous request. Only one of the two supplied callbacks will ever be called: <i>callback</i> on reply,
     * or <i>exceptionHandler</i> if an exception prevents the request from being sent.
     * 
     * @param callback
     *            A callback to be called upon completion.
     * @param exceptionHandler
     *            A callback to be called if an exception occurs during the send operation.
     */
    void execute(AsyncResultCallback<Reply> callback, AsyncResultCallback<IOException> exceptionHandler);
    
    void send() throws IOException;
    
    Request timeout(long msec);
}
