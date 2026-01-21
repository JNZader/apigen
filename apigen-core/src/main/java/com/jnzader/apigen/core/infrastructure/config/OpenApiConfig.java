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
 * Configuración de SpringDoc para generar la documentación OpenAPI/Swagger de la API.
 *
 * <p>Proporciona metadatos personalizados, esquemas de seguridad y ejemplos de respuestas de error
 * para la interfaz de usuario de Swagger.
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

    // Spanish text constants
    private static final String VALIDATION_ERROR_TITLE = "Error de validación";

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
                        API RESTful genérica construida con Spring Boot 4, Java 25 y Spring Security.

                        ## Características
                        - **CRUD completo** con soft delete y restauración
                        - **Filtrado dinámico** con 12+ operadores
                        - **Paginación** offset-based y cursor-based
                        - **ETag** para concurrencia optimista
                        - **HATEOAS** con links de navegación
                        - **JWT** para autenticación

                        ## Autenticación
                        Usa el endpoint `/v1/auth/login` para obtener un token JWT.
                        Incluye el token en el header: `Authorization: Bearer <token>`
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
                .description("Token JWT obtenido del endpoint /v1/auth/login");
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
                        new Schema<>().type(TYPE_STRING).example("El campo 'name' es requerido"))
                .addProperty(
                        PROP_INSTANCE, new Schema<>().type(TYPE_STRING).example(EXAMPLE_INSTANCE));
    }

    @SuppressWarnings("rawtypes")
    private Schema validationErrorSchema() {
        return new Schema<>()
                .type(TYPE_OBJECT)
                .description("VALIDATION_ERROR_TITLE con detalle de campos")
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
                                .example("VALIDATION_ERROR_TITLE en los datos enviados"))
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
                                                "El nombre es requerido",
                                                "price",
                                                "El precio debe ser positivo")));
    }

    private ApiResponse badRequestResponse() {
        return new ApiResponse()
                .description("Datos inválidos o error de validación")
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
                                                                                        "VALIDATION_ERROR_TITLE",
                                                                                        PROP_STATUS,
                                                                                        400,
                                                                                        PROP_DETAIL,
                                                                                        "VALIDATION_ERROR_TITLE"
                                                                                            + " en los"
                                                                                            + " datos"
                                                                                            + " enviados",
                                                                                        PROP_INSTANCE,
                                                                                        EXAMPLE_INSTANCE,
                                                                                        "fieldErrors",
                                                                                        Map.of(
                                                                                                "name",
                                                                                                "El nombre"
                                                                                                    + " es requerido"))),
                                                                "idMismatch",
                                                                new Example()
                                                                        .summary("IDs no coinciden")
                                                                        .value(
                                                                                Map.of(
                                                                                        PROP_TYPE,
                                                                                        "urn:problem-type:id-mismatch",
                                                                                        PROP_TITLE,
                                                                                        "IDs no"
                                                                                            + " coinciden",
                                                                                        PROP_STATUS,
                                                                                        400,
                                                                                        PROP_DETAIL,
                                                                                        "El ID del"
                                                                                            + " path"
                                                                                            + " (1) no"
                                                                                            + " coincide"
                                                                                            + " con el"
                                                                                            + " ID del"
                                                                                            + " body"
                                                                                            + " (2)",
                                                                                        PROP_INSTANCE,
                                                                                        "/api/products/1",
                                                                                        "pathId",
                                                                                        1,
                                                                                        "bodyId",
                                                                                        2))))));
    }

    private ApiResponse unauthorizedResponse() {
        return new ApiResponse()
                .description("No autenticado - Token JWT faltante o inválido")
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
                                                                "No autenticado",
                                                                PROP_STATUS,
                                                                401,
                                                                PROP_DETAIL,
                                                                "Token JWT inválido o"
                                                                        + " expirado",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }

    private ApiResponse forbiddenResponse() {
        return new ApiResponse()
                .description("No autorizado - Sin permisos suficientes")
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
                                                                "Acción no permitida",
                                                                PROP_STATUS,
                                                                403,
                                                                PROP_DETAIL,
                                                                "No tienes permisos para"
                                                                        + " realizar esta"
                                                                        + " acción",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }

    private ApiResponse notFoundResponse() {
        return new ApiResponse()
                .description("Recurso no encontrado")
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
                                                                PROP_TITLE, "Recurso no encontrado",
                                                                PROP_STATUS, 404,
                                                                PROP_DETAIL,
                                                                        "Producto con ID '999' no"
                                                                                + " encontrado",
                                                                PROP_INSTANCE,
                                                                        "/api/products/999"))));
    }

    private ApiResponse conflictResponse() {
        return new ApiResponse()
                .description("Conflicto - Recurso duplicado o violación de constraint")
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
                                                                "Conflicto de recursos",
                                                                PROP_STATUS,
                                                                409,
                                                                PROP_DETAIL,
                                                                "Ya existe un producto con el SKU"
                                                                        + " 'ABC-123'",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE,
                                                                "conflictType",
                                                                "DUPLICATE_RESOURCE"))));
    }

    private ApiResponse preconditionFailedResponse() {
        return new ApiResponse()
                .description("Precondición fallida - ETag no coincide (concurrencia optimista)")
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
                                                                "Precondición fallida",
                                                                PROP_STATUS,
                                                                412,
                                                                PROP_DETAIL,
                                                                "El recurso fue modificado por otro"
                                                                        + " usuario",
                                                                PROP_INSTANCE,
                                                                "/api/products/1",
                                                                "expectedEtag",
                                                                "\"v2\"",
                                                                "providedEtag",
                                                                "\"v1\""))));
    }

    private ApiResponse internalErrorResponse() {
        return new ApiResponse()
                .description("Error interno del servidor")
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
                                                                "Error interno del" + " servidor",
                                                                PROP_STATUS,
                                                                500,
                                                                PROP_DETAIL,
                                                                "Ha ocurrido un error"
                                                                        + " inesperado",
                                                                PROP_INSTANCE,
                                                                EXAMPLE_INSTANCE))));
    }
}
