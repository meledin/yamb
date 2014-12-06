package us.yamb.amb.spi;

import us.yamb.amb.callbacks.AsyncResult;

public class AsyncResultImpl<T> implements AsyncResult<T>
{

	private AsyncResultCallback<T>	callback;
	private T	                   value	= null;
	private boolean	               called	= false;

	public AsyncResultImpl<T> callback(T value)
	{
		AsyncResultCallback<T> cb;
		synchronized (this)
		{
			if (called)
				return this; // Do nothing if we already were called

			this.value = value;
			called = true;

			if (this.callback == null)
			{
				this.notifyAll();
				return this;
			}

			cb = this.callback;
		}
		cb.completed(value);
		return this;
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

}
