package com.jnzader.apigen.core.infrastructure.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Distributed tracing configuration using Micrometer Tracing.
 *
 * <p>Enables automatic observability for:
 *
 * <ul>
 *   <li>Incoming HTTP requests
 *   <li>Outgoing HTTP calls (RestTemplate/WebClient)
 *   <li>Methods annotated with @Observed
 *   <li>Database operations
 * </ul>
 *
 * <p>Tracing can be completely disabled with:
 *
 * <pre>
 * management.tracing.enabled=false
 * </pre>
 *
 * <p>To export traces to an OTLP backend (such as Jaeger, Zipkin, etc.):
 *
 * <pre>
 * management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
 * </pre>
 */
@Configuration
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnProperty(
        name = "management.tracing.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    /**
     * Enables the @Observed aspect for declarative instrumentation.
     *
     * <p>Usage:
     *
     * <pre>
     * &#64;Observed(name = "my.operation", contextualName = "processOrder")
     * public void processOrder(Order order) {
     *     // This method will be automatically traced
     * }
     * </pre>
     *
     * @param registry Micrometer observation registry
     * @return Configured aspect
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        log.info("Tracing enabled - @Observed aspect configured");
        return new ObservedAspect(registry);
    }
}
