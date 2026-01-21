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
 * Resilience4j configuration for resilience patterns.
 *
 * <p>Includes: - Circuit Breaker: protects against cascading failures - Rate Limiter: limits
 * request rate - Retry: automatic retries on failure
 *
 * <p>Usage in services:
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
     * Circuit Breaker configuration.
     *
     * <p>Parameters: - failureRateThreshold: 50% failures opens the circuit -
     * slowCallRateThreshold: 100% slow calls (disabled by default) - waitDurationInOpenState: 60
     * seconds before switching to half-open - permittedNumberOfCallsInHalfOpenState: 3 test calls -
     * minimumNumberOfCalls: 10 minimum calls to calculate metrics - slidingWindowSize: 100 calls in
     * the sliding window
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

        // Create predefined circuit breakers
        CircuitBreaker defaultCb = registry.circuitBreaker(BREAKER_DEFAULT);
        CircuitBreaker externalCb = registry.circuitBreaker("external");
        CircuitBreaker databaseCb = registry.circuitBreaker("database");

        // Event handlers for logging
        registerCircuitBreakerEventHandlers(defaultCb);
        registerCircuitBreakerEventHandlers(externalCb);
        registerCircuitBreakerEventHandlers(databaseCb);

        log.info(
                "Circuit Breaker registry initialized with default, external, and database"
                        + " breakers");

        return registry;
    }

    /**
     * Rate Limiter configuration.
     *
     * <p>Parameters: - limitForPeriod: 100 calls per period - limitRefreshPeriod: 1 second -
     * timeoutDuration: 5 seconds maximum wait
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

        // Create predefined rate limiters
        registry.rateLimiter(BREAKER_DEFAULT);
        registry.rateLimiter("strict", strictConfig);
        registry.rateLimiter("public-api", defaultConfig);

        log.info("Rate Limiter registry initialized with default, strict, and public-api limiters");

        return registry;
    }

    /**
     * Retry configuration.
     *
     * <p>Parameters: - maxAttempts: 3 maximum attempts - waitDuration: 500ms between attempts -
     * exponentialBackoff: multiplies wait time on each attempt
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

        // Create predefined retry configs
        Retry defaultRetry = registry.retry(BREAKER_DEFAULT);
        registry.retry("conservative", conservativeConfig);
        registry.retry("database");

        // Event handler for logging
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
