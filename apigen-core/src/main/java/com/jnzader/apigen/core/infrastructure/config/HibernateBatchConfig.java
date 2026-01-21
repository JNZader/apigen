package com.jnzader.apigen.core.infrastructure.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura valores por defecto de Hibernate para optimización de rendimiento.
 *
 * <p>Configuración aplicada:
 *
 * <ul>
 *   <li>{@code default_batch_fetch_size=25}: Previene N+1 queries cargando colecciones lazy en
 *       lotes
 *   <li>{@code batch_size=25}: Optimiza inserts/updates en lote
 *   <li>{@code order_inserts=true}: Agrupa inserts por tipo para mejor batching
 *   <li>{@code order_updates=true}: Agrupa updates por tipo para mejor batching
 * </ul>
 *
 * <p>Estas configuraciones se pueden sobrescribir en application.yml:
 *
 * <pre>
 * spring:
 *   jpa:
 *     properties:
 *       hibernate:
 *         default_batch_fetch_size: 50
 *         jdbc:
 *           batch_size: 50
 * </pre>
 */
@Configuration
@ConditionalOnClass(name = "org.hibernate.cfg.AvailableSettings")
public class HibernateBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(HibernateBatchConfig.class);

    private static final int DEFAULT_BATCH_SIZE = 25;

    /**
     * Customiza las propiedades de Hibernate para optimización de rendimiento.
     *
     * <p>Solo aplica valores si no están ya configurados, permitiendo que el usuario los
     * sobrescriba.
     */
    @Bean
    public HibernatePropertiesCustomizer apigenHibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            setIfAbsent(
                    hibernateProperties,
                    "hibernate.default_batch_fetch_size",
                    String.valueOf(DEFAULT_BATCH_SIZE));
            setIfAbsent(
                    hibernateProperties,
                    "hibernate.jdbc.batch_size",
                    String.valueOf(DEFAULT_BATCH_SIZE));
            setIfAbsent(hibernateProperties, "hibernate.order_inserts", "true");
            setIfAbsent(hibernateProperties, "hibernate.order_updates", "true");

            log.info(
                    "Hibernate batch config: batch_fetch_size={}, jdbc.batch_size={}",
                    hibernateProperties.get("hibernate.default_batch_fetch_size"),
                    hibernateProperties.get("hibernate.jdbc.batch_size"));
        };
    }

    private void setIfAbsent(Map<String, Object> properties, String key, String value) {
        properties.putIfAbsent(key, value);
    }
}
