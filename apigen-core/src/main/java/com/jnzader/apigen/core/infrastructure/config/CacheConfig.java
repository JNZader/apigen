package com.jnzader.apigen.core.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Local cache configuration using Caffeine.
 *
 * <p>Caffeine is a high-performance caching library for Java, based on Guava Cache design but with
 * better performance. Ideal for single-instance applications.
 *
 * <p>For distributed cache (multi-instance), use Redis with: {@code apigen.cache.type=redis}
 *
 * <p>Configured caches: - entities: Entity cache by ID (TTL: 10 minutes) - lists: List cache (TTL:
 * 5 minutes) - counts: Count cache (TTL: 2 minutes)
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "apigen.cache.type", havingValue = "local", matchIfMissing = true)
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final AppProperties.CacheProperties cacheProperties;

    public CacheConfig(AppProperties appProperties) {
        this.cacheProperties =
                appProperties != null && appProperties.cache() != null
                        ? appProperties.cache()
                        : new AppProperties.CacheProperties(null, null, null);
    }

    /**
     * Configures the CacheManager with multiple specialized caches. Each cache uses its own
     * optimized configuration.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches =
                Arrays.asList(
                        buildCache("entities", entitiesCaffeineBuilder()),
                        buildCache("lists", listsCaffeineBuilder()),
                        buildCache("counts", countsCaffeineBuilder()));

        cacheManager.setCaches(caches);

        log.info(
                "Cache manager configured with Caffeine. Specialized caches: entities ({}),"
                        + " lists ({}), counts ({})",
                cacheProperties.entities().expireAfterWrite(),
                cacheProperties.lists().expireAfterWrite(),
                cacheProperties.counts().expireAfterWrite());

        return cacheManager;
    }

    /** Builds a Caffeine cache with the given configuration. */
    private CaffeineCache buildCache(String name, Caffeine<Object, Object> caffeineBuilder) {
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    /** Specific builder for entity cache. Larger size and longer expiration time. */
    private Caffeine<Object, Object> entitiesCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.entities();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .removalListener(
                        (key, value, cause) ->
                                log.debug(
                                        "Cache 'entities' - removed key: {}, cause: {}",
                                        key,
                                        cause));
    }

    /** Specific builder for list cache. Smaller size and shorter expiration time. */
    private Caffeine<Object, Object> listsCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.lists();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener(
                        (key, value, cause) ->
                                log.debug(
                                        "Cache 'lists' - removed key: {}, cause: {}", key, cause));
    }

    /** Specific builder for count cache. Very small size and fast expiration. */
    private Caffeine<Object, Object> countsCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.counts();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener(
                        (key, value, cause) ->
                                log.debug(
                                        "Cache 'counts' - removed key: {}, cause: {}", key, cause));
    }

    /** Cache error handler. Logs errors but allows the application to continue functioning. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(
                    RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error(
                        "Error getting from cache '{}' with key '{}': {}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCachePutError(
                    RuntimeException exception,
                    org.springframework.cache.Cache cache,
                    Object key,
                    Object value) {
                log.error(
                        "Error saving to cache '{}' with key '{}': {}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(
                    RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error(
                        "Error evicting from cache '{}' with key '{}': {}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheClearError(
                    RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error("Error clearing cache '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
