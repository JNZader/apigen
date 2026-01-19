package com.jnzader.apigen.core.infrastructure.exception;

import com.jnzader.apigen.core.domain.exception.*;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Sealed interface que representa errores de API tipados.
 *
 * <p>Proporciona un manejo de errores type-safe con pattern matching exhaustivo. Cada variante
 * corresponde a un tipo especifico de error HTTP.
 *
 * <p>Uso con Result pattern:
 *
 * <pre>
 * result.fold(
 *     success -> ResponseEntity.ok(success),
 *     error -> ApiError.from(error).toResponse()
 * );
 * </pre>
 *
 * <p>Uso con pattern matching:
 *
 * <pre>
 * switch (apiError) {
 *     case ApiError.NotFound nf -> handleNotFound(nf);
 *     case ApiError.Validation v -> handleValidation(v);
 *     // ... exhaustivo gracias a sealed
 * }
 * </pre>
 */
public sealed interface ApiError {

    /** Obtiene el mensaje del error. Todas las implementaciones deben proporcionar un mensaje. */
    String message();

    /**
     * Recurso no encontrado (HTTP 404).
     *
     * @param resourceType Tipo de recurso (e.g., "Usuario", "Producto")
     * @param resourceId Identificador del recurso buscado
     * @param message Mensaje descriptivo del error
     */
    record NotFound(String resourceType, Object resourceId, String message) implements ApiError {
        public NotFound(String resourceType, Object resourceId) {
            this(
                    resourceType,
                    resourceId,
                    String.format("%s con ID '%s' no encontrado", resourceType, resourceId));
        }

        public NotFound(String message) {
            this("Recurso", null, message);
        }
    }

    /**
     * Error de validación (HTTP 400).
     *
     * @param message Mensaje general de validacion
     * @param fieldErrors Mapa de campo -> mensaje de error especifico
     */
    record Validation(String message, Map<String, String> fieldErrors) implements ApiError {
        public Validation(String message) {
            this(message, Map.of());
        }

        public Validation(Map<String, String> fieldErrors) {
            this("Error de validación", fieldErrors);
        }
    }

    /**
     * Conflicto de recursos (HTTP 409). Usado para duplicados o violaciones de constraints unicos.
     *
     * @param message Descripcion del conflicto
     * @param conflictType Tipo de conflicto (e.g., "DUPLICATE_KEY", "UNIQUE_CONSTRAINT")
     */
    record Conflict(String message, String conflictType) implements ApiError {
        public Conflict(String message) {
            this(message, "CONFLICT");
        }
    }

    /**
     * Accion no autorizada (HTTP 403).
     *
     * @param message Descripcion de la restriccion
     * @param requiredRole Rol requerido para la accion (opcional)
     * @param currentUser Usuario actual (opcional)
     */
    record Forbidden(String message, String requiredRole, String currentUser) implements ApiError {
        public Forbidden(String message) {
            this(message, null, null);
        }
    }

    /**
     * Precondición fallida (HTTP 412). Usado para conflictos de ETag en concurrencia optimista.
     *
     * @param message Descripcion del fallo
     * @param expectedEtag ETag esperado/actual del recurso
     * @param providedEtag ETag proporcionado en el request
     */
    record PreconditionFailed(String message, String expectedEtag, String providedEtag)
            implements ApiError {
        public PreconditionFailed(String message) {
            this(message, null, null);
        }
    }

    /**
     * IDs no coinciden (HTTP 400). Cuando el ID en el path difiere del ID en el body.
     *
     * @param pathId ID en la URL
     * @param bodyId ID en el cuerpo de la peticion
     * @param message Mensaje descriptivo
     */
    record IdMismatch(Object pathId, Object bodyId, String message) implements ApiError {
        public IdMismatch(Object pathId, Object bodyId) {
            this(
                    pathId,
                    bodyId,
                    String.format(
                            "El ID del path (%s) no coincide con el ID del body (%s)",
                            pathId, bodyId));
        }
    }

    /**
     * Error interno del servidor (HTTP 500).
     *
     * @param message Mensaje de error
     * @param cause Causa original (para logging, no exponer al cliente)
     */
    record Internal(String message, Throwable cause) implements ApiError {
        public Internal(String message) {
            this(message, null);
        }

