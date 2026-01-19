package com.jnzader.apigen.codegen.generator.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CodeGenerationUtils Tests")
class CodeGenerationUtilsTest {

    @Nested
    @DisplayName("safeFieldName()")
    class SafeFieldNameTests {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "class",
                    "public",
                    "private",
                    "static",
                    "final",
                    "void",
                    "int",
                    "boolean",
                    "if",
                    "else",
                    "for",
                    "while",
                    "switch",
                    "case",
                    "return",
                    "new",
                    "this",
                    "super",
                    "abstract",
                    "interface",
                    "enum",
                    "extends",
                    "implements",
                    "import",
                    "package",
                    "try",
                    "catch",
                    "finally",
                    "throw",
                    "throws",
                    "synchronized",
                    "volatile",
                    "transient",
                    "native",
                    "strictfp",
                    "assert",
                    "default",
                    "break",
                    "continue",
                    "do",
                    "instanceof",
                    "goto",
                    "const",
                    "var",
                    "yield",
                    "record",
                    "sealed",
                    "permits"
                })
        @DisplayName("Should suffix Java keywords with 'Field'")
        void shouldSuffixJavaKeywords(String keyword) {
            String result = CodeGenerationUtils.safeFieldName(keyword);
            assertThat(result).isEqualTo(keyword + "Field");
        }

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "name",
                    "value",
                    "count",
                    "status",
                    "description",
                    "userId",
                    "createdAt"
                })
        @DisplayName("Should return non-keywords unchanged")
        void shouldReturnNonKeywordsUnchanged(String name) {
            String result = CodeGenerationUtils.safeFieldName(name);
            assertThat(result).isEqualTo(name);
        }

        @Test
        @DisplayName("Should handle case-insensitive keyword detection")
        void shouldHandleCaseInsensitiveKeywordDetection() {
            assertThat(CodeGenerationUtils.safeFieldName("CLASS")).isEqualTo("CLASSField");
            assertThat(CodeGenerationUtils.safeFieldName("Class")).isEqualTo("ClassField");
            assertThat(CodeGenerationUtils.safeFieldName("PUBLIC")).isEqualTo("PUBLICField");
        }
    }

    @Nested
    @DisplayName("toSnakeCase()")
    class ToSnakeCaseTests {

        @ParameterizedTest
        @CsvSource({
            "camelCase, camel_case",
            "PascalCase, pascal_case",
            "userId, user_id",
            "createdAt, created_at",
            "XMLParser, xmlparser",
            "getHTTPResponse, get_httpresponse",
            "simple, simple",
            "ALLCAPS, allcaps"
        })
        @DisplayName("Should convert camelCase to snake_case")
        void shouldConvertCamelCaseToSnakeCase(String input, String expected) {
            String result = CodeGenerationUtils.toSnakeCase(input);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should handle single character")
        void shouldHandleSingleCharacter() {
            assertThat(CodeGenerationUtils.toSnakeCase("a")).isEqualTo("a");
            assertThat(CodeGenerationUtils.toSnakeCase("A")).isEqualTo("a");
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            assertThat(CodeGenerationUtils.toSnakeCase("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("pluralize()")
    class PluralizeTests {

        @ParameterizedTest
        @CsvSource({"user, users", "product, products", "order, orders", "item, items"})
        @DisplayName("Should add 's' for regular nouns")
        void shouldAddSForRegularNouns(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({
            "category, categories",
            "company, companies",
            "entity, entities",
            "city, cities",
            "country, countries"
        })
        @DisplayName("Should change 'y' to 'ies' for consonant+y endings")
        void shouldChangeYToIes(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"day, days", "key, keys", "boy, boys", "guy, guys", "array, arrays"})
        @DisplayName("Should add 's' for vowel+y endings")
        void shouldAddSForVowelYEndings(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"class, classes", "bus, buses", "address, addresses", "process, processes"})
        @DisplayName("Should add 'es' for words ending in 's'")
        void shouldAddEsForSEndings(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"box, boxes", "tax, taxes", "index, indexes"})
        @DisplayName("Should add 'es' for words ending in 'x'")
        void shouldAddEsForXEndings(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"match, matches", "batch, batches", "search, searches"})
        @DisplayName("Should add 'es' for words ending in 'ch'")
        void shouldAddEsForChEndings(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"dish, dishes", "push, pushes", "flash, flashes"})
        @DisplayName("Should add 'es' for words ending in 'sh'")
        void shouldAddEsForShEndings(String singular, String expected) {
            assertThat(CodeGenerationUtils.pluralize(singular)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("isJavaKeyword()")
    class IsJavaKeywordTests {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "class", "public", "private", "static", "void", "int", "boolean", "if", "else",
                    "for", "while", "return", "new", "try", "catch"
                })
        @DisplayName("Should return true for Java keywords")
        void shouldReturnTrueForKeywords(String keyword) {
            assertThat(CodeGenerationUtils.isJavaKeyword(keyword)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "value", "user", "product", "order", "count"})
        @DisplayName("Should return false for non-keywords")
        void shouldReturnFalseForNonKeywords(String name) {
            assertThat(CodeGenerationUtils.isJavaKeyword(name)).isFalse();
        }

        @Test
        @DisplayName("Should be case-insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(CodeGenerationUtils.isJavaKeyword("CLASS")).isTrue();
            assertThat(CodeGenerationUtils.isJavaKeyword("Public")).isTrue();
            assertThat(CodeGenerationUtils.isJavaKeyword("PRIVATE")).isTrue();
        }
    }
}
