package org.fizz_buzz.cloud.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.fizz_buzz.cloud.validation.DirectoryConstraintValidator;
import org.fizz_buzz.cloud.validation.PathConstraintValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, ANNOTATION_TYPE, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = PathConstraintValidator.class)
public @interface Path {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
