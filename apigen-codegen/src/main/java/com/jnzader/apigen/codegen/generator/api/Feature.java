package com.jnzader.apigen.codegen.generator.api;

/**
 * Enum representing features that a project generator can support.
 *
 * <p>This allows for feature detection and capability negotiation when selecting or configuring
 * generators for different target languages/frameworks.
 */
public enum Feature {
    /** CRUD operations (Create, Read, Update, Delete) */
    CRUD,

    /** HATEOAS hypermedia support */
    HATEOAS,

    /** Audit trail and change tracking */
    AUDITING,

    /** Soft delete (logical deletion) */
    SOFT_DELETE,

    /** HTTP caching with ETags */
    ETAG_CACHING,

    /** Response caching */
    CACHING,

    /** Dynamic filtering and search */
    FILTERING,

    /** Pagination support (offset or cursor-based) */
    PAGINATION,

    /** OpenAPI/Swagger documentation */
    OPENAPI,

    /** JWT authentication */
    JWT_AUTH,

    /** OAuth2 authentication */
    OAUTH2,

    /** Rate limiting */
    RATE_LIMITING,

    /** Database migrations */
    MIGRATIONS,

    /** Unit test generation */
    UNIT_TESTS,

    /** Integration test generation */
    INTEGRATION_TESTS,

    /** Docker support */
    DOCKER,

    /** Many-to-many relationships */
    MANY_TO_MANY,

    /** One-to-many relationships */
    ONE_TO_MANY,

    /** Many-to-one relationships */
    MANY_TO_ONE,

    /** Batch operations */
    BATCH_OPERATIONS,

    /** Async/reactive programming */
    ASYNC,

    /** GraphQL support */
    GRAPHQL,

    /** gRPC support */
    GRPC,

    // ==========================================================================
    // New Feature Pack Features (v2.13.0+)
    // ==========================================================================

    /** Social login with OAuth2 providers (Google, GitHub, LinkedIn) */
    SOCIAL_LOGIN,

    /** Password reset flow with email verification */
    PASSWORD_RESET,

    /** Email service with templates */
    MAIL_SERVICE,

    /** File upload with local storage */
    FILE_UPLOAD,

    /** File storage with AWS S3 */
    S3_STORAGE,

    /** File storage with Azure Blob Storage */
    AZURE_STORAGE,

    /** Server-side templates with jte */
    JTE_TEMPLATES
}
