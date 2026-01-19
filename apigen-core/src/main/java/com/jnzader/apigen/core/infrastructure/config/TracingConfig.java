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
 * Configuración de tracing distribuido usando Micrometer Tracing.
 *
 * <p>Habilita observabilidad automática para:
 *
 * <ul>
 *   <li>Peticiones HTTP entrantes
 *   <li>Llamadas HTTP salientes (RestTemplate/WebClient)
 *   <li>Métodos anotados con @Observed
 *   <li>Operaciones de base de datos
 * </ul>
 *
 * <p>El tracing se puede deshabilitar completamente con:
 *
 * <pre>
 * management.tracing.enabled=false
 * </pre>
 *
 * <p>Para exportar traces a un backend OTLP (como Jaeger, Zipkin, etc.):
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
     * Habilita el aspecto @Observed para instrumentación declarativa.
     *
     * <p>Uso:
     *
     * <pre>
     * &#64;Observed(name = "my.operation", contextualName = "processOrder")
     * public void processOrder(Order order) {
     *     // Este método será trazado automáticamente
     * }
     * </pre>
     *
     * @param registry Registro de observaciones de Micrometer
     * @return Aspecto configurado
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry registry) {
        log.info("Tracing enabled - @Observed aspect configured");
        return new ObservedAspect(registry);
    }
}
