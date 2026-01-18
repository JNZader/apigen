package com.jnzader.apigen.core.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests para CacheEvictionService.
 * <p>
 * Verifica que el servicio de eviction selectivo funciona correctamente
 * para diferentes tipos de cache y escenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CacheEvictionService Tests")
class CacheEvictionServiceTest {

    @Mock
    private CacheManager cacheManager;

    private CacheEvictionService cacheEvictionService;

    // Caffeine caches for testing
    private Cache<Object, Object> listsNativeCache;
    private Cache<Object, Object> entitiesNativeCache;
    private Cache<Object, Object> countsNativeCache;

    private CaffeineCache listsCaffeineCache;
    private CaffeineCache entitiesCaffeineCache;
    private CaffeineCache countsCaffeineCache;

    @BeforeEach
    void setUp() {
        cacheEvictionService = new CacheEvictionService(cacheManager);

        // Create Caffeine native caches
        listsNativeCache = Caffeine.newBuilder().build();
        entitiesNativeCache = Caffeine.newBuilder().build();
        countsNativeCache = Caffeine.newBuilder().build();

        // Wrap in Spring CaffeineCache
        listsCaffeineCache = new CaffeineCache("lists", listsNativeCache);
        entitiesCaffeineCache = new CaffeineCache("entities", entitiesNativeCache);
        countsCaffeineCache = new CaffeineCache("counts", countsNativeCache);
    }

    @Nested
    @DisplayName("Evict Lists By Entity Name")
    class EvictListsByEntityNameTests {

        @Test
        @DisplayName("should evict only entries matching entity prefix")
        void shouldEvictOnlyEntriesMatchingEntityPrefix() {
            // Populate cache with mixed entries
            listsNativeCache.put("User:all:0:20:id:ASC", "user-list-1");
            listsNativeCache.put("User:active:0:10:name:DESC", "user-list-2");
            listsNativeCache.put("Product:all:0:20:id:ASC", "product-list-1");
            listsNativeCache.put("Order:all:0:20:id:ASC", "order-list-1");

            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);

            // Act
            cacheEvictionService.evictListsByEntityName("User");

            // Assert: User entries evicted, others remain
            assertThat(listsNativeCache.getIfPresent("User:all:0:20:id:ASC")).isNull();
            assertThat(listsNativeCache.getIfPresent("User:active:0:10:name:DESC")).isNull();
            assertThat(listsNativeCache.getIfPresent("Product:all:0:20:id:ASC")).isEqualTo("product-list-1");
            assertThat(listsNativeCache.getIfPresent("Order:all:0:20:id:ASC")).isEqualTo("order-list-1");
        }

        @Test
        @DisplayName("should handle empty cache gracefully")
        void shouldHandleEmptyCacheGracefully() {
            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);

            // Act - should not throw
            cacheEvictionService.evictListsByEntityName("User");

