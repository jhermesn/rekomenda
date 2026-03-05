package com.rekomenda.api.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = AlphanumericValidator.class)
public @interface Alphanumeric {

    String message() default "Deve conter apenas letras, números e underscore";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
