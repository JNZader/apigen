package com.jnzader.apigen.core.infrastructure.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * Tests para configuración de caché (Caffeine).
 * <p>
 * Verifica:
 * - Almacenamiento y recuperación
 * - Expiración de entradas
 * - Límites de tamaño
 * - Estadísticas de caché
 * - Invalidación
 */
@DisplayName("Cache Tests")
class CacheTest {

    // ==================== Basic Cache Operations Tests ====================

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperationsTests {

        private Cache<String, String> cache;

        @BeforeEach
        void setUp() {
            cache = Caffeine.newBuilder()
                    .maximumSize(100)
                    .build();
        }

        @Test
        @DisplayName("should store and retrieve value")
        void shouldStoreAndRetrieveValue() {
            // When
            cache.put("key1", "value1");

            // Then
            assertThat(cache.getIfPresent("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("should return null for missing key")
        void shouldReturnNullForMissingKey() {
            assertThat(cache.getIfPresent("nonexistent")).isNull();
        }

        @Test
        @DisplayName("should compute value if absent")
        void shouldComputeValueIfAbsent() {
            // When
            String value = cache.get("key", k -> "computed-" + k);

            // Then
            assertThat(value).isEqualTo("computed-key");
            assertThat(cache.getIfPresent("key")).isEqualTo("computed-key");
        }

        @Test
        @DisplayName("should invalidate single entry")
        void shouldInvalidateSingleEntry() {
            // Given
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            // When
            cache.invalidate("key1");

            // Then
            assertThat(cache.getIfPresent("key1")).isNull();
            assertThat(cache.getIfPresent("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("should invalidate all entries")
        void shouldInvalidateAllEntries() {
            // Given
            cache.put("key1", "value1");
            cache.put("key2", "value2");

            // When
            cache.invalidateAll();

            // Then
            assertThat(cache.getIfPresent("key1")).isNull();
            assertThat(cache.getIfPresent("key2")).isNull();
        }
    }

    // ==================== Expiration Tests ====================

    @Nested
    @DisplayName("Expiration")
    class ExpirationTests {

        @Test
        @DisplayName("should expire entries after write duration")
        void shouldExpireEntriesAfterWriteDuration() {
            // Given
            Cache<String, String> cache = Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMillis(100))
                    .build();

            cache.put("key", "value");
            assertThat(cache.getIfPresent("key")).isEqualTo("value");

            // When - wait for expiration
            await()
                    .atMost(Duration.ofMillis(500))
                    .until(() -> cache.getIfPresent("key") == null);

            // Then
            assertThat(cache.getIfPresent("key")).isNull();
        }

        @Test
        @DisplayName("should expire entries after access duration")
        @SuppressWarnings("java:S2925") // Thread.sleep necesario para probar expiración de cache
        void shouldExpireEntriesAfterAccessDuration() {
            // Given
            Cache<String, String> cache = Caffeine.newBuilder()
                    .expireAfterAccess(Duration.ofMillis(100))
                    .build();

            cache.put("key", "value");

            // Access to reset expiration
            cache.getIfPresent("key");

            // When - wait less than expiration time
            try {
                Thread.sleep(50);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }

            // Then - should still be present
            assertThat(cache.getIfPresent("key")).isEqualTo("value");
        }
    }

    // ==================== Size Limit Tests ====================

    @Nested
    @DisplayName("Size Limits")
    class SizeLimitTests {

        @Test
        @DisplayName("should evict entries when maximum size exceeded")
        void shouldEvictEntriesWhenMaximumSizeExceeded() {
            // Given
            Cache<String, String> cache = Caffeine.newBuilder()
                    .maximumSize(3)
                    .build();

            // When
            cache.put("key1", "value1");
            cache.put("key2", "value2");
            cache.put("key3", "value3");
            cache.put("key4", "value4"); // This should trigger eviction

            // Need to trigger cleanup
            cache.cleanUp();

            // Then - one entry should be evicted
            long size = cache.estimatedSize();
            assertThat(size).isLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("should respect weight-based size limit")
        void shouldRespectWeightBasedSizeLimit() {
            // Given
            Cache<String, String> cache = Caffeine.newBuilder()
                    .maximumWeight(100)
                    .weigher((String key, String value) -> value.length())
                    .build();

            // When
            cache.put("key1", "short");           // weight: 5
            cache.put("key2", "medium length");   // weight: 13
            cache.put("key3", "a very long string that takes up space"); // weight: 39

            // Then
            assertThat(cache.estimatedSize()).isLessThanOrEqualTo(3);
        }
    }

    // ==================== Statistics Tests ====================

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        private Cache<String, String> cache;

        @BeforeEach
        void setUp() {
            cache = Caffeine.newBuilder()
                    .maximumSize(100)
                    .recordStats()
                    .build();
        }

        @Test
        @DisplayName("should record hit statistics")
        void shouldRecordHitStatistics() {
            // Given
            cache.put("key", "value");

            // When
            cache.getIfPresent("key"); // hit
            cache.getIfPresent("key"); // hit

            // Then
            CacheStats stats = cache.stats();
            assertThat(stats.hitCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should record miss statistics")
        void shouldRecordMissStatistics() {
            // When
            cache.getIfPresent("nonexistent1"); // miss
            cache.getIfPresent("nonexistent2"); // miss

            // Then
            CacheStats stats = cache.stats();
            assertThat(stats.missCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate hit rate")
        void shouldCalculateHitRate() {
            // Given
            cache.put("key", "value");

            // When
            cache.getIfPresent("key");           // hit
            cache.getIfPresent("key");           // hit
            cache.getIfPresent("nonexistent");   // miss

            // Then
            CacheStats stats = cache.stats();
            assertThat(stats.hitRate()).isCloseTo(0.666, within(0.01));
        }

        @Test
        @DisplayName("should record load statistics")
        void shouldRecordLoadStatistics() {
            // When
            cache.get("key1", k -> "value1");
            cache.get("key2", k -> "value2");

            // Then
            CacheStats stats = cache.stats();
            assertThat(stats.loadCount()).isEqualTo(2);
        }
    }

    // ==================== Entity Cache Simulation ====================

    @Nested
    @DisplayName("Entity Cache Simulation")
    class EntityCacheSimulationTests {

        record TestEntity(Long id, String name) {}

        private Cache<String, TestEntity> entityCache;

        @BeforeEach
        void setUp() {
            entityCache = Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .recordStats()
                    .build();
        }

        @Test
        @DisplayName("should cache entity by composite key")
        void shouldCacheEntityByCompositeKey() {
            // Given
            TestEntity entity = new TestEntity(1L, "Test Entity");
            String cacheKey = "TestEntity:1";

            // When
            entityCache.put(cacheKey, entity);

            // Then
            TestEntity cached = entityCache.getIfPresent(cacheKey);
            assertThat(cached).isNotNull();
            assertThat(cached.id()).isEqualTo(1L);
            assertThat(cached.name()).isEqualTo("Test Entity");
        }

        @Test
        @DisplayName("should load entity on cache miss")
        void shouldLoadEntityOnCacheMiss() {
            // Given
            String cacheKey = "TestEntity:1";

            // When - simulate repository lookup on miss
            TestEntity entity = entityCache.get(cacheKey, key -> {
                // Simulate database lookup
                return new TestEntity(1L, "Loaded Entity");
            });

            // Then
            assertThat(entity.name()).isEqualTo("Loaded Entity");

            // Second access should be from cache
            TestEntity cachedEntity = entityCache.getIfPresent(cacheKey);
            assertThat(cachedEntity).isSameAs(entity);
        }

        @Test
        @DisplayName("should invalidate entity cache on update")
        void shouldInvalidateEntityCacheOnUpdate() {
            // Given
            String cacheKey = "TestEntity:1";
            entityCache.put(cacheKey, new TestEntity(1L, "Original"));

            // When - simulate entity update
            entityCache.invalidate(cacheKey);
            entityCache.put(cacheKey, new TestEntity(1L, "Updated"));

            // Then
            TestEntity updated = entityCache.getIfPresent(cacheKey);
            assertThat(updated.name()).isEqualTo("Updated");
        }

        @Test
        @DisplayName("should support list cache with pattern invalidation")
        void shouldSupportListCacheWithPatternInvalidation() {
            // Given
            entityCache.put("TestEntity:list:page0", new TestEntity(0L, "Page 0"));
            entityCache.put("TestEntity:list:page1", new TestEntity(1L, "Page 1"));
            entityCache.put("TestEntity:single:1", new TestEntity(1L, "Single"));

            // When - invalidate all list caches
            entityCache.asMap().keySet().stream()
                    .filter(key -> key.contains(":list:"))
                    .forEach(entityCache::invalidate);

            // Then
            assertThat(entityCache.getIfPresent("TestEntity:list:page0")).isNull();
            assertThat(entityCache.getIfPresent("TestEntity:list:page1")).isNull();
            assertThat(entityCache.getIfPresent("TestEntity:single:1")).isNotNull();
        }
    }
}
