package io.paradoxical.cassieq.model.validators;

import io.paradoxical.common.valuetypes.StringValue;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StringValueValidator extends CustomValidationBase implements ConstraintValidator<StringTypeValid, StringValue> {

    private StringTypeValid constraintAnnotation;

    private Pattern validPattern;

    @Override
    public void initialize(final StringTypeValid constraintAnnotation) {
        this.constraintAnnotation = constraintAnnotation;

        validPattern = Pattern.compile(constraintAnnotation.regex());
    }

    @Override
    public boolean isValid(final StringValue value, final ConstraintValidatorContext context) {
        if (validPattern.pattern() == null || validPattern.pattern().isEmpty()) {
            withError(context, "Pattern can not be null or empty");

            return false;
        }

        if (constraintAnnotation.isNullAllowed() && value == null) {
            return true;
        }

        if (constraintAnnotation.isBlankAllowed() && value.isBlank()) {
            return true;
        }

        if (validPattern.matcher(value.get()).matches()) {
            return true;
        }

        withError(context, errorMessage(value.get()));

        return false;
    }

    private String errorMessage(String value) {
        return " value \"" + value + "\" doesn't match the expected pattern. \"" + validPattern.pattern() + "\"";
    }
}