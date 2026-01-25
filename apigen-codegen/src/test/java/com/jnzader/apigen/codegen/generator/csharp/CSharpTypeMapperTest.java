package com.jnzader.apigen.codegen.generator.csharp;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CSharpTypeMapper Tests")
class CSharpTypeMapperTest {

    private CSharpTypeMapper typeMapper;

    @BeforeEach
    void setUp() {
        typeMapper = new CSharpTypeMapper();
    }

    @Nested
    @DisplayName("mapJavaType()")
    class MapJavaTypeTests {

        @Test
        @DisplayName("Should map Integer to int")
        void shouldMapIntegerToInt() {
            assertThat(typeMapper.mapJavaType("Integer")).isEqualTo("int");
        }

        @Test
        @DisplayName("Should map int primitive to int")
        void shouldMapIntPrimitiveToInt() {
            assertThat(typeMapper.mapJavaType("int")).isEqualTo("int");
        }

        @Test
        @DisplayName("Should map Long to long")
        void shouldMapLongToLong() {
            assertThat(typeMapper.mapJavaType("Long")).isEqualTo("long");
        }

        @Test
        @DisplayName("Should map Boolean to bool")
        void shouldMapBooleanToBool() {
            assertThat(typeMapper.mapJavaType("Boolean")).isEqualTo("bool");
        }

        @Test
        @DisplayName("Should map Character to char")
        void shouldMapCharacterToChar() {
            assertThat(typeMapper.mapJavaType("Character")).isEqualTo("char");
        }

        @Test
        @DisplayName("Should map String to string")
        void shouldMapStringToLowerCase() {
            assertThat(typeMapper.mapJavaType("String")).isEqualTo("string");
        }

        @Test
        @DisplayName("Should map BigDecimal to decimal")
        void shouldMapBigDecimalToDecimal() {
            assertThat(typeMapper.mapJavaType("BigDecimal")).isEqualTo("decimal");
        }

        @Test
        @DisplayName("Should map LocalDateTime to DateTime")
        void shouldMapLocalDateTimeToDateTime() {
            assertThat(typeMapper.mapJavaType("LocalDateTime")).isEqualTo("DateTime");
        }

        @Test
        @DisplayName("Should map LocalDate to DateOnly")
        void shouldMapLocalDateToDateOnly() {
            assertThat(typeMapper.mapJavaType("LocalDate")).isEqualTo("DateOnly");
        }

        @Test
        @DisplayName("Should map LocalTime to TimeOnly")
        void shouldMapLocalTimeToTimeOnly() {
            assertThat(typeMapper.mapJavaType("LocalTime")).isEqualTo("TimeOnly");
        }

        @Test
        @DisplayName("Should map UUID to Guid")
        void shouldMapUuidToGuid() {
            assertThat(typeMapper.mapJavaType("UUID")).isEqualTo("Guid");
        }

        @Test
        @DisplayName("Should map Instant to DateTimeOffset")
        void shouldMapInstantToDateTimeOffset() {
            assertThat(typeMapper.mapJavaType("Instant")).isEqualTo("DateTimeOffset");
        }

        @Test
        @DisplayName("Should map Duration to TimeSpan")
        void shouldMapDurationToTimeSpan() {
            assertThat(typeMapper.mapJavaType("Duration")).isEqualTo("TimeSpan");
        }
    }

    @Nested
    @DisplayName("mapColumnType()")
    class MapColumnTypeTests {

        @Test
        @DisplayName("Should map column with Integer type to int")
        void shouldMapColumnWithIntegerTypeToInt() {
            SqlColumn column = SqlColumn.builder().name("quantity").javaType("Integer").build();

            assertThat(typeMapper.mapColumnType(column)).isEqualTo("int");
        }

