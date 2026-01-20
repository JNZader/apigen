package com.jnzader.apigen.core.infrastructure.feature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.Feature;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.core.user.NoOpUserProvider;
import org.togglz.core.user.UserProvider;

/**
 * Configuration for Togglz feature flags.
 *
 * <p>Manually configures Togglz since the togglz-spring-boot-starter is incompatible with Spring
 * Boot 4.0.0 (references removed EndpointExposure.CLOUD_FOUNDRY enum).
 *
 * <p>Enabled when {@code apigen.features.enabled=true} (default).
 *
 * <p>Features can be managed via:
 *
 * <ul>
 *   <li>Togglz Console: {@code /togglz-console}
 *   <li>FeatureChecker utility: inject and use for checking feature states
 * </ul>
 */
@Configuration
@ConditionalOnProperty(
        name = "apigen.features.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class FeatureFlagConfig {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagConfig.class);

    /**
     * Provides the enum-based feature provider for Togglz.
     *
     * @return the feature provider using ApigenFeatures enum
     */
    @Bean
    public FeatureProvider featureProvider() {
        return new EnumBasedFeatureProvider(ApigenFeatures.class);
    }

    /**
     * Provides a state repository for storing feature states. Uses in-memory storage by default.
     *
     * <p>For production, consider providing a persistent StateRepository (e.g., JDBC, file-based).
     *
     * @return the in-memory state repository
     */
    @Bean
    @ConditionalOnMissingBean(StateRepository.class)
    public StateRepository stateRepository() {
        return new InMemoryStateRepository();
    }

    /**
     * Provides a default UserProvider when no other is available.
     *
     * <p>Uses NoOpUserProvider which doesn't track users. For user-specific feature toggles,
     * provide a custom UserProvider bean (e.g., SpringSecurityUserProvider when security is
     * enabled).
     *
     * @return the default no-op user provider
     */
    @Bean
    @ConditionalOnMissingBean(UserProvider.class)
    public UserProvider userProvider() {
        return new NoOpUserProvider();
    }

    /**
     * Creates the FeatureManager with all required components.
     *
     * @param featureProvider the feature provider
     * @param stateRepository the state repository
     * @param userProvider the user provider
     * @return the configured FeatureManager
     */
    @Bean
    @ConditionalOnMissingBean(FeatureManager.class)
    public FeatureManager featureManager(
            FeatureProvider featureProvider,
            StateRepository stateRepository,
            UserProvider userProvider) {

        FeatureManager featureManager =
                new FeatureManagerBuilder()
                        .featureEnum(ApigenFeatures.class)
                        .stateRepository(stateRepository)
                        .userProvider(userProvider)
                        .build();

        log.info(
                "APiGen Feature Flags initialized with {} features",
                ApigenFeatures.values().length);

        // Log initial feature states
        if (log.isDebugEnabled()) {
            for (Feature feature : featureProvider.getFeatures()) {
                log.debug(
                        "Feature '{}' initial state: {}",
                        feature.name(),
                        featureManager.isActive(feature) ? "ENABLED" : "DISABLED");
            }
        }

        return featureManager;
    }
}
