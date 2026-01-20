package com.jnzader.apigen.graphql.execution;

import com.jnzader.apigen.graphql.GraphQLContext;
import com.jnzader.apigen.graphql.dataloader.DataLoaderRegistrar;
import com.jnzader.apigen.graphql.dataloader.DataLoaderRegistry;
import com.jnzader.apigen.graphql.error.GraphQLExceptionHandler;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.tracing.TracingInstrumentation;
import graphql.schema.GraphQLSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Executor for GraphQL queries, mutations, and subscriptions.
 *
 * <p>Handles execution with proper context, DataLoaders, error handling, and instrumentation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/graphql")
 * public class GraphQLController {
 *
 *     private final GraphQLExecutor executor;
 *
 *     @PostMapping
 *     public Map<String, Object> execute(@RequestBody GraphQLRequest request) {
 *         GraphQLContext context = GraphQLContext.builder()
 *             .userId(getCurrentUserId())
 *             .locale(getLocale())
 *             .build();
 *
 *         return executor.execute(
 *             request.getQuery(),
 *             request.getOperationName(),
 *             request.getVariables(),
 *             context
 *         ).toSpecification();
 *     }
 * }
 * }</pre>
 */
public class GraphQLExecutor {

    private final GraphQL graphQL;
    private final List<DataLoaderRegistrar> dataLoaderRegistrars;
    private final boolean tracingEnabled;

    private GraphQLExecutor(Builder builder) {
        this.dataLoaderRegistrars = builder.dataLoaderRegistrars;
        this.tracingEnabled = builder.tracingEnabled;

        List<Instrumentation> instrumentations = new ArrayList<>();
        if (tracingEnabled) {
            instrumentations.add(new TracingInstrumentation());
        }
        instrumentations.addAll(builder.instrumentations);

        GraphQL.Builder graphQLBuilder =
                GraphQL.newGraphQL(builder.schema)
                        .queryExecutionStrategy(
                                new AsyncExecutionStrategy(new GraphQLExceptionHandler()))
                        .mutationExecutionStrategy(
                                new AsyncExecutionStrategy(new GraphQLExceptionHandler()));

        if (!instrumentations.isEmpty()) {
            graphQLBuilder.instrumentation(new ChainedInstrumentation(instrumentations));
        }

        this.graphQL = graphQLBuilder.build();
    }

    public static Builder builder(GraphQLSchema schema) {
        return new Builder(schema);
    }

    /**
     * Executes a GraphQL query synchronously.
     *
     * @param query the GraphQL query string
     * @param operationName the operation name (optional)
     * @param variables the variables (optional)
     * @param context the GraphQL context
     * @return the execution result
     */
    public ExecutionResult execute(
            String query,
            String operationName,
            Map<String, Object> variables,
            GraphQLContext context) {
        return executeAsync(query, operationName, variables, context).join();
    }

    /**
     * Executes a GraphQL query asynchronously.
     *
     * @param query the GraphQL query string
     * @param operationName the operation name (optional)
     * @param variables the variables (optional)
     * @param context the GraphQL context
     * @return a CompletableFuture with the execution result
     */
    public CompletableFuture<ExecutionResult> executeAsync(
            String query,
            String operationName,
            Map<String, Object> variables,
            GraphQLContext context) {

        DataLoaderRegistry dataLoaderRegistry = createDataLoaderRegistry();

        ExecutionInput.Builder inputBuilder =
                ExecutionInput.newExecutionInput()
                        .query(query)
                        .dataLoaderRegistry(dataLoaderRegistry.getRegistry());

        if (operationName != null) {
            inputBuilder.operationName(operationName);
        }

        if (variables != null) {
            inputBuilder.variables(variables);
        }

        if (context != null) {
            inputBuilder.graphQLContext(builder -> builder.of(GraphQLContext.class, context));
        }

        return graphQL.executeAsync(inputBuilder.build());
    }

    /**
     * Executes a simple query without operation name or variables.
     *
     * @param query the GraphQL query string
     * @return the execution result
     */
    public ExecutionResult execute(String query) {
        return execute(query, null, null, GraphQLContext.builder().build());
    }

    /**
     * Executes a query with locale for i18n support.
     *
     * @param query the GraphQL query string
     * @param locale the locale for error messages
     * @return the execution result
     */
    public ExecutionResult execute(String query, Locale locale) {
        return execute(query, null, null, GraphQLContext.builder().locale(locale).build());
    }

    private DataLoaderRegistry createDataLoaderRegistry() {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        for (DataLoaderRegistrar registrar : dataLoaderRegistrars) {
            registrar.register(registry);
        }
        return registry;
    }

    public static class Builder {
        private final GraphQLSchema schema;
        private final List<DataLoaderRegistrar> dataLoaderRegistrars = new ArrayList<>();
        private final List<Instrumentation> instrumentations = new ArrayList<>();
        private boolean tracingEnabled = false;

        Builder(GraphQLSchema schema) {
            this.schema = schema;
        }

        public Builder dataLoaderRegistrar(DataLoaderRegistrar registrar) {
            this.dataLoaderRegistrars.add(registrar);
            return this;
        }

        public Builder dataLoaderRegistrars(List<DataLoaderRegistrar> registrars) {
            this.dataLoaderRegistrars.addAll(registrars);
            return this;
        }

        public Builder instrumentation(Instrumentation instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder enableTracing() {
            this.tracingEnabled = true;
            return this;
        }

        public GraphQLExecutor build() {
            return new GraphQLExecutor(this);
        }
    }
}
