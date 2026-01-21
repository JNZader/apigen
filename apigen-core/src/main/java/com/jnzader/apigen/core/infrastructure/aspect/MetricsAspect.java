package com.jnzader.apigen.core.infrastructure.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Selective aspect for performance metrics.
 *
 * <p>Features: - Only measures methods annotated with @Measured (selective) - Automatically
 * measures only critical public controller operations - Registers metrics in Micrometer for
 * exposure via Prometheus - Logs slow methods (> configurable threshold) - Low overhead on
 * non-instrumented operations
 *
 * <p>To measure a specific method, use:
 *
 * <pre>{@code
 * @Measured(name = "custom-operation")
 * public Result<Data, Exception> myMethod() { }
 * }</pre>
 *
 * <p>Only loads when MeterRegistry is available (i.e., Actuator is configured).
 */
@Aspect
@Component
@ConditionalOnBean(MeterRegistry.class)
public class MetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricsAspect.class);
    private static final String TAG_LAYER = "layer";

    private final MeterRegistry meterRegistry;

    @Value("${app.metrics.enabled:true}")
    private boolean metricsEnabled;

    @Value("${app.metrics.slow-threshold-ms:500}")
    private long slowThresholdMs;

    public MetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ==================== Pointcuts ====================

    /** Pointcut for methods annotated with @Measured. */
    @Pointcut("@annotation(measured)")
    public void measuredMethod(Measured measured) {}

    /** Pointcut for classes annotated with @Measured. */
    @Pointcut("@within(measured)")
    public void measuredClass(Measured measured) {}

    /**
     * Pointcut for public controller endpoints (only @GetMapping, @PostMapping, etc.). Measured
     * automatically because they are the user entry point.
     */
    @Pointcut("execution(public * com.jnzader.apigen.core.controller..*Controller.*(..))")
    public void controllerPublicMethod() {}

    /**
     * Pointcut for service write operations. Measured automatically because they are critical
     * operations.
     */
    @Pointcut(
            "execution(* com.jnzader.apigen.core.service..*Service.save*(..)) || "
                    + "execution(* com.jnzader.apigen.core.service..*Service.update*(..)) || "
                    + "execution(* com.jnzader.apigen.core.service..*Service.*Delete*(..)) || "
                    + "execution(* com.jnzader.apigen.core.service..*Service.restore*(..))")
    public void serviceWriteOperation() {}

    // ==================== Advices ====================

    /** Measures methods explicitly annotated with @Measured. */
    @Around("measuredMethod(measured)")
    public Object measureAnnotatedMethod(ProceedingJoinPoint joinPoint, Measured measured)
            throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String metricName = getMetricName(joinPoint, measured);
        long threshold =
                measured.slowThresholdMs() > 0 ? measured.slowThresholdMs() : slowThresholdMs;

        return executeWithMetrics(joinPoint, metricName, "custom", measured.histogram(), threshold);
    }

    /** Measures methods of classes annotated with @Measured. */
    @Around("measuredClass(measured) && !measuredMethod(com.jnzader.apigen.core.aspect.Measured)")
    public Object measureClassMethod(ProceedingJoinPoint joinPoint, Measured measured)
            throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = className + "." + methodName;
        long threshold =
                measured.slowThresholdMs() > 0 ? measured.slowThresholdMs() : slowThresholdMs;

        return executeWithMetrics(joinPoint, metricName, "custom", measured.histogram(), threshold);
    }

    /**
     * Measures controller endpoints automatically. Only public endpoints, excluding methods already
     * measured by @Measured.
     */
    @Around("controllerPublicMethod() && !measuredMethod(com.jnzader.apigen.core.aspect.Measured)")
    public Object measureControllerEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = className + "." + methodName;

        return executeWithMetrics(joinPoint, metricName, "controller", false, slowThresholdMs);
    }

    /** Measures service write operations automatically. */
    @Around("serviceWriteOperation() && !measuredMethod(com.jnzader.apigen.core.aspect.Measured)")
    public Object measureServiceWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!metricsEnabled) {
            return joinPoint.proceed();
        }

        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String metricName = className + "." + methodName;

        return executeWithMetrics(joinPoint, metricName, "service", false, slowThresholdMs);
    }

    // ==================== Helper Methods ====================

    private Object executeWithMetrics(
            ProceedingJoinPoint joinPoint,
            String metricName,
            String layer,
            boolean histogram,
            long threshold)
            throws Throwable {
        Timer.Builder timerBuilder =
                Timer.builder("apigen.method.duration")
                        .tag("name", metricName)
                        .tag(TAG_LAYER, layer)
                        .description("Method execution time");

        if (histogram) {
            timerBuilder.publishPercentileHistogram();
        }

        Timer timer = timerBuilder.register(meterRegistry);
        Timer.Sample sample = Timer.start(meterRegistry);

        long startTime = System.currentTimeMillis();
        String outcome = "success";

        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "error";
            meterRegistry
                    .counter(
                            "apigen.method.errors",
                            "name",
                            metricName,
                            TAG_LAYER,
                            layer,
                            "exception",
                            throwable.getClass().getSimpleName())
                    .increment();
            throw throwable;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sample.stop(timer);

            // Register outcome
            meterRegistry
                    .counter(
                            "apigen.method.calls",
                            "name",
                            metricName,
                            TAG_LAYER,
                            layer,
                            "outcome",
                            outcome)
                    .increment();

            // Log slow methods
            if (duration > threshold) {
                log.warn(
                        "[SLOW] {}.{}() took {}ms (threshold: {}ms)",
                        layer,
                        metricName,
                        duration,
                        threshold);
            } else if (log.isTraceEnabled()) {
                log.trace("{}.{}() took {}ms", layer, metricName, duration);
            }
        }
    }

    private String getMetricName(ProceedingJoinPoint joinPoint, Measured measured) {
        if (!measured.name().isBlank()) {
            return measured.name();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();

        return className + "." + methodName;
    }
}
