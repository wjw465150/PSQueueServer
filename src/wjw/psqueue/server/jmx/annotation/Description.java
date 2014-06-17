package wjw.psqueue.server.jmx.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(value = RUNTIME)
@Target(value = { CONSTRUCTOR, FIELD, METHOD, PARAMETER, TYPE })
public @interface Description {
	String name() default "";

	String description() default "";
}
