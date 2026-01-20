package com.jnzader.apigen.graphql.http;

import java.util.Map;

/**
 * GraphQL HTTP request body.
 *
 * <p>Represents a standard GraphQL request over HTTP as per the GraphQL spec.
 *
 * <p>Example JSON request:
 *
 * <pre>{@code
 * {
 *   "query": "query GetProduct($id: ID!) { product(id: $id) { id name price } }",
 *   "operationName": "GetProduct",
 *   "variables": { "id": "123" }
 * }
 * }</pre>
 */
public record GraphQLRequest(String query, String operationName, Map<String, Object> variables) {

    public GraphQLRequest {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
    }

    /**
     * Creates a simple request with just a query.
     *
     * @param query the GraphQL query
     * @return the request
     */
    public static GraphQLRequest of(String query) {
        return new GraphQLRequest(query, null, null);
    }

    /**
     * Creates a request with query and variables.
     *
     * @param query the GraphQL query
     * @param variables the variables
     * @return the request
     */
    public static GraphQLRequest of(String query, Map<String, Object> variables) {
        return new GraphQLRequest(query, null, variables);
    }
}
