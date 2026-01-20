package com.jnzader.apigen.graphql.schema;

import static graphql.Scalars.*;
import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaBuilder Tests")
class SchemaBuilderTest {

    @Nested
    @DisplayName("Type Building")
    class TypeBuildingTests {

        @Test
        @DisplayName("should build object type with scalar fields")
        void shouldBuildObjectTypeWithScalarFields() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .type("Product")
                            .field("id", GraphQLID, true)
                            .field("name", GraphQLString, true)
                            .field("price", GraphQLFloat)
                            .field("active", GraphQLBoolean)
                            .endType()
                            .query("dummy")
                            .returns(GraphQLString)
                            .fetcher(env -> "test")
                            .endQuery()
                            .build();

            GraphQLObjectType productType = (GraphQLObjectType) schema.getType("Product");

            assertThat(productType).isNotNull();
            assertThat(productType.getFieldDefinition("id")).isNotNull();
            assertThat(productType.getFieldDefinition("name")).isNotNull();
            assertThat(productType.getFieldDefinition("price")).isNotNull();
            assertThat(productType.getFieldDefinition("active")).isNotNull();
        }

        @Test
        @DisplayName("should build type with description")
        void shouldBuildTypeWithDescription() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .type("Product")
                            .description("A product in the catalog")
                            .field("id", GraphQLID, true)
                            .endType()
                            .query("dummy")
                            .returns(GraphQLString)
                            .fetcher(env -> "test")
                            .endQuery()
                            .build();

            GraphQLObjectType productType = (GraphQLObjectType) schema.getType("Product");

            assertThat(productType.getDescription()).isEqualTo("A product in the catalog");
        }
    }

    @Nested
    @DisplayName("Query Building")
    class QueryBuildingTests {

        @Test
        @DisplayName("should build query with arguments")
        void shouldBuildQueryWithArguments() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("greeting")
                            .argument("name", GraphQLString, true)
                            .returns(GraphQLString)
                            .fetcher(env -> "Hello, " + env.getArgument("name"))
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ greeting(name: \"World\") }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            assertThat(data).containsEntry("greeting", "Hello, World");
        }

        @Test
        @DisplayName("should build query returning list")
        void shouldBuildQueryReturningList() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .type("Product")
                            .field("id", GraphQLID, true)
                            .field("name", GraphQLString, true)
                            .endType()
                            .query("products")
                            .returnsList("Product")
                            .fetcher(
                                    env ->
                                            List.of(
                                                    Map.of("id", "1", "name", "Product 1"),
                                                    Map.of("id", "2", "name", "Product 2")))
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result = graphQL.execute("{ products { id name } }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> products = (List<Map<String, Object>>) data.get("products");
            assertThat(products).hasSize(2);
            assertThat(products.get(0)).containsEntry("name", "Product 1");
        }

        @Test
        @DisplayName("should support optional arguments")
        void shouldSupportOptionalArguments() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .query("search")
                            .argument("query", GraphQLString, true)
                            .argument("limit", GraphQLInt)
                            .returns(GraphQLString)
                            .fetcher(
                                    env -> {
                                        Integer limit = env.getArgument("limit");
                                        return "Found " + (limit != null ? limit : 10) + " results";
                                    })
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();

            // Without optional argument
            ExecutionResult result1 = graphQL.execute("{ search(query: \"test\") }");
            assertThat(result1.getErrors()).isEmpty();
            Map<String, Object> data1 = result1.getData();
            assertThat(data1).containsEntry("search", "Found 10 results");

            // With optional argument
            ExecutionResult result2 = graphQL.execute("{ search(query: \"test\", limit: 5) }");
            assertThat(result2.getErrors()).isEmpty();
            Map<String, Object> data2 = result2.getData();
            assertThat(data2).containsEntry("search", "Found 5 results");
        }
    }

    @Nested
    @DisplayName("Mutation Building")
    class MutationBuildingTests {

        @Test
        @DisplayName("should build mutation")
        void shouldBuildMutation() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .type("Product")
                            .field("id", GraphQLID, true)
                            .field("name", GraphQLString, true)
                            .endType()
                            .mutation("createProduct")
                            .argument("name", GraphQLString, true)
                            .returns("Product")
                            .fetcher(env -> Map.of("id", "new-1", "name", env.getArgument("name")))
                            .endMutation()
                            .query("dummy")
                            .returns(GraphQLString)
                            .fetcher(env -> "")
                            .endQuery()
                            .build();

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionResult result =
                    graphQL.execute(
                            "mutation { createProduct(name: \"New Product\") { id name } }");

            assertThat(result.getErrors()).isEmpty();
            Map<String, Object> data = result.getData();
            @SuppressWarnings("unchecked")
            Map<String, Object> product = (Map<String, Object>) data.get("createProduct");
            assertThat(product).containsEntry("id", "new-1").containsEntry("name", "New Product");
        }
    }

    @Nested
    @DisplayName("Input Types")
    class InputTypeTests {

        @Test
        @DisplayName("should build input type")
        void shouldBuildInputType() {
            GraphQLSchema schema =
                    SchemaBuilder.newSchema()
                            .inputType("CreateProductInput")
                            .description("Input for creating a product")
                            .field("name", GraphQLString, true)
                            .field("price", GraphQLFloat, true)
                            .field("description", GraphQLString)
                            .endInputType()
                            .query("dummy")
                            .returns(GraphQLString)
                            .fetcher(env -> "test")
                            .endQuery()
                            .build();

            assertThat(schema.getType("CreateProductInput")).isNotNull();
        }
    }
}
