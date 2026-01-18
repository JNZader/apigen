package com.jnzader.apigen.core.infrastructure.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BeanCopyUtils Tests")
class BeanCopyUtilsTest {

    @Nested
    @DisplayName("copyNonNullProperties")
    class CopyNonNullPropertiesTests {

        @Test
        @DisplayName("should copy non-null properties")
        void shouldCopyNonNullProperties() {
            var source = new TestBean("name", 25, "description");
            var target = new TestBean(null, null, null);

            BeanCopyUtils.copyNonNullProperties(source, target);

            assertThat(target.getName()).isEqualTo("name");
            assertThat(target.getAge()).isEqualTo(25);
            assertThat(target.getDescription()).isEqualTo("description");
        }

        @Test
        @DisplayName("should not copy null properties")
        void shouldNotCopyNullProperties() {
            var source = new TestBean("name", null, null);
            var target = new TestBean("original", 30, "original desc");

            BeanCopyUtils.copyNonNullProperties(source, target);

            assertThat(target.getName()).isEqualTo("name");
            assertThat(target.getAge()).isEqualTo(30); // Not overwritten
            assertThat(target.getDescription()).isEqualTo("original desc"); // Not overwritten
        }

        @Test
        @DisplayName("should handle null source or target")
        void shouldHandleNullSourceOrTarget() {
            var bean = new TestBean("test", 10, "desc");

            // Should not throw and bean should remain unchanged
            BeanCopyUtils.copyNonNullProperties(null, bean);
            BeanCopyUtils.copyNonNullProperties(bean, null);

            assertThat(bean.getName()).isEqualTo("test");
        }

        @Test
        @DisplayName("should ignore specified properties")
        void shouldIgnoreSpecifiedProperties() {
            var source = new TestBean("name", 25, "description");
            var target = new TestBean("original", 30, "original desc");

            BeanCopyUtils.copyNonNullProperties(source, target, "age");

            assertThat(target.getName()).isEqualTo("name");
            assertThat(target.getAge()).isEqualTo(30); // Ignored
            assertThat(target.getDescription()).isEqualTo("description");
        }
    }

    @Nested
    @DisplayName("getNullPropertyNames")
    class GetNullPropertyNamesTests {

        @Test
        @DisplayName("should return null property names")
        void shouldReturnNullPropertyNames() {
            var bean = new TestBean("name", null, null);

            String[] nullProps = BeanCopyUtils.getNullPropertyNames(bean);

            assertThat(nullProps).contains("age", "description");
        }

        @Test
        @DisplayName("should return empty array for null source")
        void shouldReturnEmptyArrayForNullSource() {
            String[] nullProps = BeanCopyUtils.getNullPropertyNames(null);
            assertThat(nullProps).isEmpty();
        }
    }

    @Nested
    @DisplayName("getNonNullPropertyNames")
    class GetNonNullPropertyNamesTests {

        @Test
        @DisplayName("should return non-null property names")
        void shouldReturnNonNullPropertyNames() {
            var bean = new TestBean("name", 25, null);

            Set<String> nonNullProps = BeanCopyUtils.getNonNullPropertyNames(bean);

            assertThat(nonNullProps)
                    .contains("name", "age")
                    .doesNotContain("description");
        }

        @Test
        @DisplayName("should return empty set for null source")
        void shouldReturnEmptySetForNullSource() {
            Set<String> nonNullProps = BeanCopyUtils.getNonNullPropertyNames(null);
            assertThat(nonNullProps).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAllPropertiesNull")
    class IsAllPropertiesNullTests {

        @Test
        @DisplayName("should return true when all properties are null")
        void shouldReturnTrueWhenAllNull() {
            var bean = new TestBean(null, null, null);
            assertThat(BeanCopyUtils.isAllPropertiesNull(bean)).isTrue();
        }

        @Test
        @DisplayName("should return false when some properties are not null")
        void shouldReturnFalseWhenSomeNotNull() {
            var bean = new TestBean("name", null, null);
            assertThat(BeanCopyUtils.isAllPropertiesNull(bean)).isFalse();
        }
    }

    @Nested
    @DisplayName("countNonNullProperties")
    class CountNonNullPropertiesTests {

        @Test
        @DisplayName("should count non-null properties")
        void shouldCountNonNullProperties() {
            var bean = new TestBean("name", 25, null);
            assertThat(BeanCopyUtils.countNonNullProperties(bean)).isEqualTo(2);
        }

        @Test
        @DisplayName("should return zero for all null properties")
        void shouldReturnZeroForAllNull() {
            var bean = new TestBean(null, null, null);
            assertThat(BeanCopyUtils.countNonNullProperties(bean)).isZero();
        }
    }

    // Test bean class
    static class TestBean {
        private String name;
        private Integer age;
        private String description;

        public TestBean(String name, Integer age, String description) {
            this.name = name;
            this.age = age;
            this.description = description;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
