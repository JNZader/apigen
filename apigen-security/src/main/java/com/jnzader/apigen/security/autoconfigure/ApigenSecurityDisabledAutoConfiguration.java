package com.jnzader.apigen.security.autoconfigure;

import com.jnzader.apigen.core.autoconfigure.ApigenCoreAutoConfiguration;
import com.jnzader.apigen.security.infrastructure.config.SecurityDisabledConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for APiGen Security module when security is disabled.
 *
 * <p>This configuration is activated when {@code apigen.security.enabled=false} and provides a
 * permissive security configuration that allows all requests without authentication.
 *
 * <p>This is useful for:
 *
 * <ul>
 *   <li>Development environments
 *   <li>Integration testing
 *   <li>Public APIs that don't require authentication
 * </ul>
 *
 * <p>Usage: Set {@code apigen.security.enabled=false} in your application properties.
 *
 * <p>Note: @ConditionalOnBean was removed for Spring Framework 7.0 compatibility. The
 * apigen-security module requires apigen-core as a dependency, so core will always be present.
 */
@AutoConfiguration(after = ApigenCoreAutoConfiguration.class)
@ConditionalOnProperty(
        name = "apigen.security.enabled",
        havingValue = "false",
        matchIfMissing = true)
@Import(SecurityDisabledConfig.class)
public class ApigenSecurityDisabledAutoConfiguration {
    // This auto-configuration only imports SecurityDisabledConfig when security is disabled
}
