package com.jnzader.apigen.core.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Respuesta de error conforme a RFC 7807 (Problem Details for HTTP APIs).
 *
 * <p>Esta clase proporciona una estructura estandarizada para respuestas de error que es compatible
 * con el estándar RFC 7807.
 *
 * @param type URI que identifica el tipo de problema
 * @param title Título breve y legible del problema
 * @param status Código de estado HTTP
 * @param detail Explicación detallada del problema específico
 * @param instance URI que identifica la ocurrencia específica del problema
 * @param timestamp Momento en que ocurrió el error
 * @param requestId ID único de la request para trazabilidad
 * @param extensions Campos adicionales específicos del problema
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

    /** Crea un ProblemDetail con los campos básicos requeridos. */
    public static ProblemDetail of(int status, String title, String detail) {
        return builder().status(status).title(title).detail(detail).build();
    }

    /** Crea un ProblemDetail para errores de validación. */
    public static ProblemDetail validationError(
            String detail, Map<String, Object> validationErrors) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "validation-error"))
                .status(400)
                .title("Error de validación")
                .detail(detail)
                .extensions(validationErrors)
                .build();
    }

    /** Crea un ProblemDetail para recurso no encontrado. */
    public static ProblemDetail notFound(String resourceType, Object resourceId) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "not-found"))
                .status(404)
                .title("Recurso no encontrado")
                .detail(String.format("%s con ID '%s' no fue encontrado", resourceType, resourceId))
                .build();
    }

    /** Crea un ProblemDetail para conflicto de recursos. */
    public static ProblemDetail conflict(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "conflict"))
                .status(409)
                .title("Conflicto de recurso")
                .detail(detail)
                .build();
    }

    /** Crea un ProblemDetail para error interno. */
    public static ProblemDetail internalError(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "internal-error"))
                .status(500)
                .title("Error interno del servidor")
                .detail(detail)
                .build();
    }

    /** Crea un ProblemDetail para acceso denegado. */
    public static ProblemDetail forbidden(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "forbidden"))
                .status(403)
                .title("Acceso denegado")
                .detail(detail)
                .build();
    }

    /** Crea un ProblemDetail para precondición fallida (ETag mismatch). */
    public static ProblemDetail preconditionFailed(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "precondition-failed"))
                .status(412)
                .title("Precondición fallida")
                .detail(detail)
                .build();
    }

    /** Crea un ProblemDetail para bad request genérico. */
    public static ProblemDetail badRequest(String detail) {
        return builder()
                .type(URI.create(BASE_TYPE_URI + "bad-request"))
                .status(400)
                .title("Solicitud inválida")
                .detail(detail)
                .build();
    }

    /** Builder para crear ProblemDetail de forma fluida. */
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
