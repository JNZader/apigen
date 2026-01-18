package com.jnzader.apigen.codegen.generator.util;

import com.jnzader.apigen.codegen.model.SqlColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestValueProvider Tests")
class TestValueProviderTest {

    @Nested
    @DisplayName("getSampleTestValue()")
    class GetSampleTestValueTests {

        @Test
        @DisplayName("Should return quoted string for String type")
        void shouldReturnQuotedStringForStringType() {
            SqlColumn column = createColumn("String", "name");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("\"Test name\"");
        }

        @Test
        @DisplayName("Should return integer for Integer type")
        void shouldReturnIntegerForIntegerType() {
            SqlColumn column = createColumn("Integer", "count");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should return integer for int type")
        void shouldReturnIntegerForIntPrimitive() {
            SqlColumn column = createColumn("int", "count");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should return long with L suffix for Long type")
        void shouldReturnLongForLongType() {
            SqlColumn column = createColumn("Long", "id");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("1000L");
        }

        @Test
        @DisplayName("Should return double for Double type")
        void shouldReturnDoubleForDoubleType() {
            SqlColumn column = createColumn("Double", "price");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("99.99");
        }

        @Test
        @DisplayName("Should return float with f suffix for Float type")
        void shouldReturnFloatForFloatType() {
            SqlColumn column = createColumn("Float", "rate");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("99.99f");
        }

        @Test
        @DisplayName("Should return BigDecimal constructor for BigDecimal type")
        void shouldReturnBigDecimalForBigDecimalType() {
            SqlColumn column = createColumn("BigDecimal", "amount");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("new java.math.BigDecimal(\"199.99\")");
        }

        @Test
        @DisplayName("Should return true for Boolean type")
        void shouldReturnTrueForBooleanType() {
            SqlColumn column = createColumn("Boolean", "active");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("true");
        }

        @Test
        @DisplayName("Should return LocalDate.now() for LocalDate type")
        void shouldReturnLocalDateNowForLocalDateType() {
            SqlColumn column = createColumn("LocalDate", "birthDate");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("java.time.LocalDate.now()");
        }

        @Test
        @DisplayName("Should return LocalDateTime.now() for LocalDateTime type")
        void shouldReturnLocalDateTimeNowForLocalDateTimeType() {
            SqlColumn column = createColumn("LocalDateTime", "createdAt");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("java.time.LocalDateTime.now()");
        }

        @Test
        @DisplayName("Should return LocalTime.now() for LocalTime type")
        void shouldReturnLocalTimeNowForLocalTimeType() {
            SqlColumn column = createColumn("LocalTime", "startTime");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("java.time.LocalTime.now()");
        }

        @Test
        @DisplayName("Should return UUID.randomUUID() for UUID type")
        void shouldReturnUuidRandomForUuidType() {
            SqlColumn column = createColumn("UUID", "uuid");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("java.util.UUID.randomUUID()");
        }

        @Test
        @DisplayName("Should return null for unknown type")
        void shouldReturnNullForUnknownType() {
            SqlColumn column = createColumn("UnknownType", "field");

            String result = TestValueProvider.getSampleTestValue(column);

            assertThat(result).isEqualTo("null");
        }

        @Test
        @DisplayName("Should include prefix when provided")
        void shouldIncludePrefixWhenProvided() {
            SqlColumn column = createColumn("String", "name");

            String result = TestValueProvider.getSampleTestValue(column, "Updated");

            assertThat(result).isEqualTo("\"Updated name\"");
        }
    }

    @Nested
    @DisplayName("getSampleJsonValue()")
    class GetSampleJsonValueTests {

        @Test
        @DisplayName("Should return quoted string with example suffix")
        void shouldReturnQuotedStringWithExampleSuffix() {
            SqlColumn column = createColumn("String", "name");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("\"Name example\"");
        }

        @Test
        @DisplayName("Should return number without suffix for Integer")
        void shouldReturnNumberWithoutSuffixForInteger() {
            SqlColumn column = createColumn("Integer", "count");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("100");
        }

        @Test
        @DisplayName("Should return number without L suffix for Long")
        void shouldReturnNumberWithoutLSuffixForLong() {
            SqlColumn column = createColumn("Long", "id");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("1000");
        }

        @Test
        @DisplayName("Should return decimal for BigDecimal")
        void shouldReturnDecimalForBigDecimal() {
            SqlColumn column = createColumn("BigDecimal", "amount");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("199.99");
        }

        @Test
        @DisplayName("Should return ISO date string for LocalDate")
        void shouldReturnIsoDateStringForLocalDate() {
            SqlColumn column = createColumn("LocalDate", "birthDate");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("\"2024-01-15\"");
        }

        @Test
        @DisplayName("Should return ISO datetime string for LocalDateTime")
        void shouldReturnIsoDateTimeStringForLocalDateTime() {
            SqlColumn column = createColumn("LocalDateTime", "createdAt");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("\"2024-01-15T10:30:00\"");
        }

        @Test
        @DisplayName("Should return ISO time string for LocalTime")
        void shouldReturnIsoTimeStringForLocalTime() {
            SqlColumn column = createColumn("LocalTime", "startTime");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("\"10:30:00\"");
        }

        @Test
        @DisplayName("Should return UUID string for UUID type")
        void shouldReturnUuidStringForUuidType() {
            SqlColumn column = createColumn("UUID", "uuid");

            String result = TestValueProvider.getSampleJsonValue(column);

            assertThat(result).isEqualTo("\"550e8400-e29b-41d4-a716-446655440000\"");
        }

        @Test
        @DisplayName("Should include prefix in JSON value")
        void shouldIncludePrefixInJsonValue() {
            SqlColumn column = createColumn("String", "title");

            String result = TestValueProvider.getSampleJsonValue(column, "updated");

            assertThat(result).isEqualTo("\"Updated title example\"");
        }
    }

    private SqlColumn createColumn(String javaType, String fieldName) {
        return SqlColumn.builder()
                .name(fieldName)
                .javaType(javaType)
                .build();
    }
}
