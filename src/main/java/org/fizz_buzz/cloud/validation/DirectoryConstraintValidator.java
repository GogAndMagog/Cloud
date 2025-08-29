package org.fizz_buzz.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.fizz_buzz.cloud.validation.annotation.Directory;


public class DirectoryConstraintValidator implements ConstraintValidator<Directory, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StringUtils.endsWith(value, "/") || StringUtils.isBlank(value);
    }
}
