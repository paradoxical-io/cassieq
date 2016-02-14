package io.paradoxical.cassieq.model.validators;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomValidationBase {
    protected static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    protected static <T> boolean validate(ConstraintValidatorContext context, final T data) {
        Set<ConstraintViolation<T>> violations = validator.validate(data);

        if (violations.size() != 0) {
            withError(context, getViolationString(violations));

            return false;
        }

        return true;
    }

    protected static <T> String getViolationString(Set<ConstraintViolation<T>> violations) {
        return String.join(",", violations.stream()
                                          .map(i -> String.format("%s: %s",
                                                                  i.getPropertyPath(),
                                                                  i.getMessage()))
                                          .collect(Collectors.toList()));
    }

    protected static void withError(ConstraintValidatorContext context, String message) {
        if (context == null) {
            return;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(message)
               .addConstraintViolation();
    }
}
