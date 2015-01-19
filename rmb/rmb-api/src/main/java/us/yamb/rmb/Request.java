package us.yamb.rmb;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;
import us.yamb.rmb.builders.RestMessageBuilder;

public interface Request extends RestMessageBuilder<Request>
{
	
	public interface Response extends Message
	{
		public Exception error();
	}
	
    AsyncResult<Response> execute();
    void execute(AsyncResultCallback<Response> callback);
    
    Request timeout(long msec);
}
