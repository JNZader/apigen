package com.jnzader.apigen.graphql.error;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom GraphQL error implementation with RFC 7807-aligned extensions.
 *
 * <p>Provides structured error information including error type, status code, and additional
 * details.
 *
 * <p>Example error response:
 *
 * <pre>{@code
 * {
 *   "errors": [{
 *     "message": "Product not found",
 *     "extensions": {
 *       "type": "NOT_FOUND",
 *       "status": 404,
 *       "detail": "Product with ID 123 does not exist",
 *       "instance": "/products/123"
 *     },
 *     "path": ["product"]
 *   }]
 * }
 * }</pre>
 */
public class ApiGenGraphQLError implements GraphQLError {

    private final String message;
    private final List<SourceLocation> locations;
    private final transient List<Object> path;
    private final GraphQLErrorType errorType;
    private final int statusCode;
    private final String detail;
    private final String instance;
    private final transient Map<String, Object> additionalExtensions;

    private ApiGenGraphQLError(Builder builder) {
        this.message = builder.message;
        this.locations = builder.locations != null ? builder.locations : Collections.emptyList();
        this.path = builder.path;
        this.errorType = builder.errorType;
        this.statusCode = builder.statusCode;
        this.detail = builder.detail;
        this.instance = builder.instance;
        this.additionalExtensions = builder.additionalExtensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return locations;
    }

    @Override
    public List<Object> getPath() {
        return path;
    }

    @Override
    public ErrorClassification getErrorType() {
        return errorType;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("type", errorType.name());
        extensions.put("status", statusCode);

        if (detail != null) {
            extensions.put("detail", detail);
        }
        if (instance != null) {
            extensions.put("instance", instance);
        }

        if (additionalExtensions != null) {
            extensions.putAll(additionalExtensions);
        }

        return extensions;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getDetail() {
        return detail;
    }

    public String getInstance() {
        return instance;
    }

    public static class Builder {
        private String message;
        private List<SourceLocation> locations;
        private List<Object> path;
        private GraphQLErrorType errorType = GraphQLErrorType.INTERNAL_ERROR;
        private int statusCode = 500;
        private String detail;
        private String instance;
        private Map<String, Object> additionalExtensions;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder locations(List<SourceLocation> locations) {
            this.locations = locations;
            return this;
        }

        public Builder path(List<Object> path) {
            this.path = path;
            return this;
        }

        public Builder errorType(GraphQLErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder extension(String key, Object value) {
            if (this.additionalExtensions == null) {
                this.additionalExtensions = new LinkedHashMap<>();
            }
            this.additionalExtensions.put(key, value);
            return this;
        }

        public Builder notFound(String resource, Object id) {
            this.errorType = GraphQLErrorType.NOT_FOUND;
            this.statusCode = 404;
            this.message = resource + " not found";
            this.detail = resource + " with ID " + id + " does not exist";
            this.instance = "/" + resource.toLowerCase() + "s/" + id;
            return this;
        }

        public Builder validationError(String field, String reason) {
            this.errorType = GraphQLErrorType.VALIDATION_ERROR;
            this.statusCode = 400;
            this.message = "Validation failed for field: " + field;
            this.detail = reason;
            return this;
        }

        public Builder unauthorized(String reason) {
            this.errorType = GraphQLErrorType.UNAUTHORIZED;
            this.statusCode = 401;
            this.message = "Authentication required";
            this.detail = reason;
            return this;
        }

        public Builder forbidden(String reason) {
            this.errorType = GraphQLErrorType.FORBIDDEN;
            this.statusCode = 403;
            this.message = "Access denied";
            this.detail = reason;
            return this;
        }

        public Builder conflict(String reason) {
            this.errorType = GraphQLErrorType.CONFLICT;
            this.statusCode = 409;
            this.message = "Conflict";
            this.detail = reason;
            return this;
        }

        public ApiGenGraphQLError build() {
            if (message == null) {
                message = "An error occurred";
            }
            return new ApiGenGraphQLError(this);
        }
    }
}
