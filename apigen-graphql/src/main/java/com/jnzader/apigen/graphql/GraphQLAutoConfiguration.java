package com.jnzader.apigen.graphql;

import com.jnzader.apigen.graphql.dataloader.DataLoaderRegistrar;
import com.jnzader.apigen.graphql.execution.GraphQLExecutor;
import com.jnzader.apigen.graphql.http.GraphQLController;
import graphql.schema.GraphQLSchema;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for APiGen GraphQL module.
 *
 * <p>Enabled when {@code apigen.graphql.enabled=true} in application properties.
 *
 * <p>Configuration properties:
 *
 * <ul>
 *   <li>{@code apigen.graphql.enabled} - Enable GraphQL support (default: false)
 *   <li>{@code apigen.graphql.path} - GraphQL endpoint path (default: /graphql)
 *   <li>{@code apigen.graphql.tracing.enabled} - Enable tracing (default: false)
 *   <li>{@code apigen.graphql.http.enabled} - Enable HTTP controller (default: true)
 * </ul>
 *
 * <p>To use this auto-configuration, provide a {@link GraphQLSchema} bean:
 *
 * <pre>{@code
 * @Configuration
 * public class MyGraphQLConfig {
 *
 *     @Bean
 *     public GraphQLSchema graphQLSchema() {
 *         return SchemaBuilder.newSchema()
 *             .type("Product")
 *                 .field("id", GraphQLID, true)
 *                 .field("name", GraphQLString, true)
 *             .endType()
 *             .query("products")
 *                 .returnsList("Product")
 *                 .fetcher(env -> productService.findAll())
 *             .endQuery()
 *             .build();
 *     }
 * }
 * }</pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "apigen.graphql", name = "enabled", havingValue = "true")
public class GraphQLAutoConfiguration {

    @Value("${apigen.graphql.tracing.enabled:false}")
    private boolean tracingEnabled;

    /**
     * Creates the GraphQL executor bean.
     *
     * @param schema the GraphQL schema
     * @param dataLoaderRegistrars the DataLoader registrars (optional)
     * @return the executor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(GraphQLSchema.class)
    public GraphQLExecutor graphQLExecutor(
            GraphQLSchema schema, List<DataLoaderRegistrar> dataLoaderRegistrars) {
        GraphQLExecutor.Builder builder =
                GraphQLExecutor.builder(schema).dataLoaderRegistrars(dataLoaderRegistrars);

        if (tracingEnabled) {
            builder.enableTracing();
        }

        return builder.build();
    }

    /**
     * Creates the GraphQL HTTP controller.
     *
     * @param executor the GraphQL executor
     * @return the controller
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(GraphQLExecutor.class)
    @ConditionalOnProperty(
            prefix = "apigen.graphql.http",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public GraphQLController graphQLController(GraphQLExecutor executor) {
        return new GraphQLController(executor);
    }
}