        @Test
        @DisplayName("Should map column with String type to string")
        void shouldMapColumnWithStringTypeToString() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            assertThat(typeMapper.mapColumnType(column)).isEqualTo("string");
        }
    }

    @Nested
    @DisplayName("getRequiredImports()")
    class GetRequiredImportsTests {

        @Test
        @DisplayName("Should return empty set for all types in C#")
        void shouldReturnEmptySetForAllTypes() {
            SqlColumn column = SqlColumn.builder().name("price").javaType("BigDecimal").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for String")
        void shouldReturnEmptySetForString() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefaultValue()")
    class GetDefaultValueTests {

        @Test
        @DisplayName("Should return string.Empty for string type")
        void shouldReturnStringEmptyForStringType() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("string.Empty");
        }

        @Test
        @DisplayName("Should return 0 for int type")
        void shouldReturnZeroForIntType() {
            SqlColumn column = SqlColumn.builder().name("count").javaType("Integer").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("0");
        }

        @Test
        @DisplayName("Should return 0L for long type")
        void shouldReturnZeroLForLongType() {
            SqlColumn column = SqlColumn.builder().name("id").javaType("Long").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("0L");
        }

        @Test
        @DisplayName("Should return false for bool type")
        void shouldReturnFalseForBoolType() {
            SqlColumn column = SqlColumn.builder().name("active").javaType("Boolean").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("false");
        }

        @Test
        @DisplayName("Should return 0m for decimal type")
        void shouldReturnZeroMForDecimalType() {
            SqlColumn column = SqlColumn.builder().name("price").javaType("BigDecimal").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("0m");
        }

        @Test
        @DisplayName("Should return DateTime.MinValue for DateTime type")
        void shouldReturnDateTimeMinValueForDateTime() {
            SqlColumn column =
                    SqlColumn.builder().name("created").javaType("LocalDateTime").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("DateTime.MinValue");
        }

        @Test
        @DisplayName("Should return Guid.Empty for Guid type")
        void shouldReturnGuidEmptyForGuid() {
            SqlColumn column = SqlColumn.builder().name("uuid").javaType("UUID").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("Guid.Empty");
        }
    }

    @Nested
    @DisplayName("getNullableType()")
    class GetNullableTypeTests {

        @Test
        @DisplayName("Should add ? suffix to non-nullable type")
        void shouldAddQuestionMarkSuffixToNonNullableType() {
            assertThat(typeMapper.getNullableType("string")).isEqualTo("string?");
        }

        @Test
        @DisplayName("Should not duplicate ? suffix")
        void shouldNotDuplicateQuestionMarkSuffix() {
            assertThat(typeMapper.getNullableType("string?")).isEqualTo("string?");
        }

        @Test
        @DisplayName("Should add ? suffix to int type")
        void shouldAddQuestionMarkSuffixToIntType() {
            assertThat(typeMapper.getNullableType("int")).isEqualTo("int?");
        }
    }

    @Nested
    @DisplayName("getListType()")
    class GetListTypeTests {

        @Test
        @DisplayName("Should return IEnumerable<T> format")
        void shouldReturnIEnumerableFormat() {
            assertThat(typeMapper.getListType("string")).isEqualTo("IEnumerable<string>");
        }

        @Test
        @DisplayName("Should work with complex types")
        void shouldWorkWithComplexTypes() {
            assertThat(typeMapper.getListType("Product")).isEqualTo("IEnumerable<Product>");
        }
    }

    @Nested
    @DisplayName("getListTypeForCollection()")
    class GetListTypeForCollectionTests {

        @Test
        @DisplayName("Should return List<T> format")
        void shouldReturnListFormat() {
            assertThat(typeMapper.getListTypeForCollection("string")).isEqualTo("List<string>");
        }

        @Test
        @DisplayName("Should work with entity types")
        void shouldWorkWithEntityTypes() {
            assertThat(typeMapper.getListTypeForCollection("Order")).isEqualTo("List<Order>");
        }
    }

    @Nested
    @DisplayName("getCollectionType()")
    class GetCollectionTypeTests {

        @Test
        @DisplayName("Should return ICollection<T> format")
        void shouldReturnICollectionFormat() {
            assertThat(typeMapper.getCollectionType("string")).isEqualTo("ICollection<string>");
        }

        @Test
        @DisplayName("Should work with entity types for navigation properties")
        void shouldWorkWithEntityTypesForNavigation() {
            assertThat(typeMapper.getCollectionType("OrderItem"))
                    .isEqualTo("ICollection<OrderItem>");
        }
    }

    @Nested
    @DisplayName("Primary Key")
    class PrimaryKeyTests {

        @Test
        @DisplayName("Should return long as primary key type")
        void shouldReturnLongAsPrimaryKeyType() {
            assertThat(typeMapper.getPrimaryKeyType()).isEqualTo("long");
        }

        @Test
        @DisplayName("Should return empty set for primary key imports")
        void shouldReturnEmptySetForPrimaryKeyImports() {
            assertThat(typeMapper.getPrimaryKeyImports()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isValueType()")
    class IsValueTypeTests {

        @Test
        @DisplayName("Should return true for int")
        void shouldReturnTrueForInt() {
            assertThat(typeMapper.isValueType("int")).isTrue();
        }

        @Test
        @DisplayName("Should return true for long")
        void shouldReturnTrueForLong() {
            assertThat(typeMapper.isValueType("long")).isTrue();
        }

        @Test
        @DisplayName("Should return true for bool")
        void shouldReturnTrueForBool() {
            assertThat(typeMapper.isValueType("bool")).isTrue();
        }

        @Test
        @DisplayName("Should return true for DateTime")
        void shouldReturnTrueForDateTime() {
            assertThat(typeMapper.isValueType("DateTime")).isTrue();
        }

        @Test
        @DisplayName("Should return true for Guid")
        void shouldReturnTrueForGuid() {
            assertThat(typeMapper.isValueType("Guid")).isTrue();
        }

        @Test
        @DisplayName("Should return false for string")
        void shouldReturnFalseForString() {
            assertThat(typeMapper.isValueType("string")).isFalse();
        }

        @Test
        @DisplayName("Should return false for custom class types")
        void shouldReturnFalseForCustomClassTypes() {
            assertThat(typeMapper.isValueType("Product")).isFalse();
        }
    }
}
