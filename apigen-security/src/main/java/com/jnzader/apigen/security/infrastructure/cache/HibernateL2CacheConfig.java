package com.jnzader.apigen.security.infrastructure.cache;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Hibernate L2 cache using JCache with Caffeine provider.
 *
 * <p>Enables caching for Role and Permission entities which are frequently read but rarely
 * modified. This significantly reduces database queries for authorization checks.
 *
 * <p>Cache regions:
 *
 * <ul>
 *   <li>roles - Role entities (max 1000 entries, 30 min TTL)
 *   <li>permissions - Permission entities (max 5000 entries, 30 min TTL)
 *   <li>role_permissions - Role-Permission association collection (max 10000 entries, 30 min TTL)
 * </ul>
 *
 * <p>This configuration is only active when JPA and Caffeine JCache are on the classpath.
 */
@Configuration
@ConditionalOnClass({EntityManagerFactory.class, CacheManager.class, CaffeineConfiguration.class})
public class HibernateL2CacheConfig {

    private static final long DEFAULT_TTL_MINUTES = 30;
    private static final long ROLES_MAX_SIZE = 1000;
    private static final long PERMISSIONS_MAX_SIZE = 5000;
    private static final long ROLE_PERMISSIONS_MAX_SIZE = 10000;

    /**
     * Creates a JCache CacheManager with Caffeine provider and pre-configured regions.
     *
     * @return configured CacheManager
     */
    @Bean
    public CacheManager jCacheCacheManager() {
        CachingProvider provider =
                Caching.getCachingProvider(
                        "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        CacheManager cacheManager = provider.getCacheManager();

        // Create cache regions for security entities
        createCacheIfAbsent(cacheManager, "roles", ROLES_MAX_SIZE);
        createCacheIfAbsent(cacheManager, "permissions", PERMISSIONS_MAX_SIZE);
        createCacheIfAbsent(cacheManager, "role_permissions", ROLE_PERMISSIONS_MAX_SIZE);

        // Default region for query cache
        createCacheIfAbsent(cacheManager, "default-query-results-region", 1000);
        createCacheIfAbsent(cacheManager, "default-update-timestamps-region", 10000);

        return cacheManager;
    }

    /**
     * Customizes Hibernate properties to enable L2 cache with JCache/Caffeine.
     *
     * @param cacheManager the JCache CacheManager
     * @return HibernatePropertiesCustomizer that enables L2 cache
     */
    @Bean
    public HibernatePropertiesCustomizer hibernateL2CacheCustomizer(CacheManager cacheManager) {
        return (Map<String, Object> properties) -> {
            // Enable L2 cache
            properties.put("hibernate.cache.use_second_level_cache", "true");
            properties.put("hibernate.cache.use_query_cache", "true");

            // Use JCache as the region factory
            properties.put(
                    "hibernate.cache.region.factory_class",
                    "org.hibernate.cache.jcache.JCacheRegionFactory");

            // Use the pre-configured CacheManager
            properties.put(ConfigSettings.CACHE_MANAGER, cacheManager);

            // Generate statistics for monitoring (disabled by default for performance)
            properties.putIfAbsent("hibernate.generate_statistics", "false");
        };
    }

    private void createCacheIfAbsent(CacheManager cacheManager, String cacheName, long maxSize) {
        if (cacheManager.getCache(cacheName) == null) {
            CaffeineConfiguration<Object, Object> config = new CaffeineConfiguration<>();
            config.setMaximumSize(OptionalLong.of(maxSize));
            config.setExpireAfterWrite(
                    OptionalLong.of(TimeUnit.MINUTES.toNanos(DEFAULT_TTL_MINUTES)));
            config.setStatisticsEnabled(true);
            cacheManager.createCache(cacheName, config);
        }
    }
}
