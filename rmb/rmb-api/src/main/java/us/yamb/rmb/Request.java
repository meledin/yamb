package us.yamb.rmb;

import java.io.IOException;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;
import us.yamb.rmb.builders.RestMessageBuilder;

public interface Request extends RestMessageBuilder<Request>
{

	public interface Reply extends Message
	{
		public Exception error();
	}

	AsyncResult<Reply> execute() throws IOException;

	void execute(AsyncResultCallback<Reply> callback) throws IOException;
	
	void send() throws IOException;

	Request timeout(long msec);
}
