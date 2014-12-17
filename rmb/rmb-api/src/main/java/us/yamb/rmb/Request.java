package us.yamb.rmb;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.builders.RestMessageBuilder;

public interface Request extends RestMessageBuilder<Request>
{
	
	public interface Response extends Message
	{
		public int status();
		public Exception error();
	}
	
    AsyncResult<Response> execute();
}
