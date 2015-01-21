package us.yamb.rmb;

import java.io.IOException;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;
import us.yamb.rmb.builders.RestMessageBuilder;

public interface Request extends RestMessageBuilder<Request>
{
	
	public interface Response extends Message
	{
		public Exception error();
	}
	
    AsyncResult<Response> execute() throws IOException;
    void execute(AsyncResultCallback<Response> callback) throws IOException;
    
    Request timeout(long msec);
}
