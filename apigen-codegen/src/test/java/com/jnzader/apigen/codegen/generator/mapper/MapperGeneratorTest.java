package com.jnzader.apigen.codegen.generator.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("MapperGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific mapper features, not the same feature
// with different inputs
class MapperGeneratorTest {

    private MapperGenerator mapperGenerator;

    @BeforeEach
    void setUp() {
        mapperGenerator = new MapperGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate mapper interface with correct package")
        void shouldGenerateMapperInterfaceWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = mapperGenerator.generate(table);

            assertThat(result).contains("package com.example.products.application.mapper;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = mapperGenerator.generate(table);

            assertThat(result)
                    .contains("import com.jnzader.apigen.core.application.mapper.BaseMapper;")
                    .contains("import com.example.products.application.dto.ProductDTO;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains("import org.mapstruct.*;");
        }

        @Test
        @DisplayName("Should generate MapStruct mapper annotation")
        void shouldGenerateMapStructMapperAnnotation() {
            SqlTable table = createSimpleTable("products");

            String result = mapperGenerator.generate(table);

            assertThat(result).contains("@Mapper(componentModel = \"spring\")");
        }

        @Test
        @DisplayName("Should extend BaseMapper with correct types")
        void shouldExtendBaseMapperWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = mapperGenerator.generate(table);

            assertThat(result)
                    .contains(
                            "public interface ProductMapper extends BaseMapper<Product,"
                                    + " ProductDTO>");
        }

        @Test
        @DisplayName("Should include comment about inherited methods")
        void shouldIncludeCommentAboutInheritedMethods() {
            SqlTable table = createSimpleTable("products");

            String result = mapperGenerator.generate(table);

            assertThat(result)
                    .contains("// Inherits toDTO, toEntity, updateEntityFromDTO from BaseMapper");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}Mapper")
        @CsvSource({
            "categories, Category, categories, 'Category, CategoryDTO'",
            "boxes, Box, boxes, 'Box, BoxDTO'",
            "user_profiles, UserProfile, userprofiles, 'UserProfile, UserProfileDTO'",
            "products, Product, products, 'Product, ProductDTO'"
        })
        @DisplayName("Should handle various table names correctly")
        void shouldHandleVariousTableNames(
                String tableName, String entityName, String moduleName, String expectedTypes) {
            SqlTable table = createSimpleTable(tableName);

            String result = mapperGenerator.generate(table);

            assertThat(result)
                    .contains("package com.example." + moduleName + ".application.mapper;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".application.dto."
                                    + entityName
                                    + "DTO;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains(
                            "public interface "
                                    + entityName
                                    + "Mapper extends BaseMapper<"
                                    + entityName
                                    + ", "
                                    + entityName
                                    + "DTO>")
                    .contains(expectedTypes);
        }
    }

    private SqlTable createSimpleTable(String tableName) {
        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build()))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }
}
