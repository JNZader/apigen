package com.jnzader.apigen.graphql.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQLRequest Tests")
class GraphQLRequestTest {

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create request with all fields")
        void shouldCreateRequestWithAllFields() {
            Map<String, Object> variables = Map.of("id", "123");

            GraphQLRequest request =
                    new GraphQLRequest("{ product { id } }", "GetProduct", variables);

            assertThat(request.query()).isEqualTo("{ product { id } }");
            assertThat(request.operationName()).isEqualTo("GetProduct");
            assertThat(request.variables()).isEqualTo(variables);
        }

        @Test
        @DisplayName("should create request with null operation name and variables")
        void shouldCreateRequestWithNullOperationNameAndVariables() {
            GraphQLRequest request = new GraphQLRequest("{ product { id } }", null, null);

            assertThat(request.query()).isEqualTo("{ product { id } }");
            assertThat(request.operationName()).isNull();
            assertThat(request.variables()).isNull();
        }

        @Test
        @DisplayName("should throw for null query")
        void shouldThrowForNullQuery() {
            assertThatThrownBy(() -> new GraphQLRequest(null, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or empty");
        }

        @Test
        @DisplayName("should throw for blank query")
        void shouldThrowForBlankQuery() {
            assertThatThrownBy(() -> new GraphQLRequest("   ", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or empty");
        }

        @Test
        @DisplayName("should throw for empty query")
        void shouldThrowForEmptyQuery() {
            assertThatThrownBy(() -> new GraphQLRequest("", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Query cannot be null or empty");
        }
    }

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("should create request with query only")
        void shouldCreateRequestWithQueryOnly() {
            GraphQLRequest request = GraphQLRequest.of("{ products { id } }");

            assertThat(request.query()).isEqualTo("{ products { id } }");
            assertThat(request.operationName()).isNull();
            assertThat(request.variables()).isNull();
        }

        @Test
        @DisplayName("should create request with query and variables")
        void shouldCreateRequestWithQueryAndVariables() {
            Map<String, Object> variables = Map.of("limit", 10);

            GraphQLRequest request =
                    GraphQLRequest.of("{ products(limit: $limit) { id } }", variables);

            assertThat(request.query()).isEqualTo("{ products(limit: $limit) { id } }");
            assertThat(request.operationName()).isNull();
            assertThat(request.variables()).isEqualTo(variables);
        }
    }
}
