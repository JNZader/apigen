package com.jnzader.apigen.graphql.error;

import graphql.ErrorClassification;

/**
 * Custom error classification types for APiGen GraphQL errors.
 *
 * <p>These error types align with RFC 7807 Problem Details and provide semantic meaning to GraphQL
 * errors.
 */
public enum GraphQLErrorType implements ErrorClassification {

    /** Resource not found (404) */
    NOT_FOUND,

    /** Validation error (400) */
    VALIDATION_ERROR,

    /** Business rule violation (422) */
    BUSINESS_RULE_VIOLATION,

    /** Authentication required (401) */
    UNAUTHORIZED,

    /** Permission denied (403) */
    FORBIDDEN,

    /** Optimistic locking conflict (409) */
    CONFLICT,

    /** Rate limit exceeded (429) */
    RATE_LIMITED,

    /** Internal server error (500) */
    INTERNAL_ERROR,

    /** Resource already exists (409) */
    ALREADY_EXISTS,

    /** Precondition failed (412) */
    PRECONDITION_FAILED
}
