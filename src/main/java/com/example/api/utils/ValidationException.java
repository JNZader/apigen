package com.example.api.utils;

import java.util.List;

/**
 * Excepción personalizada que se lanza cuando hay un error de validación en los
 * datos. Esta clase extiende RuntimeException, lo que significa que es una
 * excepción no comprobada (unchecked) y no requiere declaración en los métodos.
 */
public class ValidationException extends RuntimeException {


    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Error de validación");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}