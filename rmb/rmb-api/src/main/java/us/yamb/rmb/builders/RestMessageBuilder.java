package us.yamb.rmb.builders;

import us.yamb.mb.builders.MBMessageBuilder;
import us.yamb.rmb.Location;

/**
 * This is a base interface that allows multiple builders to share the same methods.
 * @author Vladimir Katardjiev
 *
 * @param <T>
 */
public interface RestMessageBuilder<T> extends MBMessageBuilder<T>
{
	/**
	 * The method to use for this message
	 * @param method A string representing the method
	 * @return The same builder, to be used for chaining
	 */
	T method(String method);
	
	/**
	 * The recipient to use for this message
	 * @param to A Location for the recipient
	 * @return The same builder, to be used for chaining
	 */
	T to(Location to);

	/**
	 * The status code to associate with this message
	 * @param status A non-negative integer
	 * @return The same builder, to be used for chaining
	 */
    T status(int status);
    
    /**
     * Sets a header on the message. Headers are completely optional.
     * @param name The header name to set
     * @param value The value to set
     * @return The same builder, to be used for chaining
     */
    T header(String name, String value);
}
