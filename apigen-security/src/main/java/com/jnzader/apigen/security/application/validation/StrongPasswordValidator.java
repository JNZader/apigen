package com.jnzader.apigen.security.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * Validador para contraseñas fuertes.
 *
 * <p>Verifica que la contraseña cumpla con los requisitos de seguridad.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL =
            Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private int minLength;
    private int maxLength;

    @Override
    public void initialize(StrongPassword annotation) {
        this.minLength = annotation.minLength();
        this.maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        // Construir mensaje de error detallado
        StringBuilder errors = new StringBuilder();

        if (password.length() < minLength) {
            errors.append(String.format("Mínimo %d caracteres. ", minLength));
        }

        if (password.length() > maxLength) {
            errors.append(String.format("Máximo %d caracteres. ", maxLength));
        }

        if (!UPPERCASE.matcher(password).find()) {
            errors.append("Requiere mayúscula. ");
        }

        if (!LOWERCASE.matcher(password).find()) {
            errors.append("Requiere minúscula. ");
        }

        if (!DIGIT.matcher(password).find()) {
            errors.append("Requiere número. ");
        }

        if (!SPECIAL.matcher(password).find()) {
            errors.append("Requiere carácter especial (!@#$%^&*). ");
        }

        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "Contraseña inválida: " + errors.toString().trim())
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
