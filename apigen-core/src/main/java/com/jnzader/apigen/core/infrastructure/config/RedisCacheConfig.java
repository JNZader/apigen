package com.jnzader.apigen.core.infrastructure.config;

import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-based distributed cache configuration.
 *
 * <p>Provides an alternative to Caffeine for distributed environments where multiple application
 * instances need to share cache state.
 *
 * <p>Enabled when: {@code apigen.cache.type=redis}
 *
 * <p>Required properties:
 *
 * <pre>
 * spring:
 *   data:
 *     redis:
 *       host: localhost
 *       port: 6379
 * apigen:
 *   cache:
 *     type: redis
 * </pre>
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "apigen.cache.type", havingValue = "redis")
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisCacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    private final AppProperties.CacheProperties cacheProperties;

    public RedisCacheConfig(AppProperties appProperties) {
        this.cacheProperties =
                appProperties != null && appProperties.cache() != null
                        ? appProperties.cache()
                        : new AppProperties.CacheProperties(null, null, null);
    }

    /**
     * Creates a Redis-based CacheManager with per-cache TTL configuration.
     *
     * @param connectionFactory the Redis connection factory
     * @return the configured RedisCacheManager
     */
    @Bean
    @Primary
    @Override
    public CacheManager cacheManager() {
        // This will be called by Spring, we need to get the connection factory from context
        throw new UnsupportedOperationException("Use cacheManager(RedisConnectionFactory) instead");
    }

    /**
     * Creates a Redis-based CacheManager with per-cache TTL configuration.
     *
     * @param connectionFactory the Redis connection factory
     * @return the configured RedisCacheManager
     */
    @Bean(name = "redisCacheManager")
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        GenericJacksonJsonRedisSerializer.builder().build()))
                        .prefixCacheNameWith("apigen:")
                        .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Entities cache - longer TTL for individual entities
        cacheConfigs.put(
                "entities", defaultConfig.entryTtl(cacheProperties.entities().expireAfterWrite()));

        // Lists cache - shorter TTL as lists change more frequently
        cacheConfigs.put(
                "lists", defaultConfig.entryTtl(cacheProperties.lists().expireAfterWrite()));

        // Counts cache - shortest TTL as counts change frequently
        cacheConfigs.put(
                "counts", defaultConfig.entryTtl(cacheProperties.counts().expireAfterWrite()));

        log.info(
                "Redis cache manager configured. Caches: entities (TTL: {}), lists (TTL: {}),"
                        + " counts (TTL: {})",
                cacheProperties.entities().expireAfterWrite(),
                cacheProperties.lists().expireAfterWrite(),
                cacheProperties.counts().expireAfterWrite());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    /**
     * Error handler for Redis cache operations. Logs errors but allows the application to continue.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(
                    RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error(
                        "Redis cache GET error for cache '{}' with key '{}': {}",
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
                        "Redis cache PUT error for cache '{}' with key '{}': {}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(
                    RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error(
                        "Redis cache EVICT error for cache '{}' with key '{}': {}",
                        cache.getName(),
                        key,
                        exception.getMessage());
            }

            @Override
            public void handleCacheClearError(
                    RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error(
                        "Redis cache CLEAR error for cache '{}': {}",
                        cache.getName(),
                        exception.getMessage());
            }
        };
    }
}
