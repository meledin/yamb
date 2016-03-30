package us.yamb.rmb.builders;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.Pipe;

public interface PipeBuilder extends RestMessageBuilder<PipeBuilder>
{
	public class PipeResult
	{
		enum Status {
			OK, ERROR
		}
		
		public Status status;
		public Pipe pipe;
		public Exception e;
	}
	
	/**
	 * Attempts to build a pipe. <b>This is an asynchronous operation</b>, which may take several seconds to finish.
	 * @return An {@link AsyncResult} for a pipe. Poll or set a callback, and test the result to see the outcome.
	 */
	AsyncResult<PipeResult> build();
}
