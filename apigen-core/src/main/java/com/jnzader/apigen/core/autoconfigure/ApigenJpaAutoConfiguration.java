package com.jnzader.apigen.core.autoconfigure;

import com.jnzader.apigen.core.infrastructure.config.JpaConfig;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for APiGen JPA auditing.
 * <p>
 * This auto-configuration is separate from ApigenCoreAutoConfiguration
 * to ensure JPA auditing only loads when JPA is properly configured.
 * This prevents "JPA metamodel must not be empty" errors in slice tests
 * like @WebMvcTest that don't have JPA infrastructure.
 * <p>
 * Conditions:
 * <ul>
 *     <li>JPA classes are on classpath</li>
 *     <li>EntityManagerFactory bean exists</li>
 *     <li>apigen.core.enabled property is true (default)</li>
 * </ul>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass(EntityManagerFactory.class)
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnProperty(name = "apigen.core.enabled", havingValue = "true", matchIfMissing = true)
@Import(JpaConfig.class)
public class ApigenJpaAutoConfiguration {
    // JpaConfig is imported conditionally
}
