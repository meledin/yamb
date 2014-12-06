package us.yamb.amb.callbacks;

public interface AsyncResult<T>
{

	/**
	 * Asynchronous callback interface. Listeners should implement this
	 * interface (usually in an anonymous inner class) to receive the results of
	 * the callback.
	 * 
	 * @author Vladimir Katardjiev
	 * @param <T>
	 *            The type of object being provided by the callback
	 */
	public interface AsyncResultCallback<T>
	{
		/**
		 * Called to receive the callback value.
		 * 
		 * @param result
		 *            The callback value
		 */
		void completed(T arg);
	}

	/**
	 * Synchronous accessor for the completed value. Should not be used together
	 * with {@link #setCallback(AsyncResultCallback)}.
	 * 
	 * @return The callback value, synchronously
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting for the callback.
	 */
	public T get() throws InterruptedException;

	/**
	 * Like {@link #get()} but with a limited timer.
	 * 
	 * @param timeout
	 *            The maximum time to wait.
	 * @return The callback value, synchronously.
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting for the callback.
	 */
	public T get(long timeout) throws InterruptedException;

	/**
	 * Sets the function that will receive the callback. The function will be
	 * called as soon as the callback value is ready.
	 * 
	 * @param callback
	 *            The function to call
	 */
	public void setCallback(AsyncResultCallback<T> callback);

}
