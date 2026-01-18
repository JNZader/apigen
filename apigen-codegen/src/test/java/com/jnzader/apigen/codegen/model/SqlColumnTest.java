package com.jnzader.apigen.codegen.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SqlColumn Tests")
class SqlColumnTest {

    @Nested
    @DisplayName("getJavaFieldName()")
    class GetJavaFieldNameTests {

        @ParameterizedTest
        @CsvSource({
                "user_id, userId",
                "first_name, firstName",
                "created_at, createdAt",
                "last_modified_date, lastModifiedDate",
                "name, name",
                "ID, id",
                "UPPER_CASE, upperCase"
        })
        @DisplayName("Should convert snake_case to camelCase")
        void shouldConvertSnakeCaseToCamelCase(String columnName, String expected) {
            SqlColumn column = SqlColumn.builder().name(columnName).build();

            String result = column.getJavaFieldName();

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should return null for null name")
        void shouldReturnNullForNullName() {
            SqlColumn column = SqlColumn.builder().name(null).build();

            assertThat(column.getJavaFieldName()).isNull();
        }

        @Test
        @DisplayName("Should handle consecutive underscores")
        void shouldHandleConsecutiveUnderscores() {
            SqlColumn column = SqlColumn.builder().name("user__id").build();

            String result = column.getJavaFieldName();

            assertThat(result).isEqualTo("userId");
        }
    }

    @Nested
    @DisplayName("getValidationAnnotations()")
    class GetValidationAnnotationsTests {

        @Test
        @DisplayName("Should add @NotBlank for non-nullable String")
        void shouldAddNotBlankForNonNullableString() {
            SqlColumn column = SqlColumn.builder()
                    .name("name")
                    .javaType("String")
                    .nullable(false)
                    .primaryKey(false)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result).contains("@NotBlank");
        }

        @Test
        @DisplayName("Should add @NotNull for non-nullable non-String")
        void shouldAddNotNullForNonNullableNonString() {
            SqlColumn column = SqlColumn.builder()
                    .name("age")
                    .javaType("Integer")
                    .nullable(false)
                    .primaryKey(false)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result)
                    .contains("@NotNull")
                    .doesNotContain("@NotBlank");
        }

        @Test
        @DisplayName("Should add @Size for String with length")
        void shouldAddSizeForStringWithLength() {
            SqlColumn column = SqlColumn.builder()
                    .name("name")
                    .javaType("String")
                    .nullable(true)
                    .length(255)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result).contains("@Size(max = 255)");
        }

        @Test
        @DisplayName("Should not add @Size for non-String types")
        void shouldNotAddSizeForNonStringTypes() {
            SqlColumn column = SqlColumn.builder()
                    .name("count")
                    .javaType("Integer")
                    .nullable(true)
                    .length(10)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result).doesNotContain("@Size");
        }

        @Test
        @DisplayName("Should skip validation for primary key")
        void shouldSkipValidationForPrimaryKey() {
            SqlColumn column = SqlColumn.builder()
                    .name("id")
                    .javaType("Long")
                    .nullable(false)
                    .primaryKey(true)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result)
                    .doesNotContain("@NotNull")
                    .doesNotContain("@NotBlank");
        }

        @Test
        @DisplayName("Should add comment for unique constraint")
        void shouldAddCommentForUniqueConstraint() {
            SqlColumn column = SqlColumn.builder()
                    .name("email")
                    .javaType("String")
                    .nullable(true)
                    .unique(true)
                    .primaryKey(false)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result).contains("// @Unique");
        }

        @Test
        @DisplayName("Should combine multiple annotations")
        void shouldCombineMultipleAnnotations() {
            SqlColumn column = SqlColumn.builder()
                    .name("email")
                    .javaType("String")
                    .nullable(false)
                    .unique(true)
                    .primaryKey(false)
                    .length(100)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result)
                    .contains("@NotBlank")
                    .contains("@Size(max = 100)")
                    .contains("// @Unique");
        }

        @Test
        @DisplayName("Should return empty string for nullable column without constraints")
        void shouldReturnEmptyStringForNullableColumnWithoutConstraints() {
            SqlColumn column = SqlColumn.builder()
                    .name("description")
                    .javaType("String")
                    .nullable(true)
                    .build();

            String result = column.getValidationAnnotations();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Builder and Getters")
    class BuilderAndGettersTests {

        @Test
        @DisplayName("Should build column with all properties")
        void shouldBuildColumnWithAllProperties() {
            SqlColumn column = SqlColumn.builder()
                    .name("price")
                    .sqlType("DECIMAL(10,2)")
                    .javaType("BigDecimal")
                    .nullable(false)
                    .primaryKey(false)
                    .unique(false)
                    .autoIncrement(false)
                    .defaultValue("0.00")
                    .length(10)
                    .precision(10)
                    .scale(2)
                    .checkConstraint("price >= 0")
                    .comment("Product price")
                    .build();

            assertThat(column.getName()).isEqualTo("price");
            assertThat(column.getSqlType()).isEqualTo("DECIMAL(10,2)");
            assertThat(column.getJavaType()).isEqualTo("BigDecimal");
            assertThat(column.isNullable()).isFalse();
            assertThat(column.isPrimaryKey()).isFalse();
            assertThat(column.isUnique()).isFalse();
            assertThat(column.isAutoIncrement()).isFalse();
            assertThat(column.getDefaultValue()).isEqualTo("0.00");
            assertThat(column.getLength()).isEqualTo(10);
            assertThat(column.getPrecision()).isEqualTo(10);
            assertThat(column.getScale()).isEqualTo(2);
            assertThat(column.getCheckConstraint()).isEqualTo("price >= 0");
            assertThat(column.getComment()).isEqualTo("Product price");
        }
    }
}
