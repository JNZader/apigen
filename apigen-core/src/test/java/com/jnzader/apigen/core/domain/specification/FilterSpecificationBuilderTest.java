package com.jnzader.apigen.core.domain.specification;

import com.jnzader.apigen.core.domain.entity.Base;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("FilterSpecificationBuilder Tests")
@SuppressWarnings("java:S5976") // Tests are intentionally separate for better readability and error isolation
class FilterSpecificationBuilderTest {

    private FilterSpecificationBuilder builder;

    // Test entity for specifications
    @Entity
    static class TestEntity extends Base {
        @Column
        private String name;
        @Column
        private Integer age;
        @Column
        private BigDecimal price;
        @Column
        private Boolean active;
        @Column
        private LocalDate birthDate;
        @Column
        private LocalDateTime createdAt;
        @Column
        private TestStatus status;

        enum TestStatus { PENDING, ACTIVE, COMPLETED }
    }

    @BeforeEach
    void setUp() {
        builder = new FilterSpecificationBuilder();
    }

    @Nested
    @DisplayName("Build from String")
    class BuildFromStringTests {

        @Test
        @DisplayName("should return empty specification for null filter")
        void shouldReturnEmptySpecForNullFilter() {
            Specification<TestEntity> spec = builder.build(null, TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should return empty specification for blank filter")
        void shouldReturnEmptySpecForBlankFilter() {
            Specification<TestEntity> spec = builder.build("   ", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should parse single EQ filter")
        void shouldParseSingleEqFilter() {
            Specification<TestEntity> spec = builder.build("name:eq:John", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should parse multiple filters")
        void shouldParseMultipleFilters() {
            Specification<TestEntity> spec = builder.build("name:eq:John,age:gte:25", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle LIKE operator")
        void shouldHandleLikeOperator() {
            Specification<TestEntity> spec = builder.build("name:like:John", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle STARTS operator")
        void shouldHandleStartsOperator() {
            Specification<TestEntity> spec = builder.build("name:starts:Jo", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle ENDS operator")
        void shouldHandleEndsOperator() {
            Specification<TestEntity> spec = builder.build("name:ends:hn", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle GT operator")
        void shouldHandleGtOperator() {
            Specification<TestEntity> spec = builder.build("age:gt:25", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle GTE operator")
        void shouldHandleGteOperator() {
            Specification<TestEntity> spec = builder.build("age:gte:25", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle LT operator")
        void shouldHandleLtOperator() {
            Specification<TestEntity> spec = builder.build("age:lt:30", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle LTE operator")
        void shouldHandleLteOperator() {
            Specification<TestEntity> spec = builder.build("age:lte:30", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle NEQ operator")
        void shouldHandleNeqOperator() {
            Specification<TestEntity> spec = builder.build("name:neq:John", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle IN operator")
        void shouldHandleInOperator() {
            Specification<TestEntity> spec = builder.build("name:in:John;Jane;Bob", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle NOT_IN operator")
        void shouldHandleNotInOperator() {
            Specification<TestEntity> spec = builder.build("name:notin:John;Jane", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle BETWEEN operator")
        void shouldHandleBetweenOperator() {
            Specification<TestEntity> spec = builder.build("age:between:20;30", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle NULL operator")
        void shouldHandleNullOperator() {
            Specification<TestEntity> spec = builder.build("name:null", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle NOT_NULL operator")
        void shouldHandleNotNullOperator() {
            Specification<TestEntity> spec = builder.build("name:notnull", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should skip invalid filters")
        void shouldSkipInvalidFilters() {
            Specification<TestEntity> spec = builder.build("invalid", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should skip unknown operators")
        void shouldSkipUnknownOperators() {
            Specification<TestEntity> spec = builder.build("name:unknown:value", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should skip filters without value when required")
        void shouldSkipFiltersWithoutValue() {
            Specification<TestEntity> spec = builder.build("name:eq:", TestEntity.class);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle empty filters in comma-separated string")
        void shouldHandleEmptyFilters() {
            Specification<TestEntity> spec = builder.build("name:eq:John,,age:gte:25", TestEntity.class);
            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("Build from Map")
    class BuildFromMapTests {

        @Test
        @DisplayName("should return empty specification for null map")
        void shouldReturnEmptySpecForNullMap() {
            Specification<TestEntity> spec = builder.build((Map<String, String>) null);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should return empty specification for empty map")
        void shouldReturnEmptySpecForEmptyMap() {
            Specification<TestEntity> spec = builder.build(new HashMap<>());
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should filter out system parameters")
        void shouldFilterOutSystemParams() {
            Map<String, String> filters = new HashMap<>();
            filters.put("page", "0");
            filters.put("size", "10");
            filters.put("sort", "id");
            filters.put("fields", "name,age");
            filters.put("filter", "custom");
            filters.put("name", "John");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle value with operator prefix")
        void shouldHandleValueWithOperatorPrefix() {
            Map<String, String> filters = new HashMap<>();
            filters.put("age", "gte:25");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle wildcards in value")
        void shouldHandleWildcardsInValue() {
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "*John*");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should handle percent wildcards in value")
        void shouldHandlePercentWildcardsInValue() {
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "%John%");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should use EQ for simple values")
        void shouldUseEqForSimpleValues() {
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "John");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should skip null values in map")
        void shouldSkipNullValuesInMap() {
            Map<String, String> filters = new HashMap<>();
            filters.put("name", null);

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }

        @Test
        @DisplayName("should skip blank values in map")
        void shouldSkipBlankValuesInMap() {
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "   ");

            Specification<TestEntity> spec = builder.build(filters);
            assertThat(spec).isNotNull();
        }
    }

    @Nested
    @DisplayName("FilterOperator")
    class FilterOperatorTests {

        @Test
        @DisplayName("should parse all operators from string")
        void shouldParseAllOperatorsFromString() {
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("eq"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.EQ);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("neq"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.NEQ);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("like"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.LIKE);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("starts"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.STARTS);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("ends"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.ENDS);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("gt"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.GT);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("gte"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.GTE);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("lt"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.LT);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("lte"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.LTE);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("in"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.IN);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("notin"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.NOT_IN);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("between"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.BETWEEN);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("null"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.NULL);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("notnull"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.NOT_NULL);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("EQ"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.EQ);
            assertThat(FilterSpecificationBuilder.FilterOperator.fromString("Eq"))
                    .isEqualTo(FilterSpecificationBuilder.FilterOperator.EQ);
        }

        @Test
        @DisplayName("should throw for unknown operator")
        void shouldThrowForUnknownOperator() {
            assertThatThrownBy(() -> FilterSpecificationBuilder.FilterOperator.fromString("unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("Specification Execution")
    @SuppressWarnings({"unchecked", "rawtypes"})
    class SpecificationExecutionTests {

        @Mock
        private Root<TestEntity> root;
        @Mock
        private CriteriaQuery<?> query;
        @Mock
        private CriteriaBuilder cb;
        @Mock
        private Path<Object> path;
        @Mock
        private Path<String> stringPath;
        @Mock
        private Path<Integer> intPath;
        @Mock
        private Predicate predicate;
        @Mock
        private Expression<String> lowerExpr;

        @BeforeEach
        void setUpMocks() {
            MockitoAnnotations.openMocks(this);
            // Use Mockito's default answer to return predicate for all CriteriaBuilder methods
            cb = mock(CriteriaBuilder.class, invocation -> {
                if (invocation.getMethod().getReturnType() == Predicate.class) {
                    return predicate;
                }
                if (invocation.getMethod().getReturnType() == Expression.class) {
                    return lowerExpr;
                }
                return null;
            });
            lenient().when(path.in(any(java.util.Collection.class))).thenReturn(predicate);
            lenient().when(path.getJavaType()).thenReturn((Class) String.class);
        }

        private void setupPath(String fieldName, Class<?> type) {
            when(root.get(fieldName)).thenReturn((Path) path);
            when(path.get(anyString())).thenReturn(path);
            when(path.getJavaType()).thenReturn((Class) type);
        }

        @Test
        @DisplayName("should execute EQ specification")
        void shouldExecuteEqSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:eq:John", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq("John"));
        }

        @Test
        @DisplayName("should execute NEQ specification")
        void shouldExecuteNeqSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:neq:John", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).notEqual(any(), eq("John"));
        }

        @Test
        @DisplayName("should execute LIKE specification")
        void shouldExecuteLikeSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:like:John", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).like(any(Expression.class), eq("%john%"));
        }

        @Test
        @DisplayName("should execute STARTS specification")
        void shouldExecuteStartsSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:starts:Jo", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).like(any(Expression.class), eq("jo%"));
        }

        @Test
        @DisplayName("should execute ENDS specification")
        void shouldExecuteEndsSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:ends:hn", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).like(any(Expression.class), eq("%hn"));
        }

        @Test
        @DisplayName("should execute GT specification with Integer")
        void shouldExecuteGtSpecification() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:gt:25", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThan(any(Expression.class), eq(25));
        }

        @Test
        @DisplayName("should execute GTE specification")
        void shouldExecuteGteSpecification() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:gte:25", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(25));
        }

        @Test
        @DisplayName("should execute LT specification")
        void shouldExecuteLtSpecification() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:lt:30", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).lessThan(any(Expression.class), eq(30));
        }

        @Test
        @DisplayName("should execute LTE specification")
        void shouldExecuteLteSpecification() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:lte:30", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).lessThanOrEqualTo(any(Expression.class), eq(30));
        }

        @Test
        @DisplayName("should execute IN specification")
        void shouldExecuteInSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:in:John;Jane;Bob", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(path).in(any(java.util.Collection.class));
        }

        @Test
        @DisplayName("should execute NOT_IN specification")
        void shouldExecuteNotInSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:notin:John;Jane", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).not(any());
        }

        @Test
        @DisplayName("should execute BETWEEN specification")
        void shouldExecuteBetweenSpecification() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:between:20;30", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).between(any(Expression.class), eq(20), eq(30));
        }

        @Test
        @DisplayName("should execute NULL specification")
        void shouldExecuteNullSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:null", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).isNull(any());
        }

        @Test
        @DisplayName("should execute NOT_NULL specification")
        void shouldExecuteNotNullSpecification() {
            setupPath("name", String.class);
            Specification<TestEntity> spec = builder.build("name:notnull", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).isNotNull(any());
        }

        @Test
        @DisplayName("should handle invalid field gracefully")
        void shouldHandleInvalidFieldGracefully() {
            when(root.get("invalidField")).thenThrow(new IllegalArgumentException("Unknown field"));
            Specification<TestEntity> spec = builder.build("invalidField:eq:value", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            // Should return conjunction (true) on error
            verify(cb, atLeastOnce()).conjunction();
        }

        @Test
        @DisplayName("should handle BETWEEN with invalid values")
        void shouldHandleBetweenWithInvalidValues() {
            setupPath("age", Integer.class);
            // Only one value instead of two
            Specification<TestEntity> spec = builder.build("age:between:20", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            // Should return conjunction on error
            verify(cb, atLeastOnce()).conjunction();
        }

        @Test
        @DisplayName("should convert Long values")
        void shouldConvertLongValues() {
            setupPath("id", Long.class);
            Specification<TestEntity> spec = builder.build("id:eq:12345", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(12345L));
        }

        @Test
        @DisplayName("should convert Double values")
        void shouldConvertDoubleValues() {
            setupPath("price", Double.class);
            Specification<TestEntity> spec = builder.build("price:gt:99.99", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThan(any(Expression.class), eq(99.99));
        }

        @Test
        @DisplayName("should convert Float values")
        void shouldConvertFloatValues() {
            setupPath("rate", Float.class);
            Specification<TestEntity> spec = builder.build("rate:lt:5.5", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).lessThan(any(Expression.class), eq(5.5f));
        }

        @Test
        @DisplayName("should convert BigDecimal values")
        void shouldConvertBigDecimalValues() {
            setupPath("price", BigDecimal.class);
            Specification<TestEntity> spec = builder.build("price:gte:100.50", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(new BigDecimal("100.50")));
        }

        @Test
        @DisplayName("should convert Boolean values")
        void shouldConvertBooleanValues() {
            setupPath("active", Boolean.class);
            Specification<TestEntity> spec = builder.build("active:eq:true", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(true));
        }

        @Test
        @DisplayName("should convert LocalDate values")
        void shouldConvertLocalDateValues() {
            setupPath("birthDate", LocalDate.class);
            Specification<TestEntity> spec = builder.build("birthDate:eq:2023-01-15", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(LocalDate.of(2023, 1, 15)));
        }

        @Test
        @DisplayName("should convert LocalDateTime values")
        void shouldConvertLocalDateTimeValues() {
            setupPath("createdAt", LocalDateTime.class);
            Specification<TestEntity> spec = builder.build("createdAt:gte:2023-01-15T10:30:00", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(LocalDateTime.of(2023, 1, 15, 10, 30, 0)));
        }

        @Test
        @DisplayName("should convert LocalDateTime from date-only string")
        void shouldConvertLocalDateTimeFromDateOnly() {
            setupPath("createdAt", LocalDateTime.class);
            Specification<TestEntity> spec = builder.build("createdAt:eq:2023-01-15", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(LocalDate.of(2023, 1, 15).atStartOfDay()));
        }

        @Test
        @DisplayName("should convert Enum values")
        void shouldConvertEnumValues() {
            setupPath("status", TestEntity.TestStatus.class);
            Specification<TestEntity> spec = builder.build("status:eq:ACTIVE", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(TestEntity.TestStatus.ACTIVE));
        }

        @Test
        @DisplayName("should handle nested path")
        void shouldHandleNestedPath() {
            Path nestedPath = mock(Path.class);
            when(root.get("role")).thenReturn((Path) path);
            when(path.get("name")).thenReturn(nestedPath);
            when(nestedPath.getJavaType()).thenReturn((Class) String.class);

            Specification<TestEntity> spec = builder.build("role.name:eq:Admin", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(root).get("role");
            verify(path).get("name");
        }

        @Test
        @DisplayName("should handle conversion error gracefully")
        void shouldHandleConversionErrorGracefully() {
            setupPath("age", Integer.class);
            // Invalid integer value
            Specification<TestEntity> spec = builder.build("age:eq:notanumber", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            // Should fallback to string value
            verify(cb).equal(any(), eq("notanumber"));
        }

        @Test
        @DisplayName("should handle invalid date format gracefully")
        void shouldHandleInvalidDateFormatGracefully() {
            setupPath("createdAt", LocalDateTime.class);
            Specification<TestEntity> spec = builder.build("createdAt:eq:invalid-date", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            // Should fallback to string value
            verify(cb).equal(any(), eq("invalid-date"));
        }

        @Test
        @DisplayName("should execute map filter with operator prefix")
        void shouldExecuteMapFilterWithOperatorPrefix() {
            setupPath("age", Integer.class);
            Map<String, String> filters = new HashMap<>();
            filters.put("age", "gte:25");

            Specification<TestEntity> spec = builder.build(filters);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThanOrEqualTo(any(Expression.class), eq(25));
        }

        @Test
        @DisplayName("should execute map filter with wildcard")
        void shouldExecuteMapFilterWithWildcard() {
            setupPath("name", String.class);
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "*John*");

            Specification<TestEntity> spec = builder.build(filters);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).like(any(Expression.class), eq("%%john%%"));
        }

        @Test
        @DisplayName("should execute map filter with simple value")
        void shouldExecuteMapFilterWithSimpleValue() {
            setupPath("name", String.class);
            Map<String, String> filters = new HashMap<>();
            filters.put("name", "John");

            Specification<TestEntity> spec = builder.build(filters);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq("John"));
        }

        @Test
        @DisplayName("should handle primitive int type")
        void shouldHandlePrimitiveIntType() {
            setupPath("age", int.class);
            Specification<TestEntity> spec = builder.build("age:eq:25", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(25));
        }

        @Test
        @DisplayName("should handle primitive long type")
        void shouldHandlePrimitiveLongType() {
            setupPath("id", long.class);
            Specification<TestEntity> spec = builder.build("id:eq:100", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(100L));
        }

        @Test
        @DisplayName("should handle primitive double type")
        void shouldHandlePrimitiveDoubleType() {
            setupPath("value", double.class);
            Specification<TestEntity> spec = builder.build("value:gt:10.5", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).greaterThan(any(Expression.class), eq(10.5));
        }

        @Test
        @DisplayName("should handle primitive float type")
        void shouldHandlePrimitiveFloatType() {
            setupPath("rate", float.class);
            Specification<TestEntity> spec = builder.build("rate:lt:3.5", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).lessThan(any(Expression.class), eq(3.5f));
        }

        @Test
        @DisplayName("should handle primitive boolean type")
        void shouldHandlePrimitiveBooleanType() {
            setupPath("active", boolean.class);
            Specification<TestEntity> spec = builder.build("active:eq:false", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(cb).equal(any(), eq(false));
        }

        @Test
        @DisplayName("should handle IN with multiple Integer values")
        void shouldHandleInWithMultipleIntegerValues() {
            setupPath("age", Integer.class);
            Specification<TestEntity> spec = builder.build("age:in:20;25;30", TestEntity.class);

            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            verify(path).in(any(java.util.Collection.class));
        }

        @Test
        @DisplayName("should handle map with value containing colon but not operator")
        void shouldHandleMapWithColonValueNotOperator() {
            setupPath("description", String.class);
            Map<String, String> filters = new HashMap<>();
            // "time" is not a valid operator, so entire value should be used
            filters.put("description", "time:10:30");

            Specification<TestEntity> spec = builder.build(filters);
            Predicate result = spec.toPredicate(root, query, cb);

            assertThat(result).isNotNull();
            // Should use EQ with full value
            verify(cb).equal(any(), eq("time:10:30"));
        }
    }
}
