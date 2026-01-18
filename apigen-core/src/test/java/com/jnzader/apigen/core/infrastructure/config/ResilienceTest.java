package com.jnzader.apigen.core.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests para patrones de resiliencia (Resilience4j).
 * <p>
 * Verifica:
 * - Circuit Breaker behavior
 * - Rate Limiter behavior
 * - Retry behavior
 * - Integration of patterns
 */
@DisplayName("Resilience Patterns Tests")
class ResilienceTest {

    // ==================== Circuit Breaker Tests ====================

    @Nested
    @DisplayName("Circuit Breaker")
    class CircuitBreakerTests {

        private CircuitBreakerRegistry registry;
        private CircuitBreaker circuitBreaker;

        @BeforeEach
        void setUp() {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(5)
                    .slidingWindowSize(10)
                    .waitDurationInOpenState(Duration.ofMillis(100))
                    .permittedNumberOfCallsInHalfOpenState(2)
                    .build();

            registry = CircuitBreakerRegistry.of(config);
            circuitBreaker = registry.circuitBreaker("test");
        }

        @Test
        @DisplayName("should start in CLOSED state")
        void shouldStartInClosedState() {
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should open after failure threshold exceeded")
        void shouldOpenAfterFailureThresholdExceeded() {
            // Given - a supplier that always fails
            Supplier<String> failingSupplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> { throw new RuntimeException("Failure"); }
            );

            // When - execute enough failures to trip the circuit
            for (int i = 0; i < 10; i++) {
                try {
                    failingSupplier.get();
                } catch (Exception _) {
                    // Expected: intentional failure for resilience pattern testing
                }
            }

            // Then
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("should allow successful calls in CLOSED state")
        void shouldAllowSuccessfulCallsInClosedState() {
            // Given
            Supplier<String> successSupplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> "success"
            );

            // When
            String result = successSupplier.get();

            // Then
            assertThat(result).isEqualTo("success");
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("should record metrics correctly")
        void shouldRecordMetricsCorrectly() {
            // Given
            Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                    circuitBreaker,
                    () -> "success"
            );

            // When
            for (int i = 0; i < 5; i++) {
                supplier.get();
            }

            // Then
            var metrics = circuitBreaker.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(5);
            assertThat(metrics.getNumberOfFailedCalls()).isZero();
        }
    }

    // ==================== Rate Limiter Tests ====================

    @Nested
    @DisplayName("Rate Limiter")
    class RateLimiterTests {

        private RateLimiterRegistry registry;
        private RateLimiter rateLimiter;

        @BeforeEach
        void setUp() {
            RateLimiterConfig config = RateLimiterConfig.custom()
                    .limitForPeriod(3)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ofMillis(100))
                    .build();

            registry = RateLimiterRegistry.of(config);
            rateLimiter = registry.rateLimiter("test");
        }

