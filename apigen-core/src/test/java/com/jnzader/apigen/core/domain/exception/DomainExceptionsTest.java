package com.jnzader.apigen.core.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Exceptions Tests")
class DomainExceptionsTest {

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundExceptionTests {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            var exception = new ResourceNotFoundException("User not found");

            assertThat(exception.getMessage()).isEqualTo("User not found");
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException")
    class DuplicateResourceExceptionTests {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            var exception = new DuplicateResourceException("Email already exists");

            assertThat(exception.getMessage()).isEqualTo("Email already exists");
        }
    }

    @Nested
    @DisplayName("ValidationException")
    class ValidationExceptionTests {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            var exception = new ValidationException("Invalid input");

            assertThat(exception.getMessage()).isEqualTo("Invalid input");
        }
    }

    @Nested
    @DisplayName("OperationFailedException")
    class OperationFailedExceptionTests {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            var exception = new OperationFailedException("Operation failed");

            assertThat(exception.getMessage()).isEqualTo("Operation failed");
        }

        @Test
        @DisplayName("should create with message and cause")
        void shouldCreateWithMessageAndCause() {
            var cause = new RuntimeException("Database error");
            var exception = new OperationFailedException("Operation failed", cause);

            assertThat(exception.getMessage()).isEqualTo("Operation failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("PreconditionFailedException")
    class PreconditionFailedExceptionTests {

        @Test
        @DisplayName("should create with message only")
        void shouldCreateWithMessageOnly() {
            var exception = new PreconditionFailedException("ETag mismatch");

            assertThat(exception.getMessage()).isEqualTo("ETag mismatch");
            assertThat(exception.getCurrentEtag()).isNull();
            assertThat(exception.getProvidedEtag()).isNull();
        }

        @Test
        @DisplayName("should create with message and etags")
        void shouldCreateWithMessageAndEtags() {
            var exception = new PreconditionFailedException("Mismatch", "abc123", "xyz789");

            assertThat(exception.getMessage()).isEqualTo("Mismatch");
            assertThat(exception.getCurrentEtag()).isEqualTo("abc123");
            assertThat(exception.getProvidedEtag()).isEqualTo("xyz789");
        }

        @Test
        @DisplayName("should create with static factory method")
        void shouldCreateWithStaticFactoryMethod() {
            var exception = PreconditionFailedException.etagMismatch("current", "provided");

            assertThat(exception.getMessage()).contains("current").contains("provided");
            assertThat(exception.getCurrentEtag()).isEqualTo("current");
            assertThat(exception.getProvidedEtag()).isEqualTo("provided");
        }
    }

    @Nested
    @DisplayName("UnauthorizedActionException")
    class UnauthorizedActionExceptionTests {

        @Test
        @DisplayName("should create with message")
        void shouldCreateWithMessage() {
            var exception = new UnauthorizedActionException("Access denied");

            assertThat(exception.getMessage()).isEqualTo("Access denied");
        }
    }

    @Nested
    @DisplayName("IdMismatchException")
    class IdMismatchExceptionTests {

        @Test
        @DisplayName("should create with path and body ids")
        void shouldCreateWithPathAndBodyIds() {
            var exception = new IdMismatchException(1L, 2L);

            assertThat(exception.getMessage()).contains("1").contains("2");
            assertThat(exception.getPathId()).isEqualTo(1L);
            assertThat(exception.getBodyId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should create with custom message and ids")
        void shouldCreateWithCustomMessageAndIds() {
            var exception = new IdMismatchException("Custom message", 1L, 2L);

            assertThat(exception.getMessage()).isEqualTo("Custom message");
            assertThat(exception.getPathId()).isEqualTo(1L);
            assertThat(exception.getBodyId()).isEqualTo(2L);
        }
    }
}
