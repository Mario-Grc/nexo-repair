package com.nexo.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint: a customer needs at least an email or a phone.
 * Blank strings count as absent, so the frontend can send "" or null.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidContactValidator.class)
@Documented
public @interface ValidContact {

    String message() default "A customer needs at least an email or phone contact";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
