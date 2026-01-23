package com.jnzader.apigen.codegen.generator.kotlin;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("KotlinTypeMapper Tests")
class KotlinTypeMapperTest {

    private KotlinTypeMapper typeMapper;

    @BeforeEach
    void setUp() {
        typeMapper = new KotlinTypeMapper();
    }

    @Nested
    @DisplayName("mapJavaTypeToKotlin()")
    class MapJavaTypeToKotlinTests {

        @Test
        @DisplayName("Should map Integer to Int")
        void shouldMapIntegerToInt() {
            assertThat(typeMapper.mapJavaTypeToKotlin("Integer")).isEqualTo("Int");
        }

        @Test
        @DisplayName("Should map int primitive to Int")
        void shouldMapIntPrimitiveToInt() {
            assertThat(typeMapper.mapJavaTypeToKotlin("int")).isEqualTo("Int");
        }

        @Test
        @DisplayName("Should map Long to Long")
        void shouldMapLongToLong() {
            assertThat(typeMapper.mapJavaTypeToKotlin("Long")).isEqualTo("Long");
        }

        @Test
        @DisplayName("Should map Boolean to Boolean")
        void shouldMapBooleanToBoolean() {
            assertThat(typeMapper.mapJavaTypeToKotlin("Boolean")).isEqualTo("Boolean");
        }

        @Test
        @DisplayName("Should map Character to Char")
        void shouldMapCharacterToChar() {
            assertThat(typeMapper.mapJavaTypeToKotlin("Character")).isEqualTo("Char");
        }

        @Test
        @DisplayName("Should keep String unchanged")
        void shouldKeepStringUnchanged() {
            assertThat(typeMapper.mapJavaTypeToKotlin("String")).isEqualTo("String");
        }

        @Test
        @DisplayName("Should keep BigDecimal unchanged")
        void shouldKeepBigDecimalUnchanged() {
            assertThat(typeMapper.mapJavaTypeToKotlin("BigDecimal")).isEqualTo("BigDecimal");
        }

        @Test
        @DisplayName("Should keep LocalDateTime unchanged")
        void shouldKeepLocalDateTimeUnchanged() {
            assertThat(typeMapper.mapJavaTypeToKotlin("LocalDateTime")).isEqualTo("LocalDateTime");
        }
    }

    @Nested
    @DisplayName("mapColumnType()")
    class MapColumnTypeTests {

        @Test
        @DisplayName("Should map column with Integer type to Int")
        void shouldMapColumnWithIntegerTypeToInt() {
            SqlColumn column = SqlColumn.builder().name("quantity").javaType("Integer").build();

            assertThat(typeMapper.mapColumnType(column)).isEqualTo("Int");
        }

