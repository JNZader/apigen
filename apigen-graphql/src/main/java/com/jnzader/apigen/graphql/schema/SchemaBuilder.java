package com.jnzader.apigen.graphql.schema;

import graphql.schema.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for constructing GraphQL schemas.
 *
 * <p>Provides a convenient API for building GraphQL types, queries, mutations, and subscriptions.
 *
 * <p>Example:
 *
 * <pre>{@code
 * GraphQLSchema schema = SchemaBuilder.newSchema()
 *     .type("Product")
 *         .field("id", GraphQLID, true)
 *         .field("name", GraphQLString, true)
 *         .field("price", GraphQLFloat, true)
 *         .field("description", GraphQLString)
 *     .endType()
 *     .query("product")
 *         .argument("id", GraphQLID, true)
 *         .returns("Product")
 *         .fetcher(productByIdFetcher)
 *     .endQuery()
 *     .query("products")
 *         .argument("limit", GraphQLInt)
 *         .argument("offset", GraphQLInt)
 *         .returnsList("Product")
 *         .fetcher(productsListFetcher)
 *     .endQuery()
 *     .mutation("createProduct")
 *         .argument("input", "CreateProductInput", true)
 *         .returns("Product")
 *         .fetcher(createProductFetcher)
 *     .endMutation()
 *     .build();
 * }</pre>
 */
public class SchemaBuilder {

    private final Map<String, GraphQLObjectType.Builder> types = new LinkedHashMap<>();
    private final Map<String, GraphQLInputObjectType.Builder> inputTypes = new LinkedHashMap<>();
    private final GraphQLObjectType.Builder queryBuilder;
    private final GraphQLObjectType.Builder mutationBuilder;
    private final GraphQLObjectType.Builder subscriptionBuilder;

    private SchemaBuilder() {
        this.queryBuilder = GraphQLObjectType.newObject().name("Query");
        this.mutationBuilder = GraphQLObjectType.newObject().name("Mutation");
        this.subscriptionBuilder = GraphQLObjectType.newObject().name("Subscription");
    }

    public static SchemaBuilder newSchema() {
        return new SchemaBuilder();
    }

    /**
     * Starts building a new object type.
     *
     * @param name the type name
     * @return a TypeBuilder for fluent API
     */
    public TypeBuilder type(String name) {
        GraphQLObjectType.Builder builder = GraphQLObjectType.newObject().name(name);
        types.put(name, builder);
        return new TypeBuilder(this, builder);
    }

    /**
     * Starts building a new input type.
     *
     * @param name the input type name
     * @return an InputTypeBuilder for fluent API
     */
    public InputTypeBuilder inputType(String name) {
        GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject().name(name);
        inputTypes.put(name, builder);
        return new InputTypeBuilder(this, builder);
    }

    /**
     * Starts building a new query field.
     *
     * @param name the query name
     * @return a FieldBuilder for fluent API
     */
    public FieldBuilder query(String name) {
        return new FieldBuilder(this, queryBuilder, name, FieldBuilder.FieldType.QUERY);
    }

    /**
     * Starts building a new mutation field.
     *
     * @param name the mutation name
     * @return a FieldBuilder for fluent API
     */
    public FieldBuilder mutation(String name) {
        return new FieldBuilder(this, mutationBuilder, name, FieldBuilder.FieldType.MUTATION);
    }

    /**
     * Starts building a new subscription field.
     *
     * @param name the subscription name
     * @return a FieldBuilder for fluent API
     */
    public FieldBuilder subscription(String name) {
        return new FieldBuilder(
                this, subscriptionBuilder, name, FieldBuilder.FieldType.SUBSCRIPTION);
    }

    /**
     * Builds the final GraphQL schema.
     *
     * @return the constructed GraphQL schema
     */
    public GraphQLSchema build() {
        GraphQLSchema.Builder schemaBuilder = GraphQLSchema.newSchema();

        // Build types
        GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();

        // Add query type
        GraphQLObjectType queryType = queryBuilder.build();
        if (!queryType.getFieldDefinitions().isEmpty()) {
            schemaBuilder.query(queryType);
        }

        // Add mutation type
        GraphQLObjectType mutationType = mutationBuilder.build();
        if (!mutationType.getFieldDefinitions().isEmpty()) {
            schemaBuilder.mutation(mutationType);
        }

        // Add subscription type
        GraphQLObjectType subscriptionType = subscriptionBuilder.build();
        if (!subscriptionType.getFieldDefinitions().isEmpty()) {
            schemaBuilder.subscription(subscriptionType);
        }

        // Add additional types
        for (GraphQLObjectType.Builder typeBuilder : types.values()) {
            schemaBuilder.additionalType(typeBuilder.build());
        }

        for (GraphQLInputObjectType.Builder inputBuilder : inputTypes.values()) {
            schemaBuilder.additionalType(inputBuilder.build());
        }

        return schemaBuilder.codeRegistry(codeRegistryBuilder.build()).build();
    }

    GraphQLTypeReference getTypeReference(String typeName) {
        return GraphQLTypeReference.typeRef(typeName);
    }

    /** Builder for object types. */
    public static class TypeBuilder {
        private final SchemaBuilder schemaBuilder;
        private final GraphQLObjectType.Builder typeBuilder;

