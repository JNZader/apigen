package com.jnzader.apigen.graphql.fetcher;

import static graphql.Scalars.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.graphql.GraphQLContext;
import com.jnzader.apigen.graphql.schema.SchemaBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BaseDataFetcher Tests")
class BaseDataFetcherTest {

    @Nested
    @DisplayName("Argument Handling")
    class ArgumentHandlingTests {

        @Test
        @DisplayName("should get argument value")
        void shouldGetArgumentValue() {
            TestDataFetcher fetcher = new TestDataFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .argument("name", GraphQLString, true)
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ test(name: \"World\") }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data.get("test")).isEqualTo("Hello, World");
        }

        @Test
        @DisplayName("should get optional argument")
        void shouldGetOptionalArgument() {
            OptionalArgFetcher fetcher = new OptionalArgFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .argument("name", GraphQLString)
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();

            // With argument
            ExecutionResult result1 = graphQL.execute("{ test(name: \"World\") }");
            assertThat(result1.getErrors()).isEmpty();
            Map<String, Object> data1 = result1.getData();
            assertThat(data1.get("test")).isEqualTo("Hello, World");

            // Without argument
            ExecutionResult result2 = graphQL.execute("{ test }");
            assertThat(result2.getErrors()).isEmpty();
            Map<String, Object> data2 = result2.getData();
            assertThat(data2.get("test")).isEqualTo("Hello, Guest");
        }

        @Test
        @DisplayName("should throw for missing required argument")
        void shouldThrowForMissingRequiredArgument() {
            RequiredArgFetcher fetcher = new RequiredArgFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .argument("name", GraphQLString)
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ test }");

            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("should get all arguments")
        void shouldGetAllArguments() {
            AllArgsFetcher fetcher = new AllArgsFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .argument("a", GraphQLString, true)
                            .argument("b", GraphQLInt, true)
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ test(a: \"hello\", b: 42) }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data.get("test")).isEqualTo("a=hello, b=42");
        }
    }

    @Nested
    @DisplayName("Context Handling")
    class ContextHandlingTests {

        @Test
        @DisplayName("should get user ID from context")
        void shouldGetUserIdFromContext() {
            UserIdFetcher fetcher = new UserIdFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();

            graphql.ExecutionInput input =
                    graphql.ExecutionInput.newExecutionInput()
                            .query("{ test }")
                            .graphQLContext(
                                    builder ->
                                            builder.of(
                                                    GraphQLContext.class,
                                                    GraphQLContext.builder()
                                                            .userId("user-123")
                                                            .build()))
                            .build();

            ExecutionResult result = graphQL.execute(input);

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data.get("test")).isEqualTo("user-123");
        }

        @Test
        @DisplayName("should handle missing context")
        void shouldHandleMissingContext() {
            UserIdFetcher fetcher = new UserIdFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ test }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data.get("test")).isEqualTo("anonymous");
        }
    }

    @Nested
    @DisplayName("DataLoader Access")
    class DataLoaderAccessTests {

        @Test
        @DisplayName("should get data loader from environment")
        void shouldGetDataLoaderFromEnvironment() {
            DataLoaderFetcher fetcher = new DataLoaderFetcher();

            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("test")
                            .returns(GraphQLString)
                            .fetcher(fetcher)
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ test }");

            // DataLoader won't be present, but method should handle it gracefully
            assertThat(result.getErrors()).isEmpty();
        }
    }

    // Test implementations

    static class TestDataFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            String name = getArgument(env, "name");
            return "Hello, " + name;
        }
    }

    static class OptionalArgFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            Optional<String> name = getOptionalArgument(env, "name");
            return "Hello, " + name.orElse("Guest");
        }
    }

    static class RequiredArgFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            String name = getRequiredArgument(env, "name");
            return "Hello, " + name;
        }
    }

    static class AllArgsFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            Map<String, Object> args = getArguments(env);
            return "a=" + args.get("a") + ", b=" + args.get("b");
        }
    }

    static class UserIdFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            return getCurrentUserId(env).orElse("anonymous");
        }
    }

    static class DataLoaderFetcher extends BaseDataFetcher<String> {
        @Override
        protected String fetch(DataFetchingEnvironment env) {
            // Try to get a non-existent data loader - should return null gracefully
            Object loader = getDataLoader(env, "nonexistent");
            return loader == null ? "no-loader" : "has-loader";
        }
    }
}
