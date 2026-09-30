package com.ndzela.admin.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The one password policy for the whole API: 8-128 chars with a digit, a lower-case letter,
 * an upper-case letter and a special character. Mirrored by {@code frontend/src/schemas/password.ts}.
 */
@Documented
@NotBlank(message = "Password is required")
@Pattern(regexp = StrongPassword.REGEX, message = StrongPassword.MESSAGE)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface StrongPassword {

    String REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,128}$";
    String MESSAGE = "Password must be 8-128 characters and contain a digit, a lowercase letter, an uppercase letter and a special character";

    String message() default MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
