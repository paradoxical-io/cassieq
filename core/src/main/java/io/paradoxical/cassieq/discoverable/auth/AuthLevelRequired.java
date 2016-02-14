package io.paradoxical.cassieq.discoverable.auth;

import com.google.inject.BindingAnnotation;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@BindingAnnotation
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthLevelRequired {
    AuthorizationLevel level();
}
