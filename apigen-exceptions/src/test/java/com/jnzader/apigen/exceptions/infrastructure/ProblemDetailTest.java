package com.jnzader.apigen.exceptions.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProblemDetail Tests")
class ProblemDetailTest {

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            var type = URI.create("urn:apigen:problem:test-error");
            var instance = URI.create("/api/test/123");

            var problem =
                    ProblemDetail.builder()
                            .type(type)
                            .title("Test Error")
                            .status(400)
                            .detail("Something went wrong")
                            .instance(instance)
                            .extension("field", "value")
                            .build();

            assertThat(problem.type()).isEqualTo(type);
            assertThat(problem.title()).isEqualTo("Test Error");
            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.detail()).isEqualTo("Something went wrong");
            assertThat(problem.instance()).isEqualTo(instance);
            assertThat(problem.timestamp()).isNotNull();
            assertThat(problem.extensions()).containsEntry("field", "value");
        }

        @Test
        @DisplayName("should generate type from title when not provided")
        void shouldGenerateTypeFromTitle() {
            var problem = ProblemDetail.builder().title("Not Found").status(404).build();

            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:not-found"));
        }

        @Test
        @DisplayName("should set instance from string path")
        void shouldSetInstanceFromString() {
            var problem = ProblemDetail.builder().status(400).instance("/api/users/123").build();

            assertThat(problem.instance()).isEqualTo(URI.create("/api/users/123"));
        }

        @Test
        @DisplayName("should have null extensions when empty")
        void shouldHaveNullExtensionsWhenEmpty() {
            var problem = ProblemDetail.builder().status(500).build();

            assertThat(problem.extensions()).isNull();
        }

        @Test
        @DisplayName("should add multiple extensions")
        void shouldAddMultipleExtensions() {
            var problem =
                    ProblemDetail.builder()
                            .status(400)
                            .extension("field1", "value1")
                            .extensions(Map.of("field2", "value2", "field3", "value3"))
                            .build();

            assertThat(problem.extensions())
                    .containsEntry("field1", "value1")
                    .containsEntry("field2", "value2")
                    .containsEntry("field3", "value3");
        }

        @Test
        @DisplayName("should include timestamp")
        void shouldIncludeTimestamp() {
            var problem = ProblemDetail.builder().status(500).build();

            assertThat(problem.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Factory Methods Tests")
    class FactoryMethodsTests {

        @Test
        @DisplayName("of() should create basic problem detail")
        void ofShouldCreateBasicProblemDetail() {
            var problem = ProblemDetail.of(400, "Bad Request", "Invalid input");

            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.title()).isEqualTo("Bad Request");
            assertThat(problem.detail()).isEqualTo("Invalid input");
        }

        @Test
        @DisplayName("validationError() should create validation problem detail")
        void validationErrorShouldCreateValidationProblemDetail() {
            var errors = Map.<String, Object>of("email", "must be valid");
            var problem = ProblemDetail.validationError("Validation failed", errors);

            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.title()).isEqualTo("Validation error");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:validation-error"));
            assertThat(problem.extensions()).containsEntry("email", "must be valid");
        }

        @Test
        @DisplayName("notFound() should create not found problem detail")
        void notFoundShouldCreateNotFoundProblemDetail() {
            var problem = ProblemDetail.notFound("User", 123);

            assertThat(problem.status()).isEqualTo(404);
            assertThat(problem.title()).isEqualTo("Resource not found");
            assertThat(problem.detail()).isEqualTo("User with ID '123' was not found");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:not-found"));
        }

        @Test
        @DisplayName("conflict() should create conflict problem detail")
        void conflictShouldCreateConflictProblemDetail() {
            var problem = ProblemDetail.conflict("Email already exists");

            assertThat(problem.status()).isEqualTo(409);
            assertThat(problem.title()).isEqualTo("Resource conflict");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:conflict"));
        }

        @Test
        @DisplayName("internalError() should create internal error problem detail")
        void internalErrorShouldCreateInternalErrorProblemDetail() {
            var problem = ProblemDetail.internalError("Unexpected error occurred");

            assertThat(problem.status()).isEqualTo(500);
            assertThat(problem.title()).isEqualTo("Internal server error");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:internal-error"));
        }

        @Test
        @DisplayName("forbidden() should create forbidden problem detail")
        void forbiddenShouldCreateForbiddenProblemDetail() {
            var problem = ProblemDetail.forbidden("Access denied");

            assertThat(problem.status()).isEqualTo(403);
            assertThat(problem.title()).isEqualTo("Access denied");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:forbidden"));
        }

        @Test
        @DisplayName("preconditionFailed() should create precondition failed problem detail")
        void preconditionFailedShouldCreatePreconditionFailedProblemDetail() {
            var problem = ProblemDetail.preconditionFailed("ETag mismatch");

            assertThat(problem.status()).isEqualTo(412);
            assertThat(problem.title()).isEqualTo("Precondition failed");
            assertThat(problem.type())
                    .isEqualTo(URI.create("urn:apigen:problem:precondition-failed"));
        }

        @Test
        @DisplayName("badRequest() should create bad request problem detail")
        void badRequestShouldCreateBadRequestProblemDetail() {
            var problem = ProblemDetail.badRequest("Invalid request body");

            assertThat(problem.status()).isEqualTo(400);
            assertThat(problem.title()).isEqualTo("Invalid request");
            assertThat(problem.type()).isEqualTo(URI.create("urn:apigen:problem:bad-request"));
        }

        @Test
        @DisplayName("externalServiceError() should create external service error problem detail")
        void externalServiceErrorShouldCreateExternalServiceErrorProblemDetail() {
            var problem = ProblemDetail.externalServiceError("GitHub API failed", "github");

            assertThat(problem.status()).isEqualTo(502);
            assertThat(problem.title()).isEqualTo("External service error");
            assertThat(problem.type())
                    .isEqualTo(URI.create("urn:apigen:problem:external-service-error"));
            assertThat(problem.extensions()).containsEntry("service", "github");
        }
    }
}
