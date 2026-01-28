package com.jnzader.apigen.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OperationFailedException Tests")
class OperationFailedExceptionTest {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateWithMessage() {
        var exception = new OperationFailedException("Operation failed");

        assertThat(exception.getMessage()).isEqualTo("Operation failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new OperationFailedException("Operation failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Operation failed");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void shouldBeRuntimeException() {
        var exception = new OperationFailedException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
