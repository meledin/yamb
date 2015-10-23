package us.yamb.amb.spi;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.mb.callbacks.AsyncResult.AsyncErrorCallback;
import us.yamb.mb.callbacks.AsyncResult.AsyncResultCallback;

public class AsyncResultImpl<T> implements AsyncResult<T>, AsyncResultCallback<T>, AsyncErrorCallback
{
    
    public class AsyncError
    {
        
        private Exception e;
        private String    reason;
        
        public AsyncError(String reason, Exception e)
        {
            this.reason = reason;
            this.e = e;
        }
        
        public Exception exception()
        {
            return e;
        }
        
        public String reason()
        {
            return reason;
        }
        
    }
    
    private boolean                called = false;
    private AsyncResultCallback<T> callback;
    T                              value  = null;
    AsyncResultImpl<T>.AsyncError  error;
    private AsyncErrorCallback     errorCallback;
    
    public boolean isDone()
    {
        return called;
    }
    
    public void completed(T value)
    {
        AsyncResultCallback<T> cb;
        synchronized (this)
        {
            if (called)
                return; // Do nothing if we already were called
                
            this.value = value;
            called = true;
            
            this.notifyAll();
            cb = this.callback;
        }
        if (cb != null)
            try
            {
                cb.completed(value);
            }
            catch (Exception e)
            {
                if (errorCallback != null)
                    errorCallback.errored(e.getMessage(), e);
                else
                    throw new RuntimeException(e);
            }
    }
    
    public void errored(String reason, Exception e)
    {
        AsyncErrorCallback cb;
        synchronized (this)
        {
            if (called)
                return; // Do nothing if we already were called
                
            this.error = new AsyncError(reason, e);
            called = true;
            
            this.notifyAll();
            cb = this.errorCallback;
        }
        if (cb != null)
            cb.errored(error.reason(), error.exception());
    }
    
    public T get() throws InterruptedException, AsyncResultException
    {
        return this.get(Long.MAX_VALUE);
    }
    
    public T get(long timeout) throws InterruptedException, AsyncResultException
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
            
            if (this.error != null)
                throw new AsyncResultException(error.reason(), error.exception());
            
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
        try
        {
            cb.completed(val);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static <T> AsyncResultImpl<T> wrap(T o)
    {
        AsyncResultImpl<T> res = new AsyncResultImpl<T>();
        res.completed(o);
        return res;
    }
    
    @Override
    public void setCallback(AsyncResultCallback<T> successCallback, AsyncErrorCallback errorCallback)
    {
        T val;
        AsyncError err;
        synchronized (this)
        {
            this.callback = successCallback;
            this.errorCallback = errorCallback;
            
            if (!called)
                return;
            
            // Retrieve the results outside of synchronized to prevent
            // deadlocks.
            err = this.error;
            val = this.value;
            successCallback = callback;
            errorCallback = this.errorCallback;
        }
        
        if (err != null)
        {
            errorCallback.errored(err.reason(), err.exception());
            return;
        }
        
        try
        {
            successCallback.completed(val);
        }
        catch (Exception e)
        {
            errorCallback.errored(e.getMessage(), e);
        }
    }
    
}