        @Test
        @DisplayName("Should map column with String type to String")
        void shouldMapColumnWithStringTypeToString() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            assertThat(typeMapper.mapColumnType(column)).isEqualTo("String");
        }
    }

    @Nested
    @DisplayName("getRequiredImports()")
    class GetRequiredImportsTests {

        @Test
        @DisplayName("Should return BigDecimal import")
        void shouldReturnBigDecimalImport() {
            SqlColumn column = SqlColumn.builder().name("price").javaType("BigDecimal").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).containsExactly("import java.math.BigDecimal");
        }

        @Test
        @DisplayName("Should return LocalDateTime import")
        void shouldReturnLocalDateTimeImport() {
            SqlColumn column =
                    SqlColumn.builder().name("created").javaType("LocalDateTime").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).containsExactly("import java.time.LocalDateTime");
        }

        @Test
        @DisplayName("Should return UUID import")
        void shouldReturnUuidImport() {
            SqlColumn column = SqlColumn.builder().name("uuid").javaType("UUID").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).containsExactly("import java.util.UUID");
        }

        @Test
        @DisplayName("Should return empty set for String")
        void shouldReturnEmptySetForString() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).isEmpty();
        }

        @Test
        @DisplayName("Should return empty set for Int")
        void shouldReturnEmptySetForInt() {
            SqlColumn column = SqlColumn.builder().name("count").javaType("Integer").build();

            Set<String> imports = typeMapper.getRequiredImports(column);

            assertThat(imports).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDefaultValue()")
    class GetDefaultValueTests {

        @Test
        @DisplayName("Should return empty string for String type")
        void shouldReturnEmptyStringForStringType() {
            SqlColumn column = SqlColumn.builder().name("name").javaType("String").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("\"\"");
        }

        @Test
        @DisplayName("Should return 0 for Int type")
        void shouldReturnZeroForIntType() {
            SqlColumn column = SqlColumn.builder().name("count").javaType("Integer").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("0");
        }

        @Test
        @DisplayName("Should return 0L for Long type")
        void shouldReturnZeroLForLongType() {
            SqlColumn column = SqlColumn.builder().name("id").javaType("Long").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("0L");
        }

        @Test
        @DisplayName("Should return false for Boolean type")
        void shouldReturnFalseForBooleanType() {
            SqlColumn column = SqlColumn.builder().name("active").javaType("Boolean").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("false");
        }

        @Test
        @DisplayName("Should return BigDecimal.ZERO for BigDecimal type")
        void shouldReturnBigDecimalZeroForBigDecimalType() {
            SqlColumn column = SqlColumn.builder().name("price").javaType("BigDecimal").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("BigDecimal.ZERO");
        }

        @Test
        @DisplayName("Should return null for other types")
        void shouldReturnNullForOtherTypes() {
            SqlColumn column =
                    SqlColumn.builder().name("created").javaType("LocalDateTime").build();

            assertThat(typeMapper.getDefaultValue(column)).isEqualTo("null");
        }
    }

    @Nested
    @DisplayName("getNullableType()")
    class GetNullableTypeTests {

        @Test
        @DisplayName("Should add ? suffix to non-nullable type")
        void shouldAddQuestionMarkSuffixToNonNullableType() {
            assertThat(typeMapper.getNullableType("String")).isEqualTo("String?");
        }

        @Test
        @DisplayName("Should not duplicate ? suffix")
        void shouldNotDuplicateQuestionMarkSuffix() {
            assertThat(typeMapper.getNullableType("String?")).isEqualTo("String?");
        }

        @Test
        @DisplayName("Should add ? suffix to Int type")
        void shouldAddQuestionMarkSuffixToIntType() {
            assertThat(typeMapper.getNullableType("Int")).isEqualTo("Int?");
        }
    }

    @Nested
    @DisplayName("getListType()")
    class GetListTypeTests {

        @Test
        @DisplayName("Should return List<T> format")
        void shouldReturnListFormat() {
            assertThat(typeMapper.getListType("String")).isEqualTo("List<String>");
        }

        @Test
        @DisplayName("Should work with complex types")
        void shouldWorkWithComplexTypes() {
            assertThat(typeMapper.getListType("Product")).isEqualTo("List<Product>");
        }
    }

    @Nested
    @DisplayName("getMutableListType()")
    class GetMutableListTypeTests {

        @Test
        @DisplayName("Should return MutableList<T> format")
        void shouldReturnMutableListFormat() {
            assertThat(typeMapper.getMutableListType("String")).isEqualTo("MutableList<String>");
        }

        @Test
        @DisplayName("Should work with entity types")
        void shouldWorkWithEntityTypes() {
            assertThat(typeMapper.getMutableListType("Order")).isEqualTo("MutableList<Order>");
        }
    }

    @Nested
    @DisplayName("Primary Key")
    class PrimaryKeyTests {

        @Test
        @DisplayName("Should return Long as primary key type")
        void shouldReturnLongAsPrimaryKeyType() {
            assertThat(typeMapper.getPrimaryKeyType()).isEqualTo("Long");
        }

        @Test
        @DisplayName("Should return empty set for primary key imports")
        void shouldReturnEmptySetForPrimaryKeyImports() {
            assertThat(typeMapper.getPrimaryKeyImports()).isEmpty();
        }
    }
}
