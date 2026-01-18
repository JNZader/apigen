package com.jnzader.apigen.core.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

/**
 * Interceptor que agrega un Request ID único a cada solicitud HTTP.
 * <p>
 * El Request ID se utiliza para:
 * - Trazabilidad de requests a través de logs
 * - Correlación de errores en sistemas distribuidos
 * - Debugging y soporte al cliente
 * <p>
 * El ID se obtiene del header X-Request-ID si existe, o se genera uno nuevo.
 * Se agrega al MDC de SLF4J para logging y se retorna en el header de respuesta.
 */
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Obtener Request ID del header o generar uno nuevo
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = generateRequestId();
        }

        // Agregar al MDC para logging
        MDC.put(MDC_REQUEST_ID_KEY, requestId);

        // Agregar al header de respuesta
        response.setHeader(REQUEST_ID_HEADER, requestId);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // No action needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Limpiar MDC al finalizar la request
        MDC.remove(MDC_REQUEST_ID_KEY);
    }

    /**
     * Genera un Request ID único.
     * <p>
     * Usa UUID v4 truncado para balance entre unicidad y legibilidad.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
