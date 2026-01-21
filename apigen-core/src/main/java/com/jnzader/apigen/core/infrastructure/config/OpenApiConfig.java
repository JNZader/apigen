package com.jnzader.apigen.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc configuration for generating OpenAPI/Swagger documentation for the API.
 *
 * <p>Provides custom metadata, security schemes, and error response examples for the Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    // Schema type constants
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_INTEGER = "integer";

    // Property name constants
    private static final String PROP_TYPE = "type";
    private static final String PROP_TITLE = "title";
    private static final String PROP_STATUS = "status";
    private static final String PROP_DETAIL = "detail";
    private static final String PROP_INSTANCE = "instance";

    // Media type constant
    private static final String MEDIA_TYPE_PROBLEM_JSON = "application/problem+json";

    // Schema reference constants
    private static final String SCHEMA_REF_PROBLEM_DETAIL = "#/components/schemas/ProblemDetail";
    private static final String SCHEMA_REF_VALIDATION_ERROR =
            "#/components/schemas/ValidationError";

    // Problem type constants
    private static final String PROBLEM_TYPE_VALIDATION = "urn:problem-type:validation-error";

    // Example instance constant
    private static final String EXAMPLE_INSTANCE = "/api/products";

    // Error title constants
    private static final String VALIDATION_ERROR_TITLE = "Validation error";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(apiComponents())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private Info apiInfo() {
        return new Info()
                .title("APiGen REST API")
                .version("1.0.0")
                .description(
                        """
                        Generic RESTful API built with Spring Boot 4, Java 25 and Spring Security.

                        ## Features
                        - **Full CRUD** with soft delete and restore
                        - **Dynamic filtering** with 12+ operators
                        - **Pagination** offset-based and cursor-based
                        - **ETag** for optimistic concurrency
                        - **HATEOAS** with navigation links
                        - **JWT** for authentication

                        ## Authentication
                        Use the `/v1/auth/login` endpoint to obtain a JWT token.
                        Include the token in the header: `Authorization: Bearer <token>`
                        """)
                .termsOfService("https://github.com/JNZader/apigen")
                .contact(new Contact().name("Javier N. Zader").url("https://github.com/JNZader"))
                .license(
                        new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT"));
    }

    private Components apiComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", securityScheme())
                .addSchemas("ProblemDetail", problemDetailSchema())
                .addSchemas("ValidationError", validationErrorSchema())
                .addResponses("BadRequest", badRequestResponse())
                .addResponses("Unauthorized", unauthorizedResponse())
                .addResponses("Forbidden", forbiddenResponse())
                .addResponses("NotFound", notFoundResponse())
                .addResponses("Conflict", conflictResponse())
                .addResponses("PreconditionFailed", preconditionFailedResponse())
                .addResponses("InternalError", internalErrorResponse());
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtained from the /v1/auth/login endpoint");
    }

    @SuppressWarnings("rawtypes")
    private Schema problemDetailSchema() {
        return new Schema<>()
                .type(TYPE_OBJECT)
                .description("RFC 7807 Problem Details for HTTP APIs")
                .addProperty(
                        PROP_TYPE,
                        new Schema<>().type(TYPE_STRING).example(PROBLEM_TYPE_VALIDATION))
                .addProperty(
                        PROP_TITLE,
                        new Schema<>().type(TYPE_STRING).example(VALIDATION_ERROR_TITLE))
                .addProperty(PROP_STATUS, new Schema<>().type(TYPE_INTEGER).example(400))
                .addProperty(
                        PROP_DETAIL,
                        new Schema<>().type(TYPE_STRING).example("The 'name' field is required"))
                .addProperty(
                        PROP_INSTANCE, new Schema<>().type(TYPE_STRING).example(EXAMPLE_INSTANCE));
    }

    @SuppressWarnings("rawtypes")
    private Schema validationErrorSchema() {
        return new Schema<>()
                .type(TYPE_OBJECT)
                .description("Validation error with field details")
                .addProperty(
                        PROP_TYPE,
                        new Schema<>().type(TYPE_STRING).example(PROBLEM_TYPE_VALIDATION))
                .addProperty(
                        PROP_TITLE,
                        new Schema<>().type(TYPE_STRING).example(VALIDATION_ERROR_TITLE))
                .addProperty(PROP_STATUS, new Schema<>().type(TYPE_INTEGER).example(400))
                .addProperty(
                        PROP_DETAIL,
                        new Schema<>()
                                .type(TYPE_STRING)
                                .example("Validation error in the submitted data"))
                .addProperty(
                        PROP_INSTANCE, new Schema<>().type(TYPE_STRING).example(EXAMPLE_INSTANCE))
                .addProperty(
                        "fieldErrors",
                        new Schema<>()
                                .type(TYPE_OBJECT)
                                .additionalProperties(new Schema<>().type(TYPE_STRING))
                                .example(
                                        Map.of(
                                                "name",
                                                "Name is required",
                                                "price",
                                                "Price must be positive")));
    }

    private ApiResponse badRequestResponse() {
        return new ApiResponse()
                .description("Invalid data or validation error")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_VALIDATION_ERROR))
                                                .examples(
                                                        Map.of(
                                                                "validation",
                                                                new Example()
                                                                        .summary(
                                                                                VALIDATION_ERROR_TITLE)
                                                                        .value(
                                                                                Map.of(
                                                                                        PROP_TYPE,
                                                                                        PROBLEM_TYPE_VALIDATION,
                                                                                        PROP_TITLE,
                                                                                        VALIDATION_ERROR_TITLE,
                                                                                        PROP_STATUS,
                                                                                        400,
                                                                                        PROP_DETAIL,
                                                                                        "Validation"
                                                                                            + " error"
                                                                                            + " in the"
                                                                                            + " submitted"
                                                                                            + " data",
                                                                                        PROP_INSTANCE,
                                                                                        EXAMPLE_INSTANCE,
                                                                                        "fieldErrors",
                                                                                        Map.of(
                                                                                                "name",
                                                                                                "Name is"
                                                                                                    + " required"))),
                                                                "idMismatch",
                                                                new Example()
                                                                        .summary("IDs do not match")
                                                                        .value(
                                                                                Map.of(
                                                                                        PROP_TYPE,
                                                                                        "urn:problem-type:id-mismatch",
                                                                                        PROP_TITLE,
                                                                                        "IDs do not"
                                                                                            + " match",
                                                                                        PROP_STATUS,
                                                                                        400,
                                                                                        PROP_DETAIL,
                                                                                        "The path"
                                                                                            + " ID (1)"
                                                                                            + " does"
                                                                                            + " not match"
                                                                                            + " the body"
                                                                                            + " ID (2)",
                                                                                        PROP_INSTANCE,
                                                                                        "/api/products/1",
                                                                                        "pathId",
                                                                                        1,
                                                                                        "bodyId",
                                                                                        2))))));
    }

    private ApiResponse unauthorizedResponse() {
        return new ApiResponse()
                .description("Not authenticated - JWT token missing or invalid")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                "urn:problem-type:unauthorized",
                                                                PROP_TITLE,
                                                                "Not authenticated",
                                                                PROP_STATUS,
                                                                401,
                                                                PROP_DETAIL,
                                                                "Invalid or expired JWT" + " token",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }

    private ApiResponse forbiddenResponse() {
        return new ApiResponse()
                .description("Not authorized - Insufficient permissions")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                "urn:problem-type:forbidden",
                                                                PROP_TITLE,
                                                                "Action not allowed",
                                                                PROP_STATUS,
                                                                403,
                                                                PROP_DETAIL,
                                                                "You do not have permission"
                                                                        + " to perform this"
                                                                        + " action",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }

    private ApiResponse notFoundResponse() {
        return new ApiResponse()
                .description("Resource not found")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                        "urn:problem-type:resource-not-found",
                                                                PROP_TITLE, "Resource not found",
                                                                PROP_STATUS, 404,
                                                                PROP_DETAIL,
                                                                        "Product with ID '999' not"
                                                                                + " found",
                                                                PROP_INSTANCE,
                                                                        "/api/products/999"))));
    }

    private ApiResponse conflictResponse() {
        return new ApiResponse()
                .description("Conflict - Duplicate resource or constraint violation")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                "urn:problem-type:resource-conflict",
                                                                PROP_TITLE,
                                                                "Resource conflict",
                                                                PROP_STATUS,
                                                                409,
                                                                PROP_DETAIL,
                                                                "A product with SKU 'ABC-123'"
                                                                        + " already exists",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE,
                                                                "conflictType",
                                                                "DUPLICATE_RESOURCE"))));
    }

    private ApiResponse preconditionFailedResponse() {
        return new ApiResponse()
                .description("Precondition failed - ETag mismatch (optimistic concurrency)")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                "urn:problem-type:precondition-failed",
                                                                PROP_TITLE,
                                                                "Precondition failed",
                                                                PROP_STATUS,
                                                                412,
                                                                PROP_DETAIL,
                                                                "The resource was modified by"
                                                                        + " another user",
                                                                PROP_INSTANCE,
                                                                "/api/products/1",
                                                                "expectedEtag",
                                                                "\"v2\"",
                                                                "providedEtag",
                                                                "\"v1\""))));
    }

    private ApiResponse internalErrorResponse() {
        return new ApiResponse()
                .description("Internal server error")
                .content(
                        new Content()
                                .addMediaType(
                                        MEDIA_TYPE_PROBLEM_JSON,
                                        new MediaType()
                                                .schema(
                                                        new Schema<>()
                                                                .$ref(SCHEMA_REF_PROBLEM_DETAIL))
                                                .example(
                                                        Map.of(
                                                                PROP_TYPE,
                                                                "urn:problem-type:internal-error",
                                                                PROP_TITLE,
                                                                "Internal server" + " error",
                                                                PROP_STATUS,
                                                                500,
                                                                PROP_DETAIL,
                                                                "An unexpected error" + " occurred",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }
}
