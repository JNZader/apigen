package com.jnzader.apigen.core.infrastructure.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FieldAccessorCache Tests")
class FieldAccessorCacheTest {

    @AfterEach
    void tearDown() {
        FieldAccessorCache.clearCache();
    }

    @Nested
    @DisplayName("getFieldValues")
    class GetFieldValuesTests {

        @Test
        @DisplayName("should return empty map for null object")
        void shouldReturnEmptyMapForNullObject() {
            Map<String, Object> result = FieldAccessorCache.getFieldValues(null, Set.of("name"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should get field values from POJO")
        void shouldGetFieldValuesFromPojo() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);

            Map<String, Object> result = FieldAccessorCache.getFieldValues(pojo, Set.of("name", "age"));

            assertThat(result)
                    .containsEntry("id", 1L)
                    .containsEntry("name", "Test")
                    .containsEntry("age", 25);
        }

        @Test
        @DisplayName("should get field values from record")
        void shouldGetFieldValuesFromRecord() {
            TestRecord testRecord = new TestRecord(1L, "Record", true);

            Map<String, Object> result = FieldAccessorCache.getFieldValues(testRecord, Set.of("name", "active"));

            assertThat(result)
                    .containsEntry("id", 1L)
                    .containsEntry("name", "Record")
                    .containsEntry("active", true);
        }

        @Test
        @DisplayName("should always include id field first")
        void shouldAlwaysIncludeIdFieldFirst() {
            TestPojo pojo = new TestPojo(42L, "Test", 30);

            Map<String, Object> result = FieldAccessorCache.getFieldValues(pojo, Set.of("name"));

            assertThat(result.keySet().iterator().next()).isEqualTo("id");
        }

        @Test
        @DisplayName("should skip missing fields")
        void shouldSkipMissingFields() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);

            Map<String, Object> result = FieldAccessorCache.getFieldValues(pojo, Set.of("nonExistent"));

            assertThat(result).containsOnlyKeys("id");
        }
    }

    @Nested
    @DisplayName("getAllFieldValues")
    class GetAllFieldValuesTests {

        @Test
        @DisplayName("should return empty map for null object")
        void shouldReturnEmptyMapForNullObject() {
            Map<String, Object> result = FieldAccessorCache.getAllFieldValues(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should get all field values")
        void shouldGetAllFieldValues() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);

            Map<String, Object> result = FieldAccessorCache.getAllFieldValues(pojo);

            assertThat(result).containsKeys("id", "name", "age");
        }
    }

    @Nested
    @DisplayName("getFieldValue")
    class GetFieldValueTests {

        @Test
        @DisplayName("should return null for null object")
        void shouldReturnNullForNullObject() {
            Object result = FieldAccessorCache.getFieldValue(null, "name");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for null field name")
        void shouldReturnNullForNullFieldName() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);
            Object result = FieldAccessorCache.getFieldValue(pojo, null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return field value")
        void shouldReturnFieldValue() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);

            Object result = FieldAccessorCache.getFieldValue(pojo, "name");

            assertThat(result).isEqualTo("Test");
        }

        @Test
        @DisplayName("should return null for non-existent field")
        void shouldReturnNullForNonExistentField() {
            TestPojo pojo = new TestPojo(1L, "Test", 25);

            Object result = FieldAccessorCache.getFieldValue(pojo, "nonExistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getAvailableFieldNames")
    class GetAvailableFieldNamesTests {

        @Test
        @DisplayName("should return field names for POJO")
        void shouldReturnFieldNamesForPojo() {
            Set<String> result = FieldAccessorCache.getAvailableFieldNames(TestPojo.class);

            assertThat(result).contains("id", "name", "age");
        }

        @Test
        @DisplayName("should return field names for record")
        void shouldReturnFieldNamesForRecord() {
            Set<String> result = FieldAccessorCache.getAvailableFieldNames(TestRecord.class);

            assertThat(result).contains("id", "name", "active");
        }
    }

    @Nested
    @DisplayName("hasField")
    class HasFieldTests {

        @Test
        @DisplayName("should return true for existing field")
        void shouldReturnTrueForExistingField() {
            assertThat(FieldAccessorCache.hasField(TestPojo.class, "name")).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existing field")
        void shouldReturnFalseForNonExistingField() {
            assertThat(FieldAccessorCache.hasField(TestPojo.class, "nonExistent")).isFalse();
        }
    }

    @Nested
    @DisplayName("Cache operations")
    class CacheOperationsTests {

        @Test
        @DisplayName("should cache accessors")
        void shouldCacheAccessors() {
            // First call creates cache
            FieldAccessorCache.getFieldValues(new TestPojo(1L, "Test", 25), Set.of("name"));

            String stats = FieldAccessorCache.getCacheStats();

            assertThat(stats).contains("1 classes cached");
        }

        @Test
        @DisplayName("should clear cache")
        void shouldClearCache() {
            FieldAccessorCache.getFieldValues(new TestPojo(1L, "Test", 25), Set.of("name"));
            FieldAccessorCache.clearCache();

            String stats = FieldAccessorCache.getCacheStats();

            assertThat(stats).contains("0 classes cached");
        }
    }

    @Nested
    @DisplayName("Boolean getter detection")
    class BooleanGetterTests {

        @Test
        @DisplayName("should detect isXxx boolean getter")
        void shouldDetectIsBooleanGetter() {
            TestPojoWithBoolean pojo = new TestPojoWithBoolean(true);

            Object result = FieldAccessorCache.getFieldValue(pojo, "active");

            assertThat(result).isEqualTo(true);
        }
    }

    // Test classes

    static class TestPojo {
        private final Long id;
        private final String name;
        private final Integer age;

        TestPojo(Long id, String name, Integer age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public Integer getAge() { return age; }
    }

    static class TestPojoWithBoolean {
        private final boolean active;

        TestPojoWithBoolean(boolean active) {
            this.active = active;
        }

        public boolean isActive() { return active; }
    }

    record TestRecord(Long id, String name, boolean active) {}
}
