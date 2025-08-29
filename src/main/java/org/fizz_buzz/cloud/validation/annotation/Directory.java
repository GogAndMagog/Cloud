package org.fizz_buzz.cloud.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.fizz_buzz.cloud.validation.DirectoryConstraintValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = DirectoryConstraintValidator.class)
public @interface Directory {

    String message() default "Path to directory must ends with '/'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
