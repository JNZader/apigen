package com.jnzader.apigen.server.service.generator.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jnzader.apigen.codegen.model.SqlColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SampleDataGenerator Tests")
class SampleDataGeneratorTest {

    @Nested
    @DisplayName("getSampleValue()")
    class GetSampleValueTests {

        @Test
        @DisplayName("Should return quoted string with example suffix for String type")
        void shouldReturnQuotedStringForStringType() {
            SqlColumn column = mockColumn("String", "title");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("\"Title example\"");
        }

        @Test
        @DisplayName("Should return 100 for Integer type")
        void shouldReturn100ForIntegerType() {
            SqlColumn column = mockColumn("Integer", "count");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should return 100 for int primitive")
        void shouldReturn100ForIntPrimitive() {
            SqlColumn column = mockColumn("int", "count");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should return 1000 for Long type")
        void shouldReturn1000ForLongType() {
            SqlColumn column = mockColumn("Long", "id");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("1000");
        }

        @Test
        @DisplayName("Should return 1000 for long primitive")
        void shouldReturn1000ForLongPrimitive() {
            SqlColumn column = mockColumn("long", "id");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("1000");
        }

        @Test
        @DisplayName("Should return 99.99 for Double type")
        void shouldReturnDecimalForDoubleType() {
            SqlColumn column = mockColumn("Double", "price");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Should return 99.99 for double primitive")
        void shouldReturnDecimalForDoublePrimitive() {
            SqlColumn column = mockColumn("double", "price");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Should return 99.99 for Float type")
        void shouldReturnDecimalForFloatType() {
            SqlColumn column = mockColumn("Float", "rate");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Should return 99.99 for float primitive")
        void shouldReturnDecimalForFloatPrimitive() {
            SqlColumn column = mockColumn("float", "rate");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Should return 199.99 for BigDecimal type")
        void shouldReturnDecimalForBigDecimalType() {
            SqlColumn column = mockColumn("BigDecimal", "amount");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("199.99");
        }

        @Test
        @DisplayName("Should return true for Boolean type")
        void shouldReturnTrueForBooleanType() {
            SqlColumn column = mockColumn("Boolean", "active");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("true");
        }

        @Test
        @DisplayName("Should return true for boolean primitive")
        void shouldReturnTrueForBooleanPrimitive() {
            SqlColumn column = mockColumn("boolean", "active");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("true");
        }

        @Test
        @DisplayName("Should return ISO date for LocalDate type")
        void shouldReturnIsoDateForLocalDateType() {
            SqlColumn column = mockColumn("LocalDate", "birthDate");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("\"2024-01-15\"");
        }

        @Test
        @DisplayName("Should return ISO datetime for LocalDateTime type")
        void shouldReturnIsoDateTimeForLocalDateTimeType() {
            SqlColumn column = mockColumn("LocalDateTime", "createdAt");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("\"2024-01-15T10:30:00\"");
        }

        @Test
        @DisplayName("Should return ISO time for LocalTime type")
        void shouldReturnIsoTimeForLocalTimeType() {
            SqlColumn column = mockColumn("LocalTime", "startTime");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("\"10:30:00\"");
        }

        @Test
        @DisplayName("Should return UUID string for UUID type")
        void shouldReturnUuidStringForUuidType() {
            SqlColumn column = mockColumn("UUID", "uuid");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("\"550e8400-e29b-41d4-a716-446655440000\"");
        }

        @Test
        @DisplayName("Should return null for unknown type")
        void shouldReturnNullForUnknownType() {
            SqlColumn column = mockColumn("CustomType", "field");

            String result = SampleDataGenerator.getSampleValue(column);

            assertThat(result).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("getSampleValue() with prefix")
    class GetSampleValueWithPrefixTests {

        @Test
        @DisplayName("Should include prefix in string value")
        void shouldIncludePrefixInStringValue() {
            SqlColumn column = mockColumn("String", "name");

            String result = SampleDataGenerator.getSampleValue(column, "updated");

            assertThat(result).isEqualTo("\"Updated name example\"");
        }

        @Test
        @DisplayName("Should not affect non-string types")
        void shouldNotAffectNonStringTypes() {
            SqlColumn column = mockColumn("Integer", "count");

            String result = SampleDataGenerator.getSampleValue(column, "updated");

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should handle null prefix")
        void shouldHandleNullPrefix() {
            SqlColumn column = mockColumn("String", "name");

            String result = SampleDataGenerator.getSampleValue(column, null);

            assertThat(result).isEqualTo("\"Name example\"");
        }
    }

    private SqlColumn mockColumn(String javaType, String fieldName) {
        SqlColumn column = mock(SqlColumn.class);
        when(column.getJavaType()).thenReturn(javaType);
        when(column.getJavaFieldName()).thenReturn(fieldName);
        return column;
    }
}
