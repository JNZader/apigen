package com.jnzader.apigen.server.exception;

import com.jnzader.apigen.exceptions.domain.ValidationException;
import java.util.List;

/**
 * Exception thrown when SQL or OpenAPI schema validation fails.
 *
 * <p>Contains a list of specific validation errors.
 */
public class SchemaValidationException extends ValidationException {

    private final List<String> validationErrors;
    private final SchemaType schemaType;

    public SchemaValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors =
                validationErrors != null ? List.copyOf(validationErrors) : List.of();
        this.schemaType = SchemaType.UNKNOWN;
    }

    public SchemaValidationException(
            String message, List<String> validationErrors, SchemaType schemaType) {
        super(message);
        this.validationErrors =
                validationErrors != null ? List.copyOf(validationErrors) : List.of();
        this.schemaType = schemaType;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    /** Types of schemas that can be validated. */
    public enum SchemaType {
        SQL,
        OPENAPI,
        UNKNOWN
    }
}
