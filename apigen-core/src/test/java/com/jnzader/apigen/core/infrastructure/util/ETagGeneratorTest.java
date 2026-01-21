package com.jnzader.apigen.core.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.core.domain.entity.Base;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

            assertThat(etag).isNotNull().startsWith("\"").endsWith("\"");
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

            assertThat(etag).isNotNull().startsWith("W/\"");
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

    @Nested
    @DisplayName("generateFromVersion - Entity")
    class GenerateFromVersionEntityTests {

        @Test
        @DisplayName("should generate ETag from entity id and version")
        void shouldGenerateEtagFromEntityIdAndVersion() {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setVersion(5L);

            String etag = ETagGenerator.generateFromVersion(entity);

            assertThat(etag).isEqualTo("\"123:5\"");
        }

        @Test
        @DisplayName("should return null for null entity")
        void shouldReturnNullForNullEntity() {
            String etag = ETagGenerator.generateFromVersion((Base) null);
            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should return null for entity with null id")
        void shouldReturnNullForNullId() {
            TestEntity entity = new TestEntity();
            entity.setVersion(5L);

            String etag = ETagGenerator.generateFromVersion(entity);

            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should return null for entity with null version")
        void shouldReturnNullForNullVersion() {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setVersion(null);

            String etag = ETagGenerator.generateFromVersion(entity);

            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should generate different ETags for different versions")
        void shouldGenerateDifferentEtagsForDifferentVersions() {
            TestEntity entity1 = new TestEntity();
            entity1.setId(123L);
            entity1.setVersion(1L);

            TestEntity entity2 = new TestEntity();
            entity2.setId(123L);
            entity2.setVersion(2L);

            String etag1 = ETagGenerator.generateFromVersion(entity1);
            String etag2 = ETagGenerator.generateFromVersion(entity2);

            assertThat(etag1).isNotEqualTo(etag2);
        }
    }

    @Nested
    @DisplayName("generateFromVersion - id/version")
    class GenerateFromVersionIdVersionTests {

        @Test
        @DisplayName("should generate ETag from id and version")
        void shouldGenerateEtagFromIdAndVersion() {
            String etag = ETagGenerator.generateFromVersion(123L, 5L);
            assertThat(etag).isEqualTo("\"123:5\"");
        }

        @Test
        @DisplayName("should return null for null id")
        void shouldReturnNullForNullId() {
            String etag = ETagGenerator.generateFromVersion(null, 5L);
            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should return null for null version")
        void shouldReturnNullForNullVersion() {
            String etag = ETagGenerator.generateFromVersion(123L, null);
            assertThat(etag).isNull();
        }

        @Test
        @DisplayName("should generate valid ETag for version 0")
        void shouldGenerateValidEtagForVersionZero() {
            String etag = ETagGenerator.generateFromVersion(1L, 0L);
            assertThat(etag).isEqualTo("\"1:0\"");
        }
    }

    @Nested
    @DisplayName("generateWeakFromVersion")
    class GenerateWeakFromVersionTests {

        @Test
        @DisplayName("should generate weak ETag from entity")
        void shouldGenerateWeakEtagFromEntity() {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setVersion(5L);

            String etag = ETagGenerator.generateWeakFromVersion(entity);

            assertThat(etag).isEqualTo("W/\"123:5\"");
        }

        @Test
        @DisplayName("should return null for null entity")
        void shouldReturnNullForNullEntity() {
            String etag = ETagGenerator.generateWeakFromVersion(null);
            assertThat(etag).isNull();
        }
    }

    @Nested
    @DisplayName("version-based ETags matching")
    class VersionBasedMatchingTests {

        @Test
        @DisplayName("version-based ETags should match correctly")
        void versionBasedEtagsShouldMatchCorrectly() {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setVersion(5L);

            String etag1 = ETagGenerator.generateFromVersion(entity);
            String etag2 = ETagGenerator.generateFromVersion(123L, 5L);

            assertThat(ETagGenerator.matches(etag1, etag2)).isTrue();
        }

        @Test
        @DisplayName("strong and weak version-based ETags should match")
        void strongAndWeakVersionBasedEtagsShouldMatch() {
            TestEntity entity = new TestEntity();
            entity.setId(123L);
            entity.setVersion(5L);

            String strong = ETagGenerator.generateFromVersion(entity);
            String weak = ETagGenerator.generateWeakFromVersion(entity);

            assertThat(ETagGenerator.matches(strong, weak)).isTrue();
        }
    }

    // Test object for serialization
    record TestObject(String name, int value) {}

    // Test entity extending Base for version-based ETag tests
    static class TestEntity extends Base {}
}
