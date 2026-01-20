package com.jnzader.apigen.graphql.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ApiGenGraphQLError Tests")
class ApiGenGraphQLErrorTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create error with message and type")
        void shouldCreateErrorWithMessageAndType() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .message("Test error")
                            .errorType(GraphQLErrorType.VALIDATION_ERROR)
                            .statusCode(400)
                            .build();

            assertThat(error.getMessage()).isEqualTo("Test error");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.VALIDATION_ERROR);
            assertThat(error.getStatusCode()).isEqualTo(400);
            // Note: Different assertion subjects - cannot chain without losing type safety
        }

        @Test
        @DisplayName("should include extensions in error")
        void shouldIncludeExtensionsInError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .message("Error")
                            .errorType(GraphQLErrorType.NOT_FOUND)
                            .statusCode(404)
                            .detail("Resource not found")
                            .instance("/products/123")
                            .build();

            Map<String, Object> extensions = error.getExtensions();

            assertThat(extensions)
                    .containsEntry("type", "NOT_FOUND")
                    .containsEntry("status", 404)
                    .containsEntry("detail", "Resource not found")
                    .containsEntry("instance", "/products/123");
        }

        @Test
        @DisplayName("should support additional extensions")
        void shouldSupportAdditionalExtensions() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .message("Error")
                            .errorType(GraphQLErrorType.VALIDATION_ERROR)
                            .statusCode(400)
                            .extension("field", "email")
                            .extension("constraint", "must be valid email")
                            .build();

            Map<String, Object> extensions = error.getExtensions();

            assertThat(extensions)
                    .containsEntry("field", "email")
                    .containsEntry("constraint", "must be valid email");
        }

        @Test
        @DisplayName("should support path")
        void shouldSupportPath() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .message("Error")
                            .path(List.of("products", 0, "name"))
                            .build();

            assertThat(error.getPath()).containsExactly("products", 0, "name");
        }

        @Test
        @DisplayName("should use default message when not provided")
        void shouldUseDefaultMessageWhenNotProvided() {
            ApiGenGraphQLError error = ApiGenGraphQLError.builder().build();

            assertThat(error.getMessage()).isEqualTo("An error occurred");
        }
    }

    @Nested
    @DisplayName("Convenience Builders")
    class ConvenienceBuildersTests {

        @Test
        @DisplayName("should create not found error")
        void shouldCreateNotFoundError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder().notFound("Product", "123").build();

            assertThat(error.getMessage()).isEqualTo("Product not found");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.NOT_FOUND);
            assertThat(error.getStatusCode()).isEqualTo(404);
            assertThat(error.getDetail()).isEqualTo("Product with ID 123 does not exist");
            assertThat(error.getInstance()).isEqualTo("/products/123");
        }

        @Test
        @DisplayName("should create validation error")
        void shouldCreateValidationError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .validationError("email", "must be a valid email address")
                            .build();

            assertThat(error.getMessage()).isEqualTo("Validation failed for field: email");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.VALIDATION_ERROR);
            assertThat(error.getStatusCode()).isEqualTo(400);
            assertThat(error.getDetail()).isEqualTo("must be a valid email address");
        }

        @Test
        @DisplayName("should create unauthorized error")
        void shouldCreateUnauthorizedError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder().unauthorized("Token expired").build();

            assertThat(error.getMessage()).isEqualTo("Authentication required");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.UNAUTHORIZED);
            assertThat(error.getStatusCode()).isEqualTo(401);
        }

        @Test
        @DisplayName("should create forbidden error")
        void shouldCreateForbiddenError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder().forbidden("Insufficient permissions").build();

            assertThat(error.getMessage()).isEqualTo("Access denied");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.FORBIDDEN);
            assertThat(error.getStatusCode()).isEqualTo(403);
        }

        @Test
        @DisplayName("should create conflict error")
        void shouldCreateConflictError() {
            ApiGenGraphQLError error =
                    ApiGenGraphQLError.builder()
                            .conflict("Resource was modified by another user")
                            .build();

            assertThat(error.getMessage()).isEqualTo("Conflict");
            assertThat(error.getErrorType()).isEqualTo(GraphQLErrorType.CONFLICT);
            assertThat(error.getStatusCode()).isEqualTo(409);
        }
    }
}
