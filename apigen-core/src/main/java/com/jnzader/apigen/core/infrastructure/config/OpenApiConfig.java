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
                .contact(
                        new Contact()
                                .name("Javier N. Zader")
                                .url("https://github.com/JNZader")
                                .email("jnzader@example.com"))
                .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT"));
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
                .type("object")
                .description("RFC 7807 Problem Details for HTTP APIs")
                .addProperty("type", new Schema<>().type("string").example("urn:problem-type:validation-error"))
                .addProperty("title", new Schema<>().type("string").example("Error de validación"))
                .addProperty("status", new Schema<>().type("integer").example(400))
                .addProperty("detail", new Schema<>().type("string").example("El campo 'name' es requerido"))
                .addProperty("instance", new Schema<>().type("string").example("/api/products"));
    }

    @SuppressWarnings("rawtypes")
    private Schema validationErrorSchema() {
        return new Schema<>()
                .type("object")
                .description("Error de validación con detalle de campos")
                .addProperty("type", new Schema<>().type("string").example("urn:problem-type:validation-error"))
                .addProperty("title", new Schema<>().type("string").example("Error de validación"))
                .addProperty("status", new Schema<>().type("integer").example(400))
                .addProperty("detail", new Schema<>().type("string").example("Error de validación en los datos enviados"))
                .addProperty("instance", new Schema<>().type("string").example("/api/products"))
                .addProperty(
                        "fieldErrors",
                        new Schema<>()
                                .type("object")
                                .additionalProperties(new Schema<>().type("string"))
                                .example(Map.of("name", "El nombre es requerido", "price", "El precio debe ser positivo")));
    }

    private ApiResponse badRequestResponse() {
        return new ApiResponse()
                .description("Datos inválidos o error de validación")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ValidationError"))
                                                .examples(
                                                        Map.of(
                                                                "validation",
                                                                new Example()
                                                                        .summary("Error de validación")
                                                                        .value(
                                                                                Map.of(
                                                                                        "type", "urn:problem-type:validation-error",
                                                                                        "title", "Error de validación",
                                                                                        "status", 400,
                                                                                        "detail", "Error de validación en los datos enviados",
                                                                                        "instance", "/api/products",
                                                                                        "fieldErrors", Map.of("name", "El nombre es requerido"))),
                                                                "idMismatch",
                                                                new Example()
                                                                        .summary("IDs no coinciden")
                                                                        .value(
                                                                                Map.of(
                                                                                        "type", "urn:problem-type:id-mismatch",
                                                                                        "title", "IDs no coinciden",
                                                                                        "status", 400,
                                                                                        "detail", "El ID del path (1) no coincide con el ID del body (2)",
                                                                                        "instance", "/api/products/1",
                                                                                        "pathId", 1,
                                                                                        "bodyId", 2))))));
    }

    private ApiResponse unauthorizedResponse() {
        return new ApiResponse()
                .description("No autenticado - Token JWT faltante o inválido")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:unauthorized",
                                                                "title", "No autenticado",
                                                                "status", 401,
                                                                "detail", "Token JWT inválido o expirado",
                                                                "instance", "/api/products"))));
    }

    private ApiResponse forbiddenResponse() {
        return new ApiResponse()
                .description("No autorizado - Sin permisos suficientes")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:forbidden",
                                                                "title", "Acción no permitida",
                                                                "status", 403,
                                                                "detail", "No tienes permisos para realizar esta acción",
                                                                "instance", "/api/products"))));
    }

    private ApiResponse notFoundResponse() {
        return new ApiResponse()
                .description("Recurso no encontrado")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:resource-not-found",
                                                                "title", "Recurso no encontrado",
                                                                "status", 404,
                                                                "detail", "Producto con ID '999' no encontrado",
                                                                "instance", "/api/products/999"))));
    }

    private ApiResponse conflictResponse() {
        return new ApiResponse()
                .description("Conflicto - Recurso duplicado o violación de constraint")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:resource-conflict",
                                                                "title", "Conflicto de recursos",
                                                                "status", 409,
                                                                "detail", "Ya existe un producto con el SKU 'ABC-123'",
                                                                "instance", "/api/products",
                                                                "conflictType", "DUPLICATE_RESOURCE"))));
    }

    private ApiResponse preconditionFailedResponse() {
        return new ApiResponse()
                .description("Precondición fallida - ETag no coincide (concurrencia optimista)")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:precondition-failed",
                                                                "title", "Precondición fallida",
                                                                "status", 412,
                                                                "detail", "El recurso fue modificado por otro usuario",
                                                                "instance", "/api/products/1",
                                                                "expectedEtag", "\"v2\"",
                                                                "providedEtag", "\"v1\""))));
    }

    private ApiResponse internalErrorResponse() {
        return new ApiResponse()
                .description("Error interno del servidor")
                .content(
                        new Content()
                                .addMediaType(
                                        "application/problem+json",
                                        new MediaType()
                                                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                                                .example(
                                                        Map.of(
                                                                "type", "urn:problem-type:internal-error",
                                                                "title", "Error interno del servidor",
                                                                "status", 500,
                                                                "detail", "Ha ocurrido un error inesperado",
                                                                "instance", "/api/products"))));
    }
}