        @Test
        @DisplayName("should allow calls within limit")
        void shouldAllowCallsWithinLimit() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);
            Runnable limitedRunnable = RateLimiter.decorateRunnable(
                    rateLimiter,
                    counter::incrementAndGet
            );

            // When - execute within limit
            for (int i = 0; i < 3; i++) {
                limitedRunnable.run();
            }

            // Then
            assertThat(counter.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("should block calls exceeding limit")
        void shouldBlockCallsExceedingLimit() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);
            Runnable limitedRunnable = RateLimiter.decorateRunnable(
                    rateLimiter,
                    counter::incrementAndGet
            );

            // When - execute beyond limit
            int successCount = 0;
            for (int i = 0; i < 10; i++) {
                try {
                    limitedRunnable.run();
                    successCount++;
                } catch (Exception _) {
                    // Expected: intentional failure for resilience pattern testing
                }
            }

            // Then - only 3 should succeed immediately
            assertThat(successCount).isLessThanOrEqualTo(4); // Allow some margin
        }

        @Test
        @DisplayName("should report available permissions")
        void shouldReportAvailablePermissions() {
            // Given
            int initialPermissions = rateLimiter.getMetrics().getAvailablePermissions();

            // When
            rateLimiter.acquirePermission();

            // Then
            int remainingPermissions = rateLimiter.getMetrics().getAvailablePermissions();
            assertThat(remainingPermissions).isEqualTo(initialPermissions - 1);
        }
    }

    // ==================== Retry Tests ====================

    @Nested
    @DisplayName("Retry")
    class RetryTests {

        private RetryRegistry registry;
        private Retry retry;

        @BeforeEach
        void setUp() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(10))
                    .retryExceptions(RuntimeException.class)
                    .build();

            registry = RetryRegistry.of(config);
            retry = registry.retry("test");
        }

        @Test
        @DisplayName("should succeed without retry on first success")
        void shouldSucceedWithoutRetryOnFirstSuccess() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                attempts.incrementAndGet();
                return "success";
            });

            // When
            String result = supplier.get();

            // Then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should retry on failure and succeed")
        void shouldRetryOnFailureAndSucceed() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                if (attempts.incrementAndGet() < 3) {
                    throw new RuntimeException("Temporary failure");
                }
                return "success";
            });

            // When
            String result = supplier.get();

            // Then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("should fail after max retries exhausted")
        void shouldFailAfterMaxRetriesExhausted() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                attempts.incrementAndGet();
                throw new RuntimeException("Permanent failure");
            });

            // When/Then
            assertThatThrownBy(supplier::get)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Permanent failure");

            assertThat(attempts.get()).isEqualTo(3); // maxAttempts
        }

        @Test
        @DisplayName("should record retry metrics")
        void shouldRecordRetryMetrics() {
            // Given
            AtomicInteger attempts = new AtomicInteger(0);
            Supplier<String> supplier = Retry.decorateSupplier(retry, () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new RuntimeException("Failure");
                }
                return "success";
            });

            // When
            supplier.get();

            // Then
            var metrics = retry.getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(1);
        }
    }

    // ==================== Combined Patterns Tests ====================

    @Nested
    @DisplayName("Combined Patterns")
    class CombinedPatternsTests {

        @Test
        @DisplayName("should combine CircuitBreaker and Retry")
        void shouldCombineCircuitBreakerAndRetry() {
            // Given
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(5)
                    .slidingWindowSize(10)
                    .build();

            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(2)
                    .waitDuration(Duration.ofMillis(10))
                    .build();

            CircuitBreaker cb = CircuitBreakerRegistry.of(cbConfig).circuitBreaker("combined");
            Retry retry = RetryRegistry.of(retryConfig).retry("combined");

            AtomicInteger attempts = new AtomicInteger(0);

            // Combine: Retry wraps CircuitBreaker
            Supplier<String> supplier = Retry.decorateSupplier(retry,
                    CircuitBreaker.decorateSupplier(cb, () -> {
                        if (attempts.incrementAndGet() < 2) {
                            throw new RuntimeException("Failure");
                        }
                        return "success";
                    }));

            // When
            String result = supplier.get();

            // Then
            assertThat(result).isEqualTo("success");
            assertThat(attempts.get()).isEqualTo(2);
        }

        @Test
        @DisplayName("should combine RateLimiter and CircuitBreaker")
        void shouldCombineRateLimiterAndCircuitBreaker() {
            // Given
            RateLimiterConfig rlConfig = RateLimiterConfig.custom()
                    .limitForPeriod(10)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ofMillis(100))
                    .build();

            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(3)
                    .slidingWindowSize(5)
                    .build();

            RateLimiter rl = RateLimiterRegistry.of(rlConfig).rateLimiter("combined");
            CircuitBreaker cb = CircuitBreakerRegistry.of(cbConfig).circuitBreaker("combined");

            AtomicInteger counter = new AtomicInteger(0);

            // Combine: RateLimiter wraps CircuitBreaker
            Supplier<Integer> supplier = RateLimiter.decorateSupplier(rl,
                    CircuitBreaker.decorateSupplier(cb, counter::incrementAndGet));

            // When
            for (int i = 0; i < 5; i++) {
                supplier.get();
            }

            // Then
            assertThat(counter.get()).isEqualTo(5);
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }
}
