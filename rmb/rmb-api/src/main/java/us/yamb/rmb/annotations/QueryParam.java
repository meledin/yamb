package us.yamb.rmb.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as to be parsed from the query. The query must contain an equivalent {param_value} element.
 * 
 * @author Vladimir Katardjiev
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER })
public @interface QueryParam
{
    public abstract String name() default "";
    public abstract String value() default "";
}
