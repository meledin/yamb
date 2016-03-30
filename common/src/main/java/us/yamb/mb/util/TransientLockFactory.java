package us.yamb.mb.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Transient lock factories provide lock objects for certain <i>keys</i> that themselves cannot constitute a lock. For example,
 * a transient lock factory can produce locks in response to <code>String</code> objects, and these locks will fulfill the
 * general contract for locks.
 * <p>
 * Transient lock factories produce <i>weak</i> lock objects, which may be garbage collected when not in use. This gives fairly
 * bad performance characteristics for recurring tasks, as the lock may end up being recreated on every invocation, resulting in
 * a significant performance and concurrency penalty.
 * <p>
 * The exact performance characteristics of a TransientLockFactory depend on the garbage collector.
 * 
 * @param <T>
 *            The type of key to lock on
 */
public class TransientLockFactory<T>
{
    private final ConcurrentHashMap<T, WeakLock> locks = new ConcurrentHashMap<T, WeakLock>();
    
    /**
     * Acquires a lock object for a given key. The lock is guaranteed to be unique for the given key, such that at any given
     * time, if two concurrently executing threads call getLock(), they will receive the same object.
     * <p>
     * Transient lock objects are <i>not</i> guaranteed to be unique between calls. If all references to a transient lock are
     * nulled, and getLock() is called, it may generate a new lock object as needed.
     * 
     * @param key
     * @return The lock object.
     */
    public Object getLock(T key)
    {
        
        AtomicReference<Object> ref = new AtomicReference<>();
        
        locks.compute(key, (k, existing) -> {
            if (existing != null)
            {
                Object o = existing.get();
                if (o != null)
                {
                    ref.set(o);
                    return existing;
                }
            }
            Object o = new Object();
            ref.set(o);
            return new WeakLock(o, key);
        });
        
        return ref.get();
    }
    
    /**
     * Immediately removes a lock from the TransientLockFactory. Calling this is generally not required, as the locks will be
     * garbage collected when not in use.
     * 
     * @param key
     *            The key whose lock to remove
     */
    public void freeLock(T key)
    {
        this.locks.remove(key);
    }
    
    class WeakLock extends WeakReference<Object>
    {
        
        final T key;
        
        public WeakLock(Object lock, T key)
        {
            super(lock);
            this.key = key;
        }
        
        /*
         * Removes the WeakLock object from the main HashMap, thus allowing garbage collection
         * (non-Javadoc)
         * @see java.lang.Object#finalize()
         */
        @Override
        protected void finalize() throws Throwable
        {
            super.finalize();
            TransientLockFactory.this.freeLock(this.key);
        }
        
    }
    
}