            // Assert - cache is still empty
            assertThat(listsNativeCache.asMap()).isEmpty();
        }

        @Test
        @DisplayName("should handle missing cache gracefully")
        void shouldHandleMissingCacheGracefully() {
            when(cacheManager.getCache("lists")).thenReturn(null);

            // Act - should not throw
            cacheEvictionService.evictListsByEntityName("User");

            // No exceptions should be thrown
            verify(cacheManager).getCache("lists");
        }

        @Test
        @DisplayName("should fallback to clear all for non-Caffeine cache")
        void shouldFallbackToClearAllForNonCaffeineCache() {
            ConcurrentMapCache nonCaffeineCache = new ConcurrentMapCache("lists");
            nonCaffeineCache.put("User:all", "value1");
            nonCaffeineCache.put("Product:all", "value2");

            when(cacheManager.getCache("lists")).thenReturn(nonCaffeineCache);

            // Act
            cacheEvictionService.evictListsByEntityName("User");

            // Assert - all entries cleared (fallback behavior)
            assertThat(nonCaffeineCache.getNativeCache()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Evict Entity")
    class EvictEntityTests {

        @Test
        @DisplayName("should evict specific entity from cache")
        void shouldEvictSpecificEntityFromCache() {
            entitiesNativeCache.put("User:1", "user-1");
            entitiesNativeCache.put("User:2", "user-2");
            entitiesNativeCache.put("Product:1", "product-1");

            when(cacheManager.getCache("entities")).thenReturn(entitiesCaffeineCache);

            // Act
            cacheEvictionService.evictEntity("User", 1);

            // Assert
            assertThat(entitiesNativeCache.getIfPresent("User:1")).isNull();
            assertThat(entitiesNativeCache.getIfPresent("User:2")).isEqualTo("user-2");
            assertThat(entitiesNativeCache.getIfPresent("Product:1")).isEqualTo("product-1");
        }

        @Test
        @DisplayName("should handle missing cache gracefully")
        void shouldHandleMissingCacheGracefully() {
            when(cacheManager.getCache("entities")).thenReturn(null);

            // Act - should not throw
            cacheEvictionService.evictEntity("User", 1);

            verify(cacheManager).getCache("entities");
        }
    }

    @Nested
    @DisplayName("Evict Counts")
    class EvictCountsTests {

        @Test
        @DisplayName("should evict count entries for entity")
        void shouldEvictCountEntriesForEntity() {
            countsNativeCache.put("User:count", 100L);
            countsNativeCache.put("User:countActive", 80L);
            countsNativeCache.put("Product:count", 50L);

            when(cacheManager.getCache("counts")).thenReturn(countsCaffeineCache);

            // Act
            cacheEvictionService.evictCounts("User");

            // Assert
            assertThat(countsNativeCache.getIfPresent("User:count")).isNull();
            assertThat(countsNativeCache.getIfPresent("User:countActive")).isNull();
            assertThat(countsNativeCache.getIfPresent("Product:count")).isEqualTo(50L);
        }

        @Test
        @DisplayName("should handle missing cache gracefully")
        void shouldHandleMissingCacheGracefully() {
            when(cacheManager.getCache("counts")).thenReturn(null);

            // Act - should not throw
            cacheEvictionService.evictCounts("User");

            verify(cacheManager).getCache("counts");
        }
    }

    @Nested
    @DisplayName("Evict Entity And Lists")
    class EvictEntityAndListsTests {

        @Test
        @DisplayName("should evict both entity and lists")
        void shouldEvictBothEntityAndLists() {
            entitiesNativeCache.put("User:1", "user-1");
            listsNativeCache.put("User:all:0:20", "list-1");

            when(cacheManager.getCache("entities")).thenReturn(entitiesCaffeineCache);
            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);

            // Act
            cacheEvictionService.evictEntityAndLists("User", 1);

            // Assert
            assertThat(entitiesNativeCache.getIfPresent("User:1")).isNull();
            assertThat(listsNativeCache.getIfPresent("User:all:0:20")).isNull();
        }

        @Test
        @DisplayName("should skip entity eviction when id is null")
        void shouldSkipEntityEvictionWhenIdIsNull() {
            listsNativeCache.put("User:all:0:20", "list-1");

            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);

            // Act
            cacheEvictionService.evictEntityAndLists("User", null);

            // Assert - only lists evicted, entities not accessed
            assertThat(listsNativeCache.getIfPresent("User:all:0:20")).isNull();
            verify(cacheManager, never()).getCache("entities");
        }
    }

    @Nested
    @DisplayName("Evict All")
    class EvictAllTests {

        @Test
        @DisplayName("should evict entity, lists, and counts")
        void shouldEvictEntityListsAndCounts() {
            entitiesNativeCache.put("User:1", "user-1");
            listsNativeCache.put("User:all:0:20", "list-1");
            countsNativeCache.put("User:count", 100L);

            when(cacheManager.getCache("entities")).thenReturn(entitiesCaffeineCache);
            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);
            when(cacheManager.getCache("counts")).thenReturn(countsCaffeineCache);

            // Act
            cacheEvictionService.evictAll("User", 1);

            // Assert - all caches evicted for User
            assertThat(entitiesNativeCache.getIfPresent("User:1")).isNull();
            assertThat(listsNativeCache.getIfPresent("User:all:0:20")).isNull();
            assertThat(countsNativeCache.getIfPresent("User:count")).isNull();
        }

        @Test
        @DisplayName("should preserve other entities caches")
        void shouldPreserveOtherEntitiesCaches() {
            entitiesNativeCache.put("User:1", "user-1");
            entitiesNativeCache.put("Product:1", "product-1");
            listsNativeCache.put("User:all", "user-list");
            listsNativeCache.put("Product:all", "product-list");
            countsNativeCache.put("User:count", 100L);
            countsNativeCache.put("Product:count", 50L);

            when(cacheManager.getCache("entities")).thenReturn(entitiesCaffeineCache);
            when(cacheManager.getCache("lists")).thenReturn(listsCaffeineCache);
            when(cacheManager.getCache("counts")).thenReturn(countsCaffeineCache);

            // Act
            cacheEvictionService.evictAll("User", 1);

            // Assert - User evicted, Product preserved
            assertThat(entitiesNativeCache.getIfPresent("User:1")).isNull();
            assertThat(entitiesNativeCache.getIfPresent("Product:1")).isEqualTo("product-1");
            assertThat(listsNativeCache.getIfPresent("User:all")).isNull();
            assertThat(listsNativeCache.getIfPresent("Product:all")).isEqualTo("product-list");
            assertThat(countsNativeCache.getIfPresent("User:count")).isNull();
            assertThat(countsNativeCache.getIfPresent("Product:count")).isEqualTo(50L);
        }
    }
}
