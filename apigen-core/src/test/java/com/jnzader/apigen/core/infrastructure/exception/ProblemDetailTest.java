package com.jnzader.apigen.core.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProblemDetail Tests")
class ProblemDetailTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodsTests {

        @Test
        @DisplayName("of() should create basic problem detail")
        void ofShouldCreateBasicProblemDetail() {
            ProblemDetail detail = ProblemDetail.of(400, "Bad Request", "Invalid input");

            assertThat(detail.status()).isEqualTo(400);
            assertThat(detail.title()).isEqualTo("Bad Request");
            assertThat(detail.detail()).isEqualTo("Invalid input");
            assertThat(detail.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("validationError() should create validation problem detail")
        void validationErrorShouldCreateValidationProblemDetail() {
            Map<String, Object> errors = Map.of("field", "error message");

            ProblemDetail detail = ProblemDetail.validationError("Validation failed", errors);

            assertThat(detail.status()).isEqualTo(400);
            assertThat(detail.title()).isEqualTo("Error de validación");
            assertThat(detail.type()).hasToString("urn:apigen:problem:validation-error");
            assertThat(detail.extensions()).containsEntry("field", "error message");
        }

        @Test
        @DisplayName("notFound() should create not found problem detail")
        void notFoundShouldCreateNotFoundProblemDetail() {
            ProblemDetail detail = ProblemDetail.notFound("User", 123L);

            assertThat(detail.status()).isEqualTo(404);
            assertThat(detail.title()).isEqualTo("Recurso no encontrado");
            assertThat(detail.detail()).isEqualTo("User con ID '123' no fue encontrado");
            assertThat(detail.type()).hasToString("urn:apigen:problem:not-found");
        }

        @Test
        @DisplayName("conflict() should create conflict problem detail")
        void conflictShouldCreateConflictProblemDetail() {
            ProblemDetail detail = ProblemDetail.conflict("Resource already exists");

            assertThat(detail.status()).isEqualTo(409);
            assertThat(detail.title()).isEqualTo("Conflicto de recurso");
            assertThat(detail.type()).hasToString("urn:apigen:problem:conflict");
        }

        @Test
        @DisplayName("internalError() should create internal error problem detail")
        void internalErrorShouldCreateInternalErrorProblemDetail() {
            ProblemDetail detail = ProblemDetail.internalError("Something went wrong");

            assertThat(detail.status()).isEqualTo(500);
            assertThat(detail.title()).isEqualTo("Error interno del servidor");
            assertThat(detail.type()).hasToString("urn:apigen:problem:internal-error");
        }

        @Test
        @DisplayName("forbidden() should create forbidden problem detail")
        void forbiddenShouldCreateForbiddenProblemDetail() {
            ProblemDetail detail = ProblemDetail.forbidden("Access denied");

            assertThat(detail.status()).isEqualTo(403);
            assertThat(detail.title()).isEqualTo("Acceso denegado");
            assertThat(detail.type()).hasToString("urn:apigen:problem:forbidden");
        }

        @Test
        @DisplayName("preconditionFailed() should create precondition failed problem detail")
        void preconditionFailedShouldCreatePreconditionFailedProblemDetail() {
            ProblemDetail detail = ProblemDetail.preconditionFailed("ETag mismatch");

            assertThat(detail.status()).isEqualTo(412);
            assertThat(detail.title()).isEqualTo("Precondición fallida");
            assertThat(detail.type()).hasToString("urn:apigen:problem:precondition-failed");
        }

        @Test
        @DisplayName("badRequest() should create bad request problem detail")
        void badRequestShouldCreateBadRequestProblemDetail() {
            ProblemDetail detail = ProblemDetail.badRequest("Invalid request");

            assertThat(detail.status()).isEqualTo(400);
            assertThat(detail.title()).isEqualTo("Solicitud inválida");
            assertThat(detail.type()).hasToString("urn:apigen:problem:bad-request");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should build with all fields")
        void shouldBuildWithAllFields() {
            URI type = URI.create("https://example.com/problems/custom");
            URI instance = URI.create("/api/users/1");

            ProblemDetail detail =
                    ProblemDetail.builder()
                            .type(type)
                            .title("Custom Error")
                            .status(418)
                            .detail("I'm a teapot")
                            .instance(instance)
                            .extension("custom", "value")
                            .build();

            assertThat(detail.type()).isEqualTo(type);
            assertThat(detail.title()).isEqualTo("Custom Error");
            assertThat(detail.status()).isEqualTo(418);
            assertThat(detail.detail()).isEqualTo("I'm a teapot");
            assertThat(detail.instance()).isEqualTo(instance);
            assertThat(detail.extensions()).containsEntry("custom", "value");
        }

        @Test
        @DisplayName("should set instance from string path")
        void shouldSetInstanceFromStringPath() {
            ProblemDetail detail =
                    ProblemDetail.builder()
                            .status(404)
                            .title("Not Found")
                            .instance("/api/users/123")
                            .build();

            assertThat(detail.instance()).hasToString("/api/users/123");
        }

        @Test
        @DisplayName("should add multiple extensions")
        void shouldAddMultipleExtensions() {
            Map<String, Object> extensions = Map.of("key1", "value1", "key2", "value2");

            ProblemDetail detail =
                    ProblemDetail.builder()
                            .status(400)
                            .title("Error")
                            .extensions(extensions)
                            .build();

            assertThat(detail.extensions())
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("should generate type from title when not provided")
        void shouldGenerateTypeFromTitleWhenNotProvided() {
            ProblemDetail detail =
                    ProblemDetail.builder().status(400).title("Custom Error Title").build();

            assertThat(detail.type()).hasToString("urn:apigen:problem:custom-error-title");
        }

        @Test
        @DisplayName("should have null extensions when empty")
        void shouldHaveNullExtensionsWhenEmpty() {
            ProblemDetail detail = ProblemDetail.builder().status(400).title("Error").build();

            assertThat(detail.extensions()).isNull();
        }

        @Test
        @DisplayName("should include timestamp")
        void shouldIncludeTimestamp() {
            ProblemDetail detail = ProblemDetail.builder().status(400).title("Error").build();

            assertThat(detail.timestamp()).isNotNull();
        }
    }
}