        public Internal(Throwable cause) {
            this("Error interno del servidor", cause);
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Convierte una Exception a su ApiError correspondiente. Util para transformar errores del
     * Result pattern a errores tipados.
     *
     * @param exception La excepcion a convertir
     * @return El ApiError correspondiente
     */
    static ApiError from(Exception exception) {
        return switch (exception) {
            case ResourceNotFoundException ex -> new NotFound(ex.getMessage());

            case ValidationException ex -> new Validation(ex.getMessage());

            case DuplicateResourceException ex ->
                    new Conflict(ex.getMessage(), "DUPLICATE_RESOURCE");

            case UnauthorizedActionException ex -> new Forbidden(ex.getMessage());

            case PreconditionFailedException ex ->
                    new PreconditionFailed(
                            ex.getMessage(), ex.getCurrentEtag(), ex.getProvidedEtag());

            case IdMismatchException ex ->
                    new IdMismatch(ex.getPathId(), ex.getBodyId(), ex.getMessage());

            case OperationFailedException ex -> new Internal(ex.getMessage(), ex.getCause());

            case IllegalArgumentException ex -> new Validation(ex.getMessage());

            case null, default ->
                    new Internal(
                            exception != null ? exception.getMessage() : "Error desconocido",
                            exception);
        };
    }

    /**
     * Obtiene el codigo de estado HTTP correspondiente a este error.
     *
     * @return El HttpStatus apropiado
     */
    default HttpStatus httpStatus() {
        return switch (this) {
            case NotFound _ -> HttpStatus.NOT_FOUND;
            case Validation _ -> HttpStatus.BAD_REQUEST;
            case Conflict _ -> HttpStatus.CONFLICT;
            case Forbidden _ -> HttpStatus.FORBIDDEN;
            case PreconditionFailed _ -> HttpStatus.PRECONDITION_FAILED;
            case IdMismatch _ -> HttpStatus.BAD_REQUEST;
            case Internal _ -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * Obtiene el codigo numerico HTTP.
     *
     * @return El codigo de estado (e.g., 404, 400, 500)
     */
    default int statusCode() {
        return httpStatus().value();
    }

    /**
     * Obtiene el titulo estandar del error para RFC 7807.
     *
     * @return El titulo del error
     */
    default String title() {
        return switch (this) {
            case NotFound _ -> "Recurso no encontrado";
            case Validation _ -> "Error de validación";
            case Conflict _ -> "Conflicto de recursos";
            case Forbidden _ -> "Acción no permitida";
            case PreconditionFailed _ -> "Precondición fallida";
            case IdMismatch _ -> "IDs no coinciden";
            case Internal _ -> "Error interno del servidor";
        };
    }

    /**
     * Obtiene el mensaje detallado del error.
     *
     * @return El mensaje de detalle
     */
    default String detail() {
        return message();
    }

    /**
     * Obtiene el tipo URI para RFC 7807.
     *
     * @return URI que identifica el tipo de problema
     */
    default String typeUri() {
        return switch (this) {
            case NotFound _ -> "urn:problem-type:resource-not-found";
            case Validation _ -> "urn:problem-type:validation-error";
            case Conflict _ -> "urn:problem-type:resource-conflict";
            case Forbidden _ -> "urn:problem-type:forbidden";
            case PreconditionFailed _ -> "urn:problem-type:precondition-failed";
            case IdMismatch _ -> "urn:problem-type:id-mismatch";
            case Internal _ -> "urn:problem-type:internal-error";
        };
    }

    /**
     * Convierte a ProblemDetail para respuestas RFC 7807.
     *
     * @param instance URI de la instancia (tipicamente el request URI)
     * @return ProblemDetail listo para serializar
     */
    default ProblemDetail toProblemDetail(String instance) {
        ProblemDetail.Builder builder =
                ProblemDetail.builder()
                        .type(java.net.URI.create(typeUri()))
                        .title(title())
                        .status(statusCode())
                        .detail(detail())
                        .instance(instance);

        // Agregar extensiones especificas por tipo
        switch (this) {
            case Validation v when !v.fieldErrors().isEmpty() ->
                    builder.extension("fieldErrors", v.fieldErrors());

            case PreconditionFailed pf when pf.expectedEtag() != null -> {
                builder.extension("expectedEtag", pf.expectedEtag());
                builder.extension("providedEtag", pf.providedEtag());
            }

            case IdMismatch im -> {
                builder.extension("pathId", im.pathId());
                builder.extension("bodyId", im.bodyId());
            }

            case Conflict c -> builder.extension("conflictType", c.conflictType());

            default -> {
                /* No extensions needed */
            }
        }

        return builder.build();
    }
}
