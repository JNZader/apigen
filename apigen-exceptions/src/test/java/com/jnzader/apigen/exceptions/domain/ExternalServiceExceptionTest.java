package com.jnzader.apigen.exceptions.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalServiceException Tests")
class ExternalServiceExceptionTest {

    @Test
    @DisplayName("should create exception with message and service name")
    void shouldCreateWithMessageAndServiceName() {
        var exception = new ExternalServiceException("API call failed", "github");

        assertThat(exception.getMessage()).isEqualTo("API call failed");
        assertThat(exception.getServiceName()).isEqualTo("github");
        assertThat(exception.getOriginalError()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message, service name and cause")
    void shouldCreateWithMessageServiceNameAndCause() {
        var cause = new RuntimeException("Connection timeout");
        var exception = new ExternalServiceException("API call failed", "stripe", cause);

        assertThat(exception.getMessage()).isEqualTo("API call failed");
        assertThat(exception.getServiceName()).isEqualTo("stripe");
        assertThat(exception.getOriginalError()).isEqualTo("Connection timeout");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should create exception with all parameters")
    void shouldCreateWithAllParameters() {
        var cause = new RuntimeException("Network error");
        var exception =
                new ExternalServiceException(
                        "API call failed", "aws", "S3 bucket not found", cause);

        assertThat(exception.getMessage()).isEqualTo("API call failed");
        assertThat(exception.getServiceName()).isEqualTo("aws");
        assertThat(exception.getOriginalError()).isEqualTo("S3 bucket not found");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("should handle null cause gracefully")
    void shouldHandleNullCause() {
        var exception = new ExternalServiceException("API call failed", "service", null);

        assertThat(exception.getOriginalError()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void shouldBeRuntimeException() {
        var exception = new ExternalServiceException("test", "service");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
