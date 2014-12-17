package us.yamb.rmb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as to be parsed from the path. The path must contain an equivalent {param_value} element.
 * 
 * @author Vladimir Katardjiev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER })
public @interface PathParam
{
    public abstract String value();
}
