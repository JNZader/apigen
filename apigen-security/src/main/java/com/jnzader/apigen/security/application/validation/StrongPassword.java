package com.jnzader.apigen.security.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotación de validación para contraseñas fuertes.
 * <p>
 * Requisitos:
 * - Mínimo 12 caracteres
 * - Al menos una letra mayúscula
 * - Al menos una letra minúscula
 * - Al menos un dígito
 * - Al menos un carácter especial
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "La contraseña debe tener al menos 12 caracteres, incluyendo mayúscula, minúscula, número y carácter especial";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 12;

    int maxLength() default 128;
}
