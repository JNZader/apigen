package com.jnzader.apigen.core.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Error response conforming to RFC 7807 (Problem Details for HTTP APIs).
 *
 * <p>This class provides a standardized structure for error responses that is compatible with the
 * RFC 7807 standard.
 *
 * @param type URI that identifies the problem type
 * @param title Brief, human-readable title of the problem
 * @param status HTTP status code
 * @param detail Detailed explanation of the specific problem
 * @param instance URI that identifies the specific occurrence of the problem
 * @param timestamp Time when the error occurred
 * @param requestId Unique request ID for traceability
 * @param extensions Additional problem-specific fields
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        Instant timestamp,
        String requestId,
        Map<String, Object> extensions) {

    /**
     * Base URI for problem types following RFC 7807.
     *
     * <p>Uses URN namespace for self-documenting problem types that don't depend on external
     * domains.
     */
    private static final String BASE_TYPE_URI = "urn:apigen:problem:";

    /** Creates a ProblemDetail with the required basic fields. */
    public static ProblemDetail of(int status, String title, String detail) {
        return builder().status(status).title(title).detail(detail).build();
    }

    /** Creates a ProblemDetail for validation errors. */
    public static ProblemDetail validationError(
            String detail, Map<String, Object> validationErrors) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "validation-error"))
                .status(400)
                .title("Validation error")
                .detail(detail)
                .extensions(validationErrors)
                .build();
    }

    /** Creates a ProblemDetail for resource not found. */
    public static ProblemDetail notFound(String resourceType, Object resourceId) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "not-found"))
                .status(404)
                .title("Resource not found")
                .detail(String.format("%s with ID '%s' was not found", resourceType, resourceId))
                .build();
    }

    /** Creates a ProblemDetail for resource conflict. */
    public static ProblemDetail conflict(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "conflict"))
                .status(409)
                .title("Resource conflict")
                .detail(detail)
                .build();
    }

    /** Creates a ProblemDetail for internal error. */
    public static ProblemDetail internalError(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "internal-error"))
                .status(500)
                .title("Internal server error")
                .detail(detail)
                .build();
    }

    /** Creates a ProblemDetail for access denied. */
    public static ProblemDetail forbidden(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "forbidden"))
                .status(403)
                .title("Access denied")
                .detail(detail)
                .build();
    }

    /** Creates a ProblemDetail for precondition failed (ETag mismatch). */
    public static ProblemDetail preconditionFailed(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "precondition-failed"))
                .status(412)
                .title("Precondition failed")
                .detail(detail)
                .build();
    }

    /** Creates a ProblemDetail for generic bad request. */
    public static ProblemDetail badRequest(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "bad-request"))
                .status(400)
                .title("Invalid request")
                .detail(detail)
                .build();
    }

    /** Builder for creating ProblemDetail fluently. */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI type;
        private String title;
        private int status;
        private String detail;
        private URI instance;
        private Map<String, Object> extensions = new HashMap<>();

        public Builder type(URI type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(URI instance) {
            this.instance = instance;
            return this;
        }

        public Builder instance(String path) {
            this.instance = URI.create(path);
            return this;
        }

        public Builder extension(String key, Object value) {
            this.extensions.put(key, value);
            return this;
        }

        public Builder extensions(Map<String, Object> extensions) {
            this.extensions.putAll(extensions);
            return this;
        }

        public ProblemDetail build() {
            if (type == null && title != null) {
                type = URI.create(BASE_TYPE_URI + title.toLowerCase().replace(" ", "-"));
            }

            return new ProblemDetail(
                    type,
                    title,
                    status,
                    detail,
                    instance,
                    Instant.now(),
                    MDC.get("requestId"),
                    extensions.isEmpty() ? null : extensions);
        }
    }
}
