package com.jnzader.apigen.core.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Resilience4j para patrones de resiliencia.
 *
 * <p>Incluye: - Circuit Breaker: protege contra fallos en cascada - Rate Limiter: limita la tasa de
 * solicitudes - Retry: reintentos automáticos en caso de fallo
 *
 * <p>Uso en servicios:
 *
 * <pre>{@code
 * @CircuitBreaker(name = "default", fallbackMethod = "fallback")
 * @RateLimiter(name = "default")
 * @Retry(name = "default")
 * public Result<Data, Exception> externalCall() { }
 * }</pre>
 */
@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);
    private static final String BREAKER_DEFAULT = "default";

    /**
     * Configuración del Circuit Breaker.
     *
     * <p>Parámetros: - failureRateThreshold: 50% de fallos abre el circuito -
     * slowCallRateThreshold: 100% de llamadas lentas (deshabilitado por defecto) -
     * waitDurationInOpenState: 60 segundos antes de pasar a half-open -
     * permittedNumberOfCallsInHalfOpenState: 3 llamadas de prueba - minimumNumberOfCalls: 10
     * llamadas mínimas para calcular métricas - slidingWindowSize: 100 llamadas en la ventana
     * deslizante
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .slowCallRateThreshold(100)
                        .slowCallDurationThreshold(Duration.ofSeconds(2))
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(10)
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(100)
                        .recordExceptions(Exception.class)
                        .ignoreExceptions(IllegalArgumentException.class)
                        .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Crear circuit breakers predefinidos
        CircuitBreaker defaultCb = registry.circuitBreaker(BREAKER_DEFAULT);
        CircuitBreaker externalCb = registry.circuitBreaker("external");
        CircuitBreaker databaseCb = registry.circuitBreaker("database");

        // Event handlers para logging
        registerCircuitBreakerEventHandlers(defaultCb);
        registerCircuitBreakerEventHandlers(externalCb);
        registerCircuitBreakerEventHandlers(databaseCb);

        log.info(
                "Circuit Breaker registry initialized with default, external, and database"
                        + " breakers");

        return registry;
    }

    /**
     * Configuración del Rate Limiter.
     *
     * <p>Parámetros: - limitForPeriod: 100 llamadas por período - limitRefreshPeriod: 1 segundo -
     * timeoutDuration: 5 segundos de espera máxima
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig =
                RateLimiterConfig.custom()
                        .limitForPeriod(100)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build();

        RateLimiterConfig strictConfig =
                RateLimiterConfig.custom()
                        .limitForPeriod(10)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);

        // Crear rate limiters predefinidos
        registry.rateLimiter(BREAKER_DEFAULT);
        registry.rateLimiter("strict", strictConfig);
        registry.rateLimiter("public-api", defaultConfig);

        log.info("Rate Limiter registry initialized with default, strict, and public-api limiters");

        return registry;
    }

    /**
     * Configuración de Retry.
     *
     * <p>Parámetros: - maxAttempts: 3 intentos máximos - waitDuration: 500ms entre intentos -
     * exponentialBackoff: multiplica el tiempo de espera en cada intento
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig =
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(500))
                        .retryExceptions(Exception.class)
                        .ignoreExceptions(IllegalArgumentException.class)
                        .build();

        RetryConfig conservativeConfig =
                RetryConfig.custom()
                        .maxAttempts(2)
                        .waitDuration(Duration.ofSeconds(1))
                        .retryExceptions(Exception.class)
                        .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);

        // Crear retry configs predefinidos
        Retry defaultRetry = registry.retry(BREAKER_DEFAULT);
        registry.retry("conservative", conservativeConfig);
        registry.retry("database");

        // Event handler para logging
        defaultRetry
                .getEventPublisher()
                .onRetry(
                        event ->
                                log.warn(
                                        "Retry attempt {} for '{}'",
                                        event.getNumberOfRetryAttempts(),
                                        event.getName()));

        log.info("Retry registry initialized with default, conservative, and database retries");

        return registry;
    }

    private void registerCircuitBreakerEventHandlers(CircuitBreaker cb) {
        cb.getEventPublisher()
                .onStateTransition(
                        event ->
                                log.warn(
                                        "Circuit Breaker '{}' state transition: {} -> {}",
                                        event.getCircuitBreakerName(),
                                        event.getStateTransition().getFromState(),
                                        event.getStateTransition().getToState()))
                .onSlowCallRateExceeded(
                        event ->
                                log.warn(
                                        "Circuit Breaker '{}' slow call rate exceeded: {}%",
                                        event.getCircuitBreakerName(), event.getSlowCallRate()))
                .onFailureRateExceeded(
                        event ->
                                log.warn(
                                        "Circuit Breaker '{}' failure rate exceeded: {}%",
                                        event.getCircuitBreakerName(), event.getFailureRate()));
    }
}
