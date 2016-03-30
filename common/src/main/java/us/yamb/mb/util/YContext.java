package us.yamb.mb.util;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a generic context class that can be used to attach context to the current thread. Care should be taken to properly
 * clear the context once out of scope. The context should not be assumed to survive a thread break.
 * 
 * @author vladi
 */
public class YContext
{
    private static final ThreadLocal<ConcurrentHashMap<String, LinkedList<Object>>> ctx = new ThreadLocal<ConcurrentHashMap<String, LinkedList<Object>>>() {
                                                                                            
                                                                                            @Override
                                                                                            protected ConcurrentHashMap<String, LinkedList<Object>> initialValue()
                                                                                            {
                                                                                                return new ConcurrentHashMap<String, LinkedList<Object>>();
                                                                                            }
                                                                                            
                                                                                        };
    
    private static LinkedList<Object> _list(String key)
    {
        return ctx.get().computeIfAbsent(key, (foo) -> new LinkedList<Object>());
    }
    
    /**
     * Fetches the object at the top of the stack, trying to cast it to the preferred type (if given). Note that if the types do
     * not match, a {@link ClassCastException} will be thrown.
     * 
     * @param key
     *            The key to the stack to peek
     * @return The value, if any, or <i>null</i>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T object(String key)
    {
        return (T) _list(key).peek();
    }
    
    /**
     * Fetches the string representation of the object at the top of the stack, using {@link Object#toString()} to access it as
     * a string.
     * 
     * @param key
     *            The key to the stack to peek
     * @return The string representation of the value, if any, or <i>null</i>.
     */
    public static String string(String key)
    {
        Object value = object(key);
        if (value == null)
            return null;
        return value.toString();
    }
    
    /**
     * Pushes a value onto the context stack. This call MUST be paired with a call to {@link #pop(String, Object)}, otherwise
     * the context will become corrupted. For example:
     * 
     * <pre>
     * try
     * {
     *     YContext.push(&quot;foo&quot;, &quot;bar&quot;);
     * }
     * finally
     * {
     *     YContext.pop(&quot;foo&quot;, &quot;bar&quot;);
     * }
     * </pre>
     * 
     * @param key
     *            The key whose stack to push to
     * @param value
     *            The value to push onto the context stack.
     */
    public static void push(String key, Object value)
    {
        _list(key).push(value);
    }
    
    /**
     * Pops a value from the object stack <b>if and only if</b> it matches the expected value, otherwise <b>throws a runtime
     * exception</b>. This is a precautionary measure to prevent stack-corruption.
     * 
     * @param key
     *            The key whose stack to pop
     * @param expectedValue
     *            The value that should be located on that point.
     * @return The expected value, for chaining purposes.
     * @throws IllegalArgumentException
     *             If the expected value does not match the top of the stack, this exception is thrown to try and shut down the
     *             current thread.
     */
    public static <T> T pop(String key, T expectedValue)
    {
        Object object = object(key);
        if (expectedValue != null && expectedValue.equals(object) || expectedValue == object)
        {
            _list(key).pop();
            return expectedValue;
        }
        _list(key).clear();
        throw new IllegalArgumentException("State corruption found! Expected value " + expectedValue + " does not match actual value " + object + ". Throwing to prevent further thread corruption. This may cause further errors");
    }
}
