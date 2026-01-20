package com.jnzader.apigen.graphql.execution;

import static graphql.Scalars.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.graphql.GraphQLContext;
import com.jnzader.apigen.graphql.schema.SchemaBuilder;
import graphql.ExecutionResult;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQLExecutor Tests")
class GraphQLExecutorTest {

    private GraphQLExecutor executor;

    @BeforeEach
    void setUp() {
        GraphQLSchema schema =
                SchemaBuilder.newSchema()
                        .type("Product")
                        .field("id", GraphQLID, true)
                        .field("name", GraphQLString, true)
                        .field("price", GraphQLFloat)
                        .endType()
                        .query("product")
                        .argument("id", GraphQLID, true)
                        .returns("Product")
                        .fetcher(
                                env -> {
                                    String id = env.getArgument("id");
                                    return Map.of(
                                            "id", id, "name", "Product " + id, "price", 99.99);
                                })
                        .endQuery()
                        .query("products")
                        .argument("limit", GraphQLInt)
                        .returnsList("Product")
                        .fetcher(
                                env -> {
                                    Integer limit = env.getArgument("limit");
                                    if (limit == null) limit = 10;
                                    return List.of(
                                            Map.of("id", "1", "name", "Product 1", "price", 10.0),
                                            Map.of("id", "2", "name", "Product 2", "price", 20.0));
                                })
                        .endQuery()
                        .query("contextUser")
                        .returns(GraphQLString)
                        .fetcher(
                                env -> {
                                    GraphQLContext context =
                                            env.getGraphQlContext().get(GraphQLContext.class);
                                    return context != null
                                            ? context.getUserId().orElse("anonymous")
                                            : "no-context";
                                })
                        .endQuery()
                        .build();

        executor = GraphQLExecutor.builder(schema).build();
    }

    @Nested
    @DisplayName("Query Execution")
    class QueryExecutionTests {

        @Test
        @DisplayName("should execute simple query")
        void shouldExecuteSimpleQuery() {
            ExecutionResult result = executor.execute("{ product(id: \"123\") { id name } }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) data.get("product");
            assertThat(product).containsEntry("id", "123").containsEntry("name", "Product 123");
        }

        @Test
        @DisplayName("should execute query with variables")
        void shouldExecuteQueryWithVariables() {
            String query = "query GetProduct($id: ID!) { product(id: $id) { id name price } }";
            Map<String, Object> variables = Map.of("id", "456");
            GraphQLContext context = GraphQLContext.builder().build();

            ExecutionResult result = executor.execute(query, "GetProduct", variables, context);

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) data.get("product");
            assertThat(product).containsEntry("id", "456");
        }

        @Test
        @DisplayName("should execute list query")
        void shouldExecuteListQuery() {
            ExecutionResult result = executor.execute("{ products { id name } }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("products");
            assertThat(products).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Context Handling")
    class ContextHandlingTests {

        @Test
        @DisplayName("should pass context to data fetchers")
        void shouldPassContextToDataFetchers() {
            GraphQLContext context = GraphQLContext.builder().userId("user-123").build();

            ExecutionResult result = executor.execute("{ contextUser }", null, null, context);

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data).containsEntry("contextUser", "user-123");
        }

        @Test
        @DisplayName("should handle missing user in context")
        void shouldHandleMissingUserInContext() {
            GraphQLContext context = GraphQLContext.builder().build();

            ExecutionResult result = executor.execute("{ contextUser }", null, null, context);

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data).containsEntry("contextUser", "anonymous");
        }

        @Test
        @DisplayName("should execute with locale")
        void shouldExecuteWithLocale() {
            ExecutionResult result =
                    executor.execute("{ product(id: \"1\") { id } }", Locale.GERMAN);

            assertThat(result.getErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Async Execution")
    class AsyncExecutionTests {

        @Test
        @DisplayName("should execute query asynchronously")
        void shouldExecuteQueryAsynchronously() {
            GraphQLContext context = GraphQLContext.builder().build();

            CompletableFuture<ExecutionResult> future =
                    executor.executeAsync(
                            "{ product(id: \"async-1\") { id name } }", null, null, context);

            ExecutionResult result = future.join();

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) data.get("product");
            assertThat(product).containsEntry("id", "async-1");
        }
    }
}
