package com.jnzader.apigen.core.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filtro para logging de requests y responses HTTP.
 *
 * <p>Características: - Genera un ID único de correlación para cada request (traceId) - Registra
 * información del request entrante (método, URI, headers) - Registra información del response
 * (status, duración) - Soporta logging del body para debugging (configurable) - Thread-safe usando
 * MDC para el ID de correlación
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Headers para correlación
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    // MDC Keys (deben coincidir con logback-spring.xml)
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_HTTP_METHOD = "httpMethod";
    private static final String MDC_REQUEST_URI = "requestUri";

    private static final int MAX_PAYLOAD_LENGTH = 1000;
    private static final String UNKNOWN_IP = "unknown";

    // Patrones para sanitizar información sensible
    private static final Pattern JWT_PATTERN =
            Pattern.compile("(Bearer\\s+)?[\\w-]+\\.[\\w-]+\\.[\\w-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "(password|passwd|pwd|secret|token|apikey|api_key|api-key|authorization)([\"':=\\s]+)([^\"',\\s}{\\]\\[]+)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern CREDIT_CARD_PATTERN =
            Pattern.compile("\\b(?:\\d{4}[- ]?){3}\\d{4}\\b");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Generar o recuperar IDs de correlación
        String traceId = getOrGenerateId(request, TRACE_ID_HEADER);
        String correlationId = getOrGenerateId(request, CORRELATION_ID_HEADER);
        String requestId = UUID.randomUUID().toString(); // Único por request
        String spanId = UUID.randomUUID().toString().substring(0, 8); // Span corto

        // Poblar MDC con toda la información de contexto
        populateMdc(request, traceId, correlationId, requestId, spanId);

        // Agregar IDs al response para correlación cliente-servidor
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Cachear request y response para poder leer el body múltiples veces
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Log request entrante
            logRequest(wrappedRequest);

            // Procesar request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            // Log response saliente
            long duration = System.currentTimeMillis() - startTime;
            logResponse(wrappedRequest, wrappedResponse, duration);

            // Copiar el body cacheado al response original
            wrappedResponse.copyBodyToResponse();

            // Limpiar MDC completamente
            clearMdc();
        }
    }

    /** Obtiene un ID del header o genera uno nuevo. */
    private String getOrGenerateId(HttpServletRequest request, String headerName) {
        String id = request.getHeader(headerName);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    /**
     * Pobla el MDC con toda la información de contexto del request. Estos campos aparecerán
     * automáticamente en los logs JSON.
     */
    private void populateMdc(
            HttpServletRequest request,
            String traceId,
            String correlationId,
            String requestId,
            String spanId) {
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_SPAN_ID, spanId);
        MDC.put(MDC_CLIENT_IP, getClientIp(request));
        MDC.put(MDC_HTTP_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());

        // userId se establecerá después de la autenticación por SecurityContextMdcFilter
        // En este punto del filtro, el SecurityContext aún no está disponible
        MDC.put(MDC_USER_ID, "anonymous");
    }

    /** Limpia todos los campos MDC al finalizar el request. */
    private void clearMdc() {
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_SPAN_ID);
        MDC.remove(MDC_CLIENT_IP);
        MDC.remove(MDC_HTTP_METHOD);
        MDC.remove(MDC_REQUEST_URI);
        MDC.remove(MDC_USER_ID);
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        // Los campos MDC (traceId, clientIp, etc.) se incluirán automáticamente en JSON
        // Para logs de texto, incluimos información clave en el mensaje
        StringBuilder message = new StringBuilder();
        message.append(">>> ").append(method).append(" ").append(uri);
        if (queryString != null) {
            // Sanitizar query string (puede contener tokens, passwords, etc.)
            message.append("?").append(sanitize(queryString));
        }

        // Log headers relevantes (solo si es nivel DEBUG)
        if (log.isDebugEnabled()) {
            String contentType = request.getContentType();
            String userAgent = request.getHeader("User-Agent");
            if (contentType != null) {
                message.append(" | Content-Type: ").append(contentType);
            }
            if (userAgent != null) {
                message.append(" | User-Agent: ").append(truncate(userAgent, 50));
            }
        }

        log.info("{}", message);
    }

    private void logResponse(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long duration) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // Los campos MDC se incluirán automáticamente en el log JSON
        StringBuilder message = new StringBuilder();
        message.append("<<< ").append(method).append(" ").append(uri);
        message.append(" | Status: ").append(status);
        message.append(" | Duration: ").append(duration).append("ms");

        // Usar nivel de log apropiado según el status
        if (status >= 500) {
            log.error("{}", message);
            // Log body en caso de error (útil para debugging)
            logResponseBody(response);
        } else if (status >= 400) {
            log.warn("{}", message);
        } else if (duration > 1000) {
            // Advertir sobre requests lentos (> 1 segundo)
            message.append(" [SLOW]");
            log.warn("{}", message);
        } else {
            log.info("{}", message);
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0 && log.isDebugEnabled()) {
            String body = new String(content, StandardCharsets.UTF_8);
            // Sanitizar el body antes de loguearlo
            String sanitizedBody = sanitize(truncate(body, MAX_PAYLOAD_LENGTH));
            log.debug("Response Body: {}", sanitizedBody);
        }
    }

    /**
     * Sanitiza texto removiendo información sensible. Reemplaza tokens, passwords, tarjetas de
     * crédito y emails con placeholders.
     */
    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // Reemplazar JWT tokens
        result = JWT_PATTERN.matcher(result).replaceAll("[JWT_REDACTED]");

        // Reemplazar passwords y secrets (mantener el nombre del campo)
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1$2[REDACTED]");

        // Reemplazar números de tarjeta de crédito
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("[CARD_REDACTED]");

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || UNKNOWN_IP.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Si hay múltiples IPs, tomar la primera (cliente original)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // No loguear requests de actuator/health (demasiado frecuentes)
        return path.startsWith("/actuator/health")
                || path.startsWith("/actuator/prometheus")
                || path.endsWith(".ico")
                || path.endsWith(".css")
                || path.endsWith(".js");
    }
}
