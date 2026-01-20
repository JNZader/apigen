package com.jnzader.apigen.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQLContext Tests")
class GraphQLContextTest {

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create context with user ID")
        void shouldCreateContextWithUserId() {
            GraphQLContext context = GraphQLContext.builder().userId("user-123").build();

            assertThat(context.getUserId()).isPresent().contains("user-123");
        }

        @Test
        @DisplayName("should create context with locale")
        void shouldCreateContextWithLocale() {
            GraphQLContext context = GraphQLContext.builder().locale(Locale.FRENCH).build();

            assertThat(context.getLocale()).isEqualTo(Locale.FRENCH);
        }

        @Test
        @DisplayName("should use default locale when not specified")
        void shouldUseDefaultLocaleWhenNotSpecified() {
            GraphQLContext context = GraphQLContext.builder().build();

            assertThat(context.getLocale()).isEqualTo(Locale.getDefault());
        }

        @Test
        @DisplayName("should create context with attributes")
        void shouldCreateContextWithAttributes() {
            GraphQLContext context =
                    GraphQLContext.builder()
                            .attribute("key1", "value1")
                            .attribute("key2", 42)
                            .build();

            assertThat(context.<String>getAttribute("key1")).isPresent().contains("value1");
            assertThat(context.<Integer>getAttribute("key2")).isPresent().contains(42);
        }

        @Test
        @DisplayName("should return empty optional for missing user ID")
        void shouldReturnEmptyForMissingUserId() {
            GraphQLContext context = GraphQLContext.builder().build();

            assertThat(context.getUserId()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Attributes")
    class AttributeTests {

        @Test
        @DisplayName("should set and get attributes")
        void shouldSetAndGetAttributes() {
            GraphQLContext context = GraphQLContext.builder().build();

            context.setAttribute("dynamic", "value");

            assertThat(context.<String>getAttribute("dynamic")).isPresent().contains("value");
        }

        @Test
        @DisplayName("should check if attribute exists")
        void shouldCheckIfAttributeExists() {
            GraphQLContext context = GraphQLContext.builder().attribute("exists", true).build();

            assertThat(context.hasAttribute("exists")).isTrue();
            assertThat(context.hasAttribute("missing")).isFalse();
        }

        @Test
        @DisplayName("should return empty for missing attribute")
        void shouldReturnEmptyForMissingAttribute() {
            GraphQLContext context = GraphQLContext.builder().build();

            assertThat(context.getAttribute("nonexistent")).isEmpty();
        }
    }
}
