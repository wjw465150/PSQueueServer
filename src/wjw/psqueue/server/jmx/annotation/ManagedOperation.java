package wjw.psqueue.server.jmx.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(value = RUNTIME)
@Target(value = { METHOD })
public @interface ManagedOperation {
	String description() default "";
}
