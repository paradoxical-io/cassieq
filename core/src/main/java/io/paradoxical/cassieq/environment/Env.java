package io.paradoxical.cassieq.environment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME )
public @interface Env {
    String help();

    String defaultValue() default "";
}
