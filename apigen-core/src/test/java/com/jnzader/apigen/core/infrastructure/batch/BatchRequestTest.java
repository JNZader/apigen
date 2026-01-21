package com.jnzader.apigen.core.infrastructure.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jnzader.apigen.core.infrastructure.batch.BatchRequest.BatchOperation;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("BatchRequest")
class BatchRequestTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("BatchOperation validation")
    class BatchOperationValidation {

        @Test
        @DisplayName("should accept valid GET operation")
        void validGetOperation() {
            BatchOperation op = new BatchOperation("op1", "GET", "/api/users", null, null);

            Set<ConstraintViolation<BatchOperation>> violations = validator.validate(op);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should accept valid POST operation with body")
        void validPostOperationWithBody() {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", "John");

            BatchOperation op = new BatchOperation("op1", "POST", "/api/users", null, body);

            Set<ConstraintViolation<BatchOperation>> violations = validator.validate(op);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should accept valid operation with headers")
        void validOperationWithHeaders() {
            BatchOperation op =
                    new BatchOperation(
                            "op1", "GET", "/api/users", Map.of("X-Custom", "value"), null);

            Set<ConstraintViolation<BatchOperation>> violations = validator.validate(op);

            assertThat(violations).isEmpty();
        }

        static Stream<Arguments> invalidOperationProvider() {
            return Stream.of(
                    Arguments.of("null method", null, "/api/users", "Method is required"),
                    Arguments.of(
                            "invalid method",
                            "INVALID",
                            "/api/users",
                            "GET, POST, PUT, PATCH, or DELETE"),
                    Arguments.of("null path", "GET", null, "Path is required"),
                    Arguments.of("path without slash", "GET", "api/users", "start with /"));
        }

        @ParameterizedTest(name = "should reject operation with {0}")
        @MethodSource("invalidOperationProvider")
        void rejectInvalidOperation(
                String scenario, String method, String path, String expectedError) {
            BatchOperation op = new BatchOperation("op1", method, path, null, null);

            Set<ConstraintViolation<BatchOperation>> violations = validator.validate(op);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains(expectedError);
        }

        @Test
        @DisplayName("should accept all valid HTTP methods")
        void acceptAllValidMethods() {
            List<String> validMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE");

            for (String method : validMethods) {
                BatchOperation op = new BatchOperation("op1", method, "/api/users", null, null);
                Set<ConstraintViolation<BatchOperation>> violations = validator.validate(op);
                assertThat(violations).as("Method %s should be valid", method).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("BatchRequest validation")
    class BatchRequestValidation {

        @Test
        @DisplayName("should accept valid batch request")
        void validBatchRequest() {
            List<BatchOperation> ops =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users/1", null, null),
                            new BatchOperation("op2", "DELETE", "/api/users/2", null, null));

            BatchRequest request = new BatchRequest(ops, false);

            Set<ConstraintViolation<BatchRequest>> violations = validator.validate(request);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should reject empty operations list")
        void rejectEmptyOperations() {
            BatchRequest request = new BatchRequest(List.of(), false);

            Set<ConstraintViolation<BatchRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage()).contains("cannot be empty");
        }

        @Test
        @DisplayName("should default to empty list when operations is null")
        void defaultEmptyListWhenNull() {
            BatchRequest request = new BatchRequest(null, false);

            assertThat(request.operations()).isEmpty();
        }

        @Test
        @DisplayName("should default stopOnError to false")
        void defaultStopOnErrorToFalse() {
            List<BatchOperation> ops =
                    List.of(new BatchOperation("op1", "GET", "/api/users", null, null));

            BatchRequest request = new BatchRequest(ops);

            assertThat(request.stopOnError()).isFalse();
        }

        @Test
        @DisplayName("should cascade validation to operations")
        void cascadeValidationToOperations() {
            List<BatchOperation> ops =
                    List.of(
                            new BatchOperation("op1", "GET", "/api/users", null, null),
                            new BatchOperation("op2", "INVALID", "no-slash", null, null));

            BatchRequest request = new BatchRequest(ops, false);

            Set<ConstraintViolation<BatchRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Convenience constructors")
    class ConvenienceConstructors {

        @Test
        @DisplayName("BatchOperation with method and path only")
        void operationWithMethodAndPathOnly() {
            BatchOperation op = new BatchOperation("GET", "/api/users");

            assertThat(op.id()).isNull();
            assertThat(op.method()).isEqualTo("GET");
            assertThat(op.path()).isEqualTo("/api/users");
            assertThat(op.headers()).isNull();
            assertThat(op.body()).isNull();
        }

        @Test
        @DisplayName("BatchOperation with method, path, and body")
        void operationWithMethodPathAndBody() {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("name", "John");

            BatchOperation op = new BatchOperation("POST", "/api/users", body);

            assertThat(op.id()).isNull();
            assertThat(op.method()).isEqualTo("POST");
            assertThat(op.path()).isEqualTo("/api/users");
            assertThat(op.headers()).isNull();
            assertThat(op.body()).isEqualTo(body);
        }
    }
}
