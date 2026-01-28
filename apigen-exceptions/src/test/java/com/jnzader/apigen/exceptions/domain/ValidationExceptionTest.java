package com.jnzader.apigen.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateWithMessage() {
        var exception = new ValidationException("Invalid input");

        assertThat(exception.getMessage()).isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void shouldBeRuntimeException() {
        var exception = new ValidationException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
