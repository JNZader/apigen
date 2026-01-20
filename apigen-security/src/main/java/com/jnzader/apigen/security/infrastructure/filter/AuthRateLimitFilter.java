package com.jnzader.apigen.security.infrastructure.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jnzader.apigen.security.infrastructure.network.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de rate limiting específico para endpoints de autenticación.
 *
 * <p>Implementa protección contra ataques de fuerza bruta limitando los intentos de login por IP.
 * Más restrictivo que el rate limiting general.
 *
 * <p>Configuración: - Máximo 5 intentos fallidos por ventana de 15 minutos - Después del límite,
 * bloqueo temporal de la IP
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Antes del rate limiting general
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private final int maxLoginAttempts;
    private final Duration lockoutDuration;
    private final Cache<String, AtomicInteger> loginAttempts;
    private final ClientIpResolver clientIpResolver;

    @Autowired
    public AuthRateLimitFilter(
            @Value("${app.security.auth-rate-limit.max-attempts:5}") int maxLoginAttempts,
            @Value("${app.security.auth-rate-limit.lockout-minutes:15}") int lockoutMinutes,
            ClientIpResolver clientIpResolver) {

        this.maxLoginAttempts = maxLoginAttempts;
        this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);
        this.clientIpResolver = clientIpResolver;

        this.loginAttempts =
                Caffeine.newBuilder()
                        .expireAfterWrite(lockoutDuration)
                        .maximumSize(10000) // Máximo 10k IPs en tracking
                        .build();

        log.info(
                "Auth Rate Limit Filter initialized: max {} attempts per {} minutes",
                maxLoginAttempts,
                lockoutMinutes);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Solo aplicar a endpoints de login
        if (!isLoginEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        AtomicInteger attempts = loginAttempts.get(clientIp, k -> new AtomicInteger(0));

        // Verificar si está bloqueado
        if (attempts.get() >= maxLoginAttempts) {
            log.warn("Auth rate limit exceeded for IP: {} - {} attempts", clientIp, attempts.get());
            sendRateLimitResponse(response);
            return;
        }

        // Agregar headers informativos
        response.setHeader("X-Auth-RateLimit-Limit", String.valueOf(maxLoginAttempts));
        response.setHeader(
                "X-Auth-RateLimit-Remaining",
                String.valueOf(Math.max(0, maxLoginAttempts - attempts.get())));

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);

        // Incrementar contador en caso de fallo de autenticación
        if (response.getStatus() == HttpStatus.UNAUTHORIZED.value()
                || response.getStatus() == HttpStatus.FORBIDDEN.value()) {
            int currentAttempts = attempts.incrementAndGet();
            log.info("Failed login attempt #{} from IP: {}", currentAttempts, clientIp);

            if (currentAttempts >= maxLoginAttempts) {
                log.warn("IP {} blocked after {} failed login attempts", clientIp, currentAttempts);
            }
        } else if (response.getStatus() == HttpStatus.OK.value()) {
            // Reset en login exitoso
            loginAttempts.invalidate(clientIp);
            log.debug("Successful login from IP: {}, resetting attempt counter", clientIp);
        }
    }

    private boolean isLoginEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Solo POST a endpoints de login
        return "POST".equalsIgnoreCase(method)
                && (uri.endsWith("/auth/login") || uri.endsWith("/auth/register"));
    }

    private String getClientIp(HttpServletRequest request) {
        return clientIpResolver.resolveClientIp(request);
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(lockoutDuration.toSeconds()));

        String jsonResponse =
                String.format(
                        """
                        {
                            "type": "about:blank",
                            "title": "Too Many Requests",
                            "status": 429,
                            "detail": "Demasiados intentos de autenticación fallidos. Intente nuevamente en %d minutos.",
                            "instance": "/auth/login"
                        }
                        """,
                        lockoutDuration.toMinutes());

        response.getWriter().write(jsonResponse);
    }
}
