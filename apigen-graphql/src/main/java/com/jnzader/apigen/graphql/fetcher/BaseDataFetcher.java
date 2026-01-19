package com.jnzader.apigen.graphql.fetcher;

import com.jnzader.apigen.graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import java.util.Optional;
import org.dataloader.DataLoader;

/**
 * Base class for GraphQL data fetchers.
 *
 * <p>Provides convenient access to the GraphQL context, arguments, and DataLoaders.
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * @Component
 * public class ProductByIdFetcher extends BaseDataFetcher<Product> {
 *
 *     private final ProductRepository productRepository;
 *
 *     public ProductByIdFetcher(ProductRepository productRepository) {
 *         this.productRepository = productRepository;
 *     }
 *
 *     @Override
 *     protected Product fetch(DataFetchingEnvironment env) {
 *         String id = getRequiredArgument(env, "id");
 *         return productRepository.findById(id)
 *             .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
 *     }
 * }
 * }</pre>
 *
 * @param <T> the return type of the data fetcher
 */
public abstract class BaseDataFetcher<T> implements DataFetcher<T> {

    @Override
    public T get(DataFetchingEnvironment environment) throws Exception {
        return fetch(environment);
    }

    /**
     * Fetches the data for this query/mutation.
     *
     * @param env the data fetching environment
     * @return the fetched data
     * @throws Exception if an error occurs during fetching
     */
    protected abstract T fetch(DataFetchingEnvironment env) throws Exception;

    /**
     * Gets the GraphQL context from the environment.
     *
     * @param env the data fetching environment
     * @return the GraphQL context
     */
    protected GraphQLContext getContext(DataFetchingEnvironment env) {
        return env.getGraphQlContext().get(GraphQLContext.class);
    }

    /**
     * Gets an argument value.
     *
     * @param env the data fetching environment
     * @param name the argument name
     * @param <V> the argument type
     * @return the argument value, or null if not present
     */
    protected <V> V getArgument(DataFetchingEnvironment env, String name) {
        return env.getArgument(name);
    }

    /**
     * Gets an optional argument value.
     *
     * @param env the data fetching environment
     * @param name the argument name
     * @param <V> the argument type
     * @return the argument value wrapped in Optional
     */
    protected <V> Optional<V> getOptionalArgument(DataFetchingEnvironment env, String name) {
        return Optional.ofNullable(env.getArgument(name));
    }

    /**
     * Gets a required argument value.
     *
     * @param env the data fetching environment
     * @param name the argument name
     * @param <V> the argument type
     * @return the argument value
     * @throws IllegalArgumentException if the argument is not present
     */
    protected <V> V getRequiredArgument(DataFetchingEnvironment env, String name) {
        V value = env.getArgument(name);
        if (value == null) {
            throw new IllegalArgumentException("Required argument '" + name + "' is missing");
        }
        return value;
    }

    /**
     * Gets all arguments as a map.
     *
     * @param env the data fetching environment
     * @return map of argument names to values
     */
    protected Map<String, Object> getArguments(DataFetchingEnvironment env) {
        return env.getArguments();
    }

    /**
     * Gets a DataLoader by name.
     *
     * @param env the data fetching environment
     * @param name the DataLoader name
     * @param <K> the key type
     * @param <V> the value type
     * @return the DataLoader
     */
    protected <K, V> DataLoader<K, V> getDataLoader(DataFetchingEnvironment env, String name) {
        return env.getDataLoader(name);
    }

    /**
     * Gets the source (parent) object for nested queries.
     *
     * @param env the data fetching environment
     * @param <S> the source type
     * @return the source object
     */
    protected <S> S getSource(DataFetchingEnvironment env) {
        return env.getSource();
    }

    /**
     * Gets the current user ID from the context.
     *
     * @param env the data fetching environment
     * @return the user ID, or empty if not authenticated
     */
    protected Optional<String> getCurrentUserId(DataFetchingEnvironment env) {
        GraphQLContext context = getContext(env);
        return context != null ? context.getUserId() : Optional.empty();
    }
}
