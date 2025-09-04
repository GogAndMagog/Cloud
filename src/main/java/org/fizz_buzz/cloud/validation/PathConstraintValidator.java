package org.fizz_buzz.cloud.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.fizz_buzz.cloud.validation.annotation.Path;

import java.util.regex.Pattern;

public class PathConstraintValidator implements ConstraintValidator<Path, String> {

    private static final Pattern FORBIDDEN_SYMBOLS = Pattern.compile(".*[\\\\/?*:<>\"|].*");

    @Override
    public boolean isValid(String path, ConstraintValidatorContext context) {
        if (path == null) {
            return true;
        }

        for (String directory : path.split("/")) {
            if (directory.isEmpty()) {
                return false;
            }
            if (FORBIDDEN_SYMBOLS.matcher(directory).matches()) {
                return false;
            }
        }
        return true;
    }
}
