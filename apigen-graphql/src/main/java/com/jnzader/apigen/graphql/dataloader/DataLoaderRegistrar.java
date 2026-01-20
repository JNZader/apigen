package com.jnzader.apigen.graphql.dataloader;

/**
 * Interface for registering DataLoaders.
 *
 * <p>Implement this interface and declare as a Spring bean to automatically register DataLoaders
 * with the GraphQL execution.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @Component
 * public class ProductDataLoaderRegistrar implements DataLoaderRegistrar {
 *
 *     private final ProductRepository productRepository;
 *
 *     public ProductDataLoaderRegistrar(ProductRepository productRepository) {
 *         this.productRepository = productRepository;
 *     }
 *
 *     @Override
 *     public void register(DataLoaderRegistry registry) {
 *         registry.register("products", ids -> {
 *             List<Product> products = productRepository.findAllById(ids);
 *             return products.stream()
 *                 .collect(Collectors.toMap(Product::getId, Function.identity()));
 *         });
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface DataLoaderRegistrar {

    /**
     * Registers DataLoaders with the registry.
     *
     * @param registry the DataLoader registry
     */
    void register(DataLoaderRegistry registry);
}
