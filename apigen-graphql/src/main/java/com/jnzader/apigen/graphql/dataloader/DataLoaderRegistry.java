package com.jnzader.apigen.graphql.dataloader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderOptions;

/**
 * Registry for managing GraphQL DataLoaders.
 *
 * <p>DataLoaders batch and cache database calls within a single request, preventing N+1 query
 * problems.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @Component
 * public class ProductDataLoaderRegistrar implements DataLoaderRegistrar {
 *
 *     private final ProductRepository productRepository;
 *
 *     @Override
 *     public void register(DataLoaderRegistry registry) {
 *         registry.register("products", ids ->
 *             productRepository.findAllById(ids)
 *                 .stream()
 *                 .collect(Collectors.toMap(Product::getId, Function.identity()))
 *         );
 *     }
 * }
 * }</pre>
 */
public class DataLoaderRegistry {

    private final Map<String, DataLoader<?, ?>> loaders = new ConcurrentHashMap<>();
    private final org.dataloader.DataLoaderRegistry delegateRegistry;

    public DataLoaderRegistry() {
        this.delegateRegistry = new org.dataloader.DataLoaderRegistry();
    }

    /**
     * Registers a new DataLoader with the given name.
     *
     * @param name the loader name
     * @param batchLoadFunction function that loads entities by their IDs
     * @param <K> the key type
     * @param <V> the value type
     */
    public <K, V> void register(String name, Function<List<K>, Map<K, V>> batchLoadFunction) {
        BatchLoader<K, V> batchLoader =
                keys -> {
                    Map<K, V> results = batchLoadFunction.apply(keys);
                    List<V> values = keys.stream().map(results::get).toList();
                    return CompletableFuture.completedFuture(values);
                };

        DataLoaderOptions options = DataLoaderOptions.newOptions().setCachingEnabled(true);

        DataLoader<K, V> dataLoader = DataLoaderFactory.newDataLoader(batchLoader, options);
        loaders.put(name, dataLoader);
        delegateRegistry.register(name, dataLoader);
    }

    /**
     * Registers a DataLoader with an async batch load function.
     *
     * @param name the loader name
     * @param batchLoader the async batch loader
     * @param <K> the key type
     * @param <V> the value type
     */
    public <K, V> void registerAsync(String name, BatchLoader<K, V> batchLoader) {
        DataLoaderOptions options = DataLoaderOptions.newOptions().setCachingEnabled(true);
        DataLoader<K, V> dataLoader = DataLoaderFactory.newDataLoader(batchLoader, options);
        loaders.put(name, dataLoader);
        delegateRegistry.register(name, dataLoader);
    }

    /**
     * Gets a DataLoader by name.
     *
     * @param name the loader name
     * @param <K> the key type
     * @param <V> the value type
     * @return the DataLoader
     */
    @SuppressWarnings("unchecked")
    public <K, V> DataLoader<K, V> getDataLoader(String name) {
        return (DataLoader<K, V>) loaders.get(name);
    }

    /**
     * Checks if a DataLoader is registered.
     *
     * @param name the loader name
     * @return true if registered
     */
    public boolean hasDataLoader(String name) {
        return loaders.containsKey(name);
    }

    /**
     * Returns the underlying DataLoader registry for integration with GraphQL execution.
     *
     * @return the DataLoader registry
     */
    public org.dataloader.DataLoaderRegistry getRegistry() {
        return delegateRegistry;
    }

    /**
     * Dispatches all pending DataLoader requests.
     *
     * <p>Call this after processing a GraphQL request to ensure all batched loads are executed.
     */
    public void dispatchAll() {
        delegateRegistry.dispatchAll();
    }

    /** Clears all cached values from all DataLoaders. */
    public void clearAll() {
        loaders.values().forEach(DataLoader::clearAll);
    }
}
