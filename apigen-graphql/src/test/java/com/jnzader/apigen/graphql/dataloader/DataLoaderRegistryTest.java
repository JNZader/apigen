package com.jnzader.apigen.graphql.dataloader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.dataloader.DataLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DataLoaderRegistry Tests")
class DataLoaderRegistryTest {

    private DataLoaderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DataLoaderRegistry();
    }

    private Function<List<String>, Map<String, String>> createBatchFunction() {
        return ids -> {
            Map<String, String> result = new HashMap<>();
            for (String id : ids) {
                result.put(id, "Product " + id);
            }
            return result;
        };
    }

    @Nested
    @DisplayName("Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register DataLoader with batch function")
        void shouldRegisterDataLoaderWithBatchFunction() {
            registry.<String, String>register("products", createBatchFunction());

            assertThat(registry.hasDataLoader("products")).isTrue();
        }

        @Test
        @DisplayName("should register async DataLoader")
        void shouldRegisterAsyncDataLoader() {
            registry.<String, String>registerAsync(
                    "asyncProducts",
                    ids ->
                            CompletableFuture.supplyAsync(
                                    () -> ids.stream().map(id -> "Async " + id).toList()));

            assertThat(registry.hasDataLoader("asyncProducts")).isTrue();
        }

        @Test
        @DisplayName("should return false for unregistered loader")
        void shouldReturnFalseForUnregisteredLoader() {
            assertThat(registry.hasDataLoader("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("Loading")
    class LoadingTests {

        @Test
        @DisplayName("should load single value")
        void shouldLoadSingleValue() {
            Function<List<String>, Map<String, String>> batchFn =
                    ids -> {
                        Map<String, String> result = new HashMap<>();
                        for (String id : ids) {
                            result.put(id, "Item " + id);
                        }
                        return result;
                    };
            registry.<String, String>register("items", batchFn);

            DataLoader<String, String> loader = registry.getDataLoader("items");
            CompletableFuture<String> future = loader.load("1");
            registry.dispatchAll();

            assertThat(future.join()).isEqualTo("Item 1");
        }

        @Test
        @DisplayName("should batch multiple loads")
        void shouldBatchMultipleLoads() {
            AtomicInteger batchCount = new AtomicInteger(0);

            Function<List<String>, Map<String, String>> batchFn =
                    ids -> {
                        batchCount.incrementAndGet();
                        Map<String, String> result = new HashMap<>();
                        for (String id : ids) {
                            result.put(id, "Batched " + id);
                        }
                        return result;
                    };
            registry.<String, String>register("batched", batchFn);

            DataLoader<String, String> loader = registry.getDataLoader("batched");

            // Queue multiple loads
            CompletableFuture<String> f1 = loader.load("a");
            CompletableFuture<String> f2 = loader.load("b");
            CompletableFuture<String> f3 = loader.load("c");

            // Dispatch all at once
            registry.dispatchAll();

            assertThat(f1.join()).isEqualTo("Batched a");
            assertThat(f2.join()).isEqualTo("Batched b");
            assertThat(f3.join()).isEqualTo("Batched c");

            // Should have been called only once due to batching
            assertThat(batchCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("should cache loaded values")
        void shouldCacheLoadedValues() {
            AtomicInteger loadCount = new AtomicInteger(0);

            Function<List<String>, Map<String, String>> batchFn =
                    ids -> {
                        loadCount.addAndGet(ids.size());
                        Map<String, String> result = new HashMap<>();
                        for (String id : ids) {
                            result.put(id, "Cached " + id);
                        }
                        return result;
                    };
            registry.<String, String>register("cached", batchFn);

            DataLoader<String, String> loader = registry.getDataLoader("cached");

            // First load
            CompletableFuture<String> f1 = loader.load("x");
            registry.dispatchAll();
            assertThat(f1.join()).isEqualTo("Cached x");

            // Second load of same key - should be cached
            CompletableFuture<String> f2 = loader.load("x");
            registry.dispatchAll();
            assertThat(f2.join()).isEqualTo("Cached x");

            // Only loaded once from source
            assertThat(loadCount.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagementTests {

        @Test
        @DisplayName("should clear all caches")
        void shouldClearAllCaches() {
            AtomicInteger loadCount = new AtomicInteger(0);

            Function<List<String>, Map<String, String>> batchFn =
                    ids -> {
                        loadCount.incrementAndGet();
                        Map<String, String> result = new HashMap<>();
                        for (String id : ids) {
                            result.put(id, "Value " + id);
                        }
                        return result;
                    };
            registry.<String, String>register("clearable", batchFn);

            DataLoader<String, String> loader = registry.getDataLoader("clearable");

            // First load - wait for async completion
            CompletableFuture<String> f1 = loader.load("key");
            registry.dispatchAll();
            f1.join();

            // Clear cache
            registry.clearAll();

            // Load again - should hit source
            CompletableFuture<String> f2 = loader.load("key");
            registry.dispatchAll();
            f2.join();

            assertThat(loadCount.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Integration")
    class IntegrationTests {

        @Test
        @DisplayName("should provide native DataLoader registry")
        void shouldProvideNativeRegistry() {
            Function<List<String>, Map<String, String>> batchFn =
                    ids -> {
                        Map<String, String> result = new HashMap<>();
                        for (String id : ids) {
                            result.put(id, "Test " + id);
                        }
                        return result;
                    };
            registry.<String, String>register("test", batchFn);

            org.dataloader.DataLoaderRegistry nativeRegistry = registry.getRegistry();

            assertThat(nativeRegistry).isNotNull();
            assertThat(nativeRegistry.getDataLoader("test")).isNotNull();
        }
    }
}
