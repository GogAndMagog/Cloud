package org.fizz_buzz.cloud.validation.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.fizz_buzz.cloud.validation.validator.DirectoryPathValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = DirectoryPathValidator.class)
@Documented
public @interface CorrectPath {

    String message() default
            "Incorrect path, aaaaaAAAAA!";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
