package io.paradoxical.cassieq.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = { StringValueValidator.class })
@Target({ ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE, ElementType.TYPE_PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StringTypeValid {

    String message() default "Invalid format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String regex();

    boolean isNullAllowed() default false;

    boolean isBlankAllowed() default false;
}
