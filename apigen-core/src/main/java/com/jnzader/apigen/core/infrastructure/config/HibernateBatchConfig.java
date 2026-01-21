package com.jnzader.apigen.core.infrastructure.config;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Hibernate default values for performance optimization.
 *
 * <p>Applied configuration:
 *
 * <ul>
 *   <li>{@code default_batch_fetch_size=25}: Prevents N+1 queries by loading lazy collections in
 *       batches
 *   <li>{@code batch_size=25}: Optimizes batch inserts/updates
 *   <li>{@code order_inserts=true}: Groups inserts by type for better batching
 *   <li>{@code order_updates=true}: Groups updates by type for better batching
 * </ul>
 *
 * <p>These configurations can be overridden in application.yml:
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
     * Customizes Hibernate properties for performance optimization.
     *
     * <p>Only applies values if not already configured, allowing the user to override them.
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
