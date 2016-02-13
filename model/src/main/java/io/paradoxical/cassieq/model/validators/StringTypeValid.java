package io.paradoxical.cassieq.model.validators;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = { StringValueValidator.class })
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StringTypeValid {

    String message() default " [ Error in string format ] ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // alphaNumeric characters, dash, or dot
    String regex() default "[(a-zA-Z_)(\\-)(0-9)(\\.)]+";

    boolean isNullAllowed() default false;

    boolean isBlankAllowed() default false;
}
