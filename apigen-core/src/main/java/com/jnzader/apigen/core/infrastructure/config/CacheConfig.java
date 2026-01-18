package com.jnzader.apigen.core.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.jnzader.apigen.core.infrastructure.config.properties.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Configuracion de cache usando Caffeine.
 * <p>
 * Caffeine es una biblioteca de cache de alto rendimiento para Java,
 * basada en el diseno de Guava Cache pero con mejor rendimiento.
 * <p>
 * Caches configurados:
 * - entities: Cache de entidades por ID (TTL: 10 minutos)
 * - lists: Cache de listas (TTL: 5 minutos)
 * - counts: Cache de conteos (TTL: 2 minutos)
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    private final AppProperties.CacheProperties cacheProperties;

    public CacheConfig(AppProperties appProperties) {
        this.cacheProperties = appProperties != null && appProperties.cache() != null
                ? appProperties.cache()
                : new AppProperties.CacheProperties(null, null, null);
    }

    /**
     * Configura el CacheManager con multiples caches especializados.
     * Cada cache usa su propia configuracion optimizada.
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        List<CaffeineCache> caches = Arrays.asList(
                buildCache("entities", entitiesCaffeineBuilder()),
                buildCache("lists", listsCaffeineBuilder()),
                buildCache("counts", countsCaffeineBuilder())
        );

        cacheManager.setCaches(caches);

        log.info("Cache manager configurado con Caffeine. Caches especializados: entities ({}), lists ({}), counts ({})",
                cacheProperties.entities().expireAfterWrite(),
                cacheProperties.lists().expireAfterWrite(),
                cacheProperties.counts().expireAfterWrite());

        return cacheManager;
    }

    /**
     * Construye un cache Caffeine con la configuracion dada.
     */
    private CaffeineCache buildCache(String name, Caffeine<Object, Object> caffeineBuilder) {
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    /**
     * Builder especifico para cache de entidades.
     * Mayor tamano y tiempo de expiracion.
     */
    private Caffeine<Object, Object> entitiesCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.entities();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .expireAfterAccess(Duration.ofMinutes(30))
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'entities' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Builder especifico para cache de listas.
     * Menor tamano y tiempo de expiracion mas corto.
     */
    private Caffeine<Object, Object> listsCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.lists();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'lists' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Builder especifico para cache de conteos.
     * Muy pequeno y expiracion rapida.
     */
    private Caffeine<Object, Object> countsCaffeineBuilder() {
        AppProperties.CacheProperties.CacheConfig config = cacheProperties.counts();
        return Caffeine.newBuilder()
                .maximumSize(config.maxSize())
                .expireAfterWrite(config.expireAfterWrite())
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("Cache 'counts' - removida key: {}, causa: {}", key, cause));
    }

    /**
     * Manejador de errores de cache.
     * Loguea los errores pero permite que la aplicacion continue funcionando.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Error al obtener del cache '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("Error al guardar en cache '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("Error al eliminar del cache '{}' con key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error("Error al limpiar cache '{}': {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
