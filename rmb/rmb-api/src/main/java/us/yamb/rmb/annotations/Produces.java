package us.yamb.rmb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the content type on the (eventual) response message. If a return value is supplied by the method, the Produces
 * annotation will be read to determine the Content Type of the created message.
 * 
 * @author Vladimir Katardjiev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Produces
{
    public abstract String value();
}
