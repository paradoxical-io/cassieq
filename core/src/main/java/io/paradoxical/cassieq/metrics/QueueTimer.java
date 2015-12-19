package io.paradoxical.cassieq.metrics;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface QueueTimer {
    /**
     * @return The param of the queue name
     */
    String segment() default "queueName";

    String actionName();
}