package us.yamb.mb.callbacks;

public interface AsyncResult<T>
{
    
    /**
     * Asynchronous callback interface. Listeners should implement this interface (usually in an anonymous inner class) to
     * receive the results of the callback.
     * 
     * @author Vladimir Katardjiev
     * @param <T>
     *            The type of object being provided by the callback
     */
    @FunctionalInterface
    public interface AsyncResultCallback<T>
    {
        /**
         * Called to receive the callback value.
         * 
         * @param result
         *            The callback value
         */
        void completed(T arg) throws Exception;
    }
    
    public interface AsyncErrorCallback
    {
        void errored(String reason, Exception ex);
    }
    
    /**
     * Synchronous accessor for the completed value. Should not be used together with {@link #setCallback(AsyncResultCallback)}.
     * 
     * @return The callback value, synchronously
     * @throws InterruptedException
     *             If the thread is interrupted while waiting for the callback.
     */
    public T get() throws InterruptedException, AsyncResultException;
    
    /**
     * Like {@link #get()} but with a limited timer.
     * 
     * @param timeout
     *            The maximum time to wait.
     * @return The callback value, synchronously.
     * @throws InterruptedException
     *             If the thread is interrupted while waiting for the callback.
     */
    public T get(long timeout) throws InterruptedException, AsyncResultException;
    
    /**
     * Sets the function that will receive the callback. The function will be called as soon as the callback value is ready.
     * 
     * @param callback
     *            The function to call
     * @deprecated
     */
    public void setCallback(AsyncResultCallback<T> callback);
    
    /**
     * Sets the function that will receive the callback. The function will be called as soon as the callback value is ready.
     * 
     * @param callback
     *            The function to call
     */
    public void setCallback(AsyncResultCallback<T> successCallback, AsyncErrorCallback errorCallback);
    
    public boolean isDone();
    
    public class AsyncResultException extends Exception
    {
        
        private static final long serialVersionUID = 1L;
        
        public AsyncResultException(String reason, Exception ex)
        {
            super(reason, ex);
        }
        
    }
    
}
