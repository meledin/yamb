package us.yamb.amb.spi;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;

public class AsyncResultImpl<T> implements AsyncResult<T>, AsyncResultCallback<T>
{

	private AsyncResultCallback<T>	callback;
	private T	                   value	= null;
	public boolean	               called	= false;

	public void completed(T value)
	{
		AsyncResultCallback<T> cb;
		synchronized (this)
		{
			if (called)
				return; // Do nothing if we already were called

			this.value = value;
			called = true;

			if (this.callback == null)
			{
				this.notifyAll();
				return;
			}

			cb = this.callback;
		}
		cb.completed(value);
		return;
	}

	public T get() throws InterruptedException
	{
		return this.get(Long.MAX_VALUE);
	}

	public T get(long timeout) throws InterruptedException
	{
		long start = System.currentTimeMillis();
		long elapsed;

		synchronized (this)
		{
			while (!called)
			{
				elapsed = System.currentTimeMillis() - start;
				long remaining = timeout - elapsed;

				if (remaining <= 0)
					break;

				this.wait(remaining);
			}
			
			if (!called)
			    throw new InterruptedException("Time limit exceeded while waiting for result");

			return this.value;
		}

	}

	public void setCallback(AsyncResultCallback<T> callback)
	{
		T val;
		AsyncResultCallback<T> cb;
		synchronized (this)
		{
			this.callback = callback;

			if (!called)
				return;

			// Retrieve the results outside of synchronized to prevent
			// deadlocks.
			val = this.value;
			cb = callback;
		}
		cb.completed(val);
	}
	
	public static <T> AsyncResultImpl<T> wrap(T o) {
	    AsyncResultImpl<T> res = new AsyncResultImpl<T>();
	    res.completed(o);
	    return res;
	}

}
