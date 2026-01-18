package com.jnzader.apigen.core.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ETagGenerator Tests")
class ETagGeneratorTest {

    @Nested
    @DisplayName("generate")
    class GenerateTests {

        @Test
        @DisplayName("should generate ETag for object")
        void shouldGenerateEtagForObject() {
            var obj = new TestObject("test", 123);
            String etag = ETagGenerator.generate(obj);

            assertThat(etag)
                    .isNotNull()
                    .startsWith("\"")
                    .endsWith("\"");
        }

        @Test
        @DisplayName("should return null for null object")
        void shouldReturnNullForNullObject() {
            String etag = ETagGenerator.generate(null);
            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should generate same ETag for identical objects")
        void shouldGenerateSameEtagForIdenticalObjects() {
            var obj1 = new TestObject("test", 123);
            var obj2 = new TestObject("test", 123);

            String etag1 = ETagGenerator.generate(obj1);
            String etag2 = ETagGenerator.generate(obj2);

            assertThat(etag1).isEqualTo(etag2);
        }

        @Test
        @DisplayName("should generate different ETags for different objects")
        void shouldGenerateDifferentEtagsForDifferentObjects() {
            var obj1 = new TestObject("test1", 123);
            var obj2 = new TestObject("test2", 456);

            String etag1 = ETagGenerator.generate(obj1);
            String etag2 = ETagGenerator.generate(obj2);

            assertThat(etag1).isNotEqualTo(etag2);
        }
    }

    @Nested
    @DisplayName("generateWeak")
    class GenerateWeakTests {

        @Test
        @DisplayName("should generate weak ETag")
        void shouldGenerateWeakEtag() {
            var obj = new TestObject("test", 123);
            String etag = ETagGenerator.generateWeak(obj);

            assertThat(etag)
                    .isNotNull()
                    .startsWith("W/\"");
        }

        @Test
        @DisplayName("should return null for null object")
        void shouldReturnNullForNullObject() {
            String etag = ETagGenerator.generateWeak(null);
            assertThat(etag).isNull();
        }
    }

    @Nested
    @DisplayName("matches")
    class MatchesTests {

        @Test
        @DisplayName("should match identical ETags")
        void shouldMatchIdenticalEtags() {
            assertThat(ETagGenerator.matches("\"abc123\"", "\"abc123\"")).isTrue();
        }

        @Test
        @DisplayName("should match strong and weak ETags with same value")
        void shouldMatchStrongAndWeakEtags() {
            assertThat(ETagGenerator.matches("\"abc123\"", "W/\"abc123\"")).isTrue();
            assertThat(ETagGenerator.matches("W/\"abc123\"", "\"abc123\"")).isTrue();
        }

        @Test
        @DisplayName("should not match different ETags")
        void shouldNotMatchDifferentEtags() {
            assertThat(ETagGenerator.matches("\"abc123\"", "\"xyz789\"")).isFalse();
        }

        @Test
        @DisplayName("should return false for null ETags")
        void shouldReturnFalseForNullEtags() {
            assertThat(ETagGenerator.matches(null, "\"abc123\"")).isFalse();
            assertThat(ETagGenerator.matches("\"abc123\"", null)).isFalse();
            assertThat(ETagGenerator.matches(null, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("matchesIfNoneMatch")
    class MatchesIfNoneMatchTests {

        @Test
        @DisplayName("should match when ETag is in list")
        void shouldMatchWhenEtagInList() {
            assertThat(ETagGenerator.matchesIfNoneMatch("\"abc\"", "\"abc\", \"def\"")).isTrue();
        }

        @Test
        @DisplayName("should match wildcard")
        void shouldMatchWildcard() {
            assertThat(ETagGenerator.matchesIfNoneMatch("\"abc\"", "*")).isTrue();
        }

        @Test
        @DisplayName("should not match when ETag not in list")
        void shouldNotMatchWhenEtagNotInList() {
            assertThat(ETagGenerator.matchesIfNoneMatch("\"xyz\"", "\"abc\", \"def\"")).isFalse();
        }

        @Test
        @DisplayName("should return false for null or blank")
        void shouldReturnFalseForNullOrBlank() {
            assertThat(ETagGenerator.matchesIfNoneMatch(null, "\"abc\"")).isFalse();
            assertThat(ETagGenerator.matchesIfNoneMatch("\"abc\"", null)).isFalse();
            assertThat(ETagGenerator.matchesIfNoneMatch("\"abc\"", "   ")).isFalse();
        }
    }

    @Nested
    @DisplayName("matchesIfMatch")
    class MatchesIfMatchTests {

        @Test
        @DisplayName("should match when ETag is in list")
        void shouldMatchWhenEtagInList() {
            assertThat(ETagGenerator.matchesIfMatch("\"abc\"", "\"abc\", \"def\"")).isTrue();
        }

        @Test
        @DisplayName("should match wildcard")
        void shouldMatchWildcard() {
            assertThat(ETagGenerator.matchesIfMatch("\"abc\"", "*")).isTrue();
        }

        @Test
        @DisplayName("should return true when If-Match is null or blank")
        void shouldReturnTrueWhenIfMatchNullOrBlank() {
            assertThat(ETagGenerator.matchesIfMatch("\"abc\"", null)).isTrue();
            assertThat(ETagGenerator.matchesIfMatch("\"abc\"", "   ")).isTrue();
        }

        @Test
        @DisplayName("should return false when current ETag is null but If-Match is present")
        void shouldReturnFalseWhenCurrentEtagNull() {
            assertThat(ETagGenerator.matchesIfMatch(null, "\"abc\"")).isFalse();
        }

        @Test
        @DisplayName("should not match when ETag not in list")
        void shouldNotMatchWhenEtagNotInList() {
            assertThat(ETagGenerator.matchesIfMatch("\"xyz\"", "\"abc\", \"def\"")).isFalse();
        }
    }

    // Test object for serialization
    record TestObject(String name, int value) {}
}
