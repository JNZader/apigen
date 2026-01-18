package com.jnzader.apigen.server.service.generator.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringTransformationUtil Tests")
class StringTransformationUtilTest {

    @Nested
    @DisplayName("capitalize()")
    class CapitalizeTests {

        @ParameterizedTest
        @CsvSource({
                "hello, Hello",
                "world, World",
                "test, Test",
                "a, A",
                "ABC, ABC"
        })
        @DisplayName("Should capitalize first letter")
        void shouldCapitalizeFirstLetter(String input, String expected) {
            assertThat(StringTransformationUtil.capitalize(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(StringTransformationUtil.capitalize(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty string for empty input")
        void shouldReturnEmptyStringForEmptyInput() {
            assertThat(StringTransformationUtil.capitalize("")).isEmpty();
        }

        @Test
        @DisplayName("Should handle already capitalized string")
        void shouldHandleAlreadyCapitalizedString() {
            assertThat(StringTransformationUtil.capitalize("Hello")).isEqualTo("Hello");
        }

        @Test
        @DisplayName("Should handle single character")
        void shouldHandleSingleCharacter() {
            assertThat(StringTransformationUtil.capitalize("x")).isEqualTo("X");
        }
    }

    @Nested
    @DisplayName("toCamelCase()")
    class ToCamelCaseTests {

        @ParameterizedTest
        @CsvSource({
                "user_id, userId",
                "created_at, createdAt",
                "first_name, firstName",
                "last_modified_date, lastModifiedDate",
                "simple, simple",
                "UPPER_CASE, upperCase",
                "Mixed_Case, mixedCase"
        })
        @DisplayName("Should convert snake_case to camelCase")
        void shouldConvertSnakeCaseToCamelCase(String input, String expected) {
            assertThat(StringTransformationUtil.toCamelCase(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            assertThat(StringTransformationUtil.toCamelCase("")).isEmpty();
        }

        @Test
        @DisplayName("Should handle single word")
        void shouldHandleSingleWord() {
            assertThat(StringTransformationUtil.toCamelCase("name")).isEqualTo("name");
        }

        @Test
        @DisplayName("Should handle consecutive underscores")
        void shouldHandleConsecutiveUnderscores() {
            assertThat(StringTransformationUtil.toCamelCase("user__id")).isEqualTo("userId");
        }

        @Test
        @DisplayName("Should handle leading underscore")
        void shouldHandleLeadingUnderscore() {
            assertThat(StringTransformationUtil.toCamelCase("_private")).isEqualTo("private");
        }

        @Test
        @DisplayName("Should handle trailing underscore")
        void shouldHandleTrailingUnderscore() {
            assertThat(StringTransformationUtil.toCamelCase("field_")).isEqualTo("field");
        }
    }

    @Nested
    @DisplayName("toPascalCase()")
    class ToPascalCaseTests {

        @ParameterizedTest
        @CsvSource({
                "my-app, MyApp",
                "user-service, UserService",
                "hello-world, HelloWorld",
                "simple, Simple",
                "a-b-c, ABC",
                "test, Test"
        })
        @DisplayName("Should convert kebab-case to PascalCase")
        void shouldConvertKebabCaseToPascalCase(String input, String expected) {
            assertThat(StringTransformationUtil.toPascalCase(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            assertThat(StringTransformationUtil.toPascalCase("")).isEmpty();
        }

        @Test
        @DisplayName("Should handle single character")
        void shouldHandleSingleCharacter() {
            assertThat(StringTransformationUtil.toPascalCase("a")).isEqualTo("A");
        }

        @Test
        @DisplayName("Should handle consecutive hyphens")
        void shouldHandleConsecutiveHyphens() {
            assertThat(StringTransformationUtil.toPascalCase("my--app")).isEqualTo("MyApp");
        }

        @Test
        @DisplayName("Should handle leading hyphen")
        void shouldHandleLeadingHyphen() {
            assertThat(StringTransformationUtil.toPascalCase("-test")).isEqualTo("Test");
        }

        @Test
        @DisplayName("Should handle trailing hyphen")
        void shouldHandleTrailingHyphen() {
            assertThat(StringTransformationUtil.toPascalCase("test-")).isEqualTo("Test");
        }
    }

    @Nested
    @DisplayName("escapeJsonString()")
    class EscapeJsonStringTests {

        @Test
        @DisplayName("Should escape backslash")
        void shouldEscapeBackslash() {
            String result = StringTransformationUtil.escapeJsonString("path\\to\\file");
            assertThat(result).isEqualTo("\"path\\\\to\\\\file\"");
        }

        @Test
        @DisplayName("Should escape double quotes")
        void shouldEscapeDoubleQuotes() {
            String result = StringTransformationUtil.escapeJsonString("He said \"hello\"");
            assertThat(result).isEqualTo("\"He said \\\"hello\\\"\"");
        }

        @Test
        @DisplayName("Should escape newline")
        void shouldEscapeNewline() {
            String result = StringTransformationUtil.escapeJsonString("line1\nline2");
            assertThat(result).isEqualTo("\"line1\\nline2\"");
        }

        @Test
        @DisplayName("Should escape carriage return")
        void shouldEscapeCarriageReturn() {
            String result = StringTransformationUtil.escapeJsonString("line1\rline2");
            assertThat(result).isEqualTo("\"line1\\rline2\"");
        }

        @Test
        @DisplayName("Should escape tab")
        void shouldEscapeTab() {
            String result = StringTransformationUtil.escapeJsonString("col1\tcol2");
            assertThat(result).isEqualTo("\"col1\\tcol2\"");
        }

        @Test
        @DisplayName("Should wrap result in quotes")
        void shouldWrapResultInQuotes() {
            String result = StringTransformationUtil.escapeJsonString("simple");
            assertThat(result).startsWith("\"").endsWith("\"");
        }

        @Test
        @DisplayName("Should handle multiple escape sequences")
        void shouldHandleMultipleEscapeSequences() {
            String result = StringTransformationUtil.escapeJsonString("line1\nline2\t\"quoted\"\\path");
            assertThat(result).isEqualTo("\"line1\\nline2\\t\\\"quoted\\\"\\\\path\"");
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            String result = StringTransformationUtil.escapeJsonString("");
            assertThat(result).isEqualTo("\"\"");
        }
    }
}
