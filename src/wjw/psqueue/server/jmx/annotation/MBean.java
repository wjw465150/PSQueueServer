package wjw.psqueue.server.jmx.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(value = RUNTIME)
@Target(value = TYPE)
@Inherited
public @interface MBean {
	String objectName() default "";

	String description() default "";
}
