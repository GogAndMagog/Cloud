package org.fizz_buzz.cloud.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.constraintvalidation.SupportedValidationTarget;
import jakarta.validation.constraintvalidation.ValidationTarget;
import org.fizz_buzz.cloud.validation.annotation.CorrectPath;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DirectoryPathValidator implements ConstraintValidator<CorrectPath, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        Pattern pattern = Pattern.compile("[.,]");
        Matcher matcher = pattern.matcher(value);

        return matcher.find();
    }

    @Override
    public void initialize(CorrectPath constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }
}