        TypeBuilder(SchemaBuilder schemaBuilder, GraphQLObjectType.Builder typeBuilder) {
            this.schemaBuilder = schemaBuilder;
            this.typeBuilder = typeBuilder;
        }

        public TypeBuilder description(String description) {
            typeBuilder.description(description);
            return this;
        }

        public TypeBuilder field(String name, GraphQLScalarType type) {
            return field(name, type, false);
        }

        public TypeBuilder field(String name, GraphQLScalarType type, boolean nonNull) {
            GraphQLOutputType outputType = nonNull ? GraphQLNonNull.nonNull(type) : type;
            typeBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition().name(name).type(outputType));
            return this;
        }

        public TypeBuilder field(String name, String typeName) {
            return field(name, typeName, false);
        }

        public TypeBuilder field(String name, String typeName, boolean nonNull) {
            GraphQLOutputType outputType =
                    nonNull
                            ? GraphQLNonNull.nonNull(schemaBuilder.getTypeReference(typeName))
                            : schemaBuilder.getTypeReference(typeName);
            typeBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition().name(name).type(outputType));
            return this;
        }

        public TypeBuilder listField(String name, GraphQLScalarType type) {
            typeBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name(name)
                            .type(GraphQLList.list(type)));
            return this;
        }

        public TypeBuilder listField(String name, String typeName) {
            typeBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                            .name(name)
                            .type(GraphQLList.list(schemaBuilder.getTypeReference(typeName))));
            return this;
        }

        public SchemaBuilder endType() {
            return schemaBuilder;
        }
    }

    /** Builder for input types. */
    public static class InputTypeBuilder {
        private final SchemaBuilder schemaBuilder;
        private final GraphQLInputObjectType.Builder inputBuilder;

        InputTypeBuilder(SchemaBuilder schemaBuilder, GraphQLInputObjectType.Builder inputBuilder) {
            this.schemaBuilder = schemaBuilder;
            this.inputBuilder = inputBuilder;
        }

        public InputTypeBuilder description(String description) {
            inputBuilder.description(description);
            return this;
        }

        public InputTypeBuilder field(String name, GraphQLScalarType type) {
            return field(name, type, false);
        }

        public InputTypeBuilder field(String name, GraphQLScalarType type, boolean required) {
            GraphQLInputType inputType = required ? GraphQLNonNull.nonNull(type) : type;
            inputBuilder.field(
                    GraphQLInputObjectField.newInputObjectField().name(name).type(inputType));
            return this;
        }

        public SchemaBuilder endInputType() {
            return schemaBuilder;
        }
    }

    /** Builder for query, mutation, and subscription fields. */
    public static class FieldBuilder {
        enum FieldType {
            QUERY,
            MUTATION,
            SUBSCRIPTION
        }

        private final SchemaBuilder schemaBuilder;
        private final GraphQLObjectType.Builder parentBuilder;
        private final GraphQLFieldDefinition.Builder fieldBuilder;
        private final FieldType fieldType;

        FieldBuilder(
                SchemaBuilder schemaBuilder,
                GraphQLObjectType.Builder parentBuilder,
                String name,
                FieldType fieldType) {
            this.schemaBuilder = schemaBuilder;
            this.parentBuilder = parentBuilder;
            this.fieldBuilder = GraphQLFieldDefinition.newFieldDefinition().name(name);
            this.fieldType = fieldType;
        }

        public FieldBuilder description(String description) {
            fieldBuilder.description(description);
            return this;
        }

        public FieldBuilder argument(String name, GraphQLScalarType type) {
            return argument(name, type, false);
        }

        public FieldBuilder argument(String name, GraphQLScalarType type, boolean required) {
            GraphQLInputType inputType = required ? GraphQLNonNull.nonNull(type) : type;
            fieldBuilder.argument(GraphQLArgument.newArgument().name(name).type(inputType));
            return this;
        }

        public FieldBuilder argument(String name, String typeName, boolean required) {
            GraphQLInputType inputType =
                    required
                            ? GraphQLNonNull.nonNull(schemaBuilder.getTypeReference(typeName))
                            : schemaBuilder.getTypeReference(typeName);
            fieldBuilder.argument(GraphQLArgument.newArgument().name(name).type(inputType));
            return this;
        }

        public FieldBuilder returns(GraphQLScalarType type) {
            fieldBuilder.type(type);
            return this;
        }

        public FieldBuilder returns(String typeName) {
            fieldBuilder.type(schemaBuilder.getTypeReference(typeName));
            return this;
        }

        public FieldBuilder returnsList(String typeName) {
            fieldBuilder.type(GraphQLList.list(schemaBuilder.getTypeReference(typeName)));
            return this;
        }

        public FieldBuilder returnsNonNull(String typeName) {
            fieldBuilder.type(GraphQLNonNull.nonNull(schemaBuilder.getTypeReference(typeName)));
            return this;
        }

        public FieldBuilder fetcher(DataFetcher<?> fetcher) {
            fieldBuilder.dataFetcher(fetcher);
            return this;
        }

        public SchemaBuilder endQuery() {
            parentBuilder.field(fieldBuilder);
            return schemaBuilder;
        }

        public SchemaBuilder endMutation() {
            parentBuilder.field(fieldBuilder);
            return schemaBuilder;
        }

        public SchemaBuilder endSubscription() {
            parentBuilder.field(fieldBuilder);
            return schemaBuilder;
        }
    }
}
