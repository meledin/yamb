package us.yamb.amb.spi;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MultiListenerAsyncResult<T> extends AsyncResultImpl<T>
{
    
    ConcurrentLinkedQueue<AsyncResultCallback<T>> callbacks = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<AsyncErrorCallback>     errors    = new ConcurrentLinkedQueue<>();
    
    public void completed(T value)
    {
        boolean called;
        synchronized (this)
        {
            called = !isDone();
            super.completed(value);
        }
        if (called)
        {
            callbacks.forEach((cb) -> {
                try
                {
                    cb.completed(value);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        }
    }
    
    public void errored(String reason, Exception e)
    {
        boolean called;
        synchronized (this)
        {
            called = !isDone();
            super.errored(reason, e);
        }
        
        if (called)
        {
            errors.forEach((errorCallback) -> errorCallback.errored(error.reason(), error.exception()));
        }
    }
    
    public void setCallback(AsyncResultCallback<T> callback)
    {
        T val;
        synchronized (this)
        {
            
            if (!isDone())
            {
                callbacks.add(callback);
                return;
            }
            
            // Retrieve the results outside of synchronized to prevent
            // deadlocks.
            val = this.value;
        }
        try
        {
            callback.completed(val);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    public void setCallback(AsyncResultCallback<T> successCallback, AsyncErrorCallback errorCallback)
    {
        T val;
        AsyncError err;
        synchronized (this)
        {
            
            if (!isDone())
            {
                callbacks.add(successCallback);
                errors.add(errorCallback);
                return;
            }
            
            err = this.error;
            val = this.value;
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
    
    public void setPrimaryCallback(AsyncResultCallback<T> successCallback, AsyncErrorCallback errorCallback)
    {
        super.setCallback(successCallback, errorCallback);
    }
    
}
