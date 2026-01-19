package com.jnzader.apigen.codegen.generator.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOTestGenerator Tests")
class DTOTestGeneratorTest {

    private DTOTestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new DTOTestGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate test class with correct package")
        void shouldGenerateTestClassWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result).contains("package com.example.products.application.dto;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("import org.junit.jupiter.api.BeforeEach;")
                    .contains("import org.junit.jupiter.api.DisplayName;")
                    .contains("import org.junit.jupiter.api.Nested;")
                    .contains("import org.junit.jupiter.api.Test;")
                    .contains("import static org.assertj.core.api.Assertions.assertThat;");
        }

        @Test
        @DisplayName("Should generate test class with correct name")
        void shouldGenerateTestClassWithCorrectName() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("class ProductDTOTest")
                    .contains("@DisplayName(\"ProductDTO Tests\")");
        }

        @Test
        @DisplayName("Should generate DTO field and setUp method")
        void shouldGenerateDtoFieldAndSetUp() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("private CategoryDTO dto;")
                    .contains("@BeforeEach")
                    .contains("void setUp()")
                    .contains("dto = new CategoryDTO();");
        }
    }

    @Nested
    @DisplayName("BaseDTO Interface Tests")
    class BaseDTOInterfaceTests {

        @Test
        @DisplayName("Should generate id() method tests")
        void shouldGenerateIdMethodTests() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"id() should return the id value\")")
                    .contains("void idShouldReturnIdValue()")
                    .contains("dto.setId(1L);")
                    .contains("assertThat(dto.id()).isEqualTo(1L);");
        }

        @Test
        @DisplayName("Should generate activo() method tests")
        void shouldGenerateActivoMethodTests() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"activo() should return the activo value\")")
                    .contains("void activoShouldReturnActivoValue()")
                    .contains("dto.setActivo(true);")
                    .contains("assertThat(dto.activo()).isTrue();");
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should generate builder test with correct entity name")
        void shouldGenerateBuilderTestWithCorrectEntityName() {
            SqlTable table = createSimpleTable("orders");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("OrderDTO builtDto = OrderDTO.builder()")
                    .contains(".id(1L)")
                    .contains(".activo(true)")
                    .contains(".build();");
        }

        @Test
        @DisplayName("Should generate empty builder test")
        void shouldGenerateEmptyBuilderTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"Should build empty DTO\")")
                    .contains("void shouldBuildEmptyDto()");
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Should generate equals test with correct entity name")
        void shouldGenerateEqualsTestWithCorrectEntityName() {
            SqlTable table = createSimpleTable("categories");

            String result = generator.generate(table);

            assertThat(result)
                    .contains(
                            "CategoryDTO dto1 = CategoryDTO.builder().id(1L).activo(true).build();")
                    .contains(
                            "CategoryDTO dto2 = CategoryDTO.builder().id(1L).activo(true).build();")
                    .contains("assertThat(dto1).isEqualTo(dto2);")
                    .contains("assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode());");
        }

        @Test
        @DisplayName("Should generate not equals test")
        void shouldGenerateNotEqualsTest() {
            SqlTable table = createSimpleTable("products");

            String result = generator.generate(table);

            assertThat(result)
                    .contains("@DisplayName(\"DTOs with different values should not be equal\")")
                    .contains("assertThat(dto1).isNotEqualTo(dto2);");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate toString test with correct entity name")
        void shouldGenerateToStringTestWithCorrectEntityName() {
            SqlTable table = createSimpleTable("users");

            String result = generator.generate(table);

            assertThat(result)
                    .contains(
                            "@DisplayName(\"toString should contain class name and field values\")")
                    .contains("assertThat(result).contains(\"UserDTO\");")
                    .contains("assertThat(result).contains(\"id=1\");")
                    .contains("assertThat(result).contains(\"activo=true\");");
        }
    }

    @Nested
    @DisplayName("Placeholder Validation")
    class PlaceholderValidationTests {

        @Test
        @DisplayName("Should not have unformatted placeholders")
        void shouldNotHaveUnformattedPlaceholders() {
            SqlTable table = createSimpleTable("test_entities");

            String result = generator.generate(table);

            assertThat(result).doesNotContain("%s");
        }

        @Test
        @DisplayName("Should generate valid Java code for various table names")
        void shouldGenerateValidJavaCodeForVariousTableNames() {
            List<String> tableNames =
                    List.of("products", "categories", "order_items", "user_profiles", "api_keys");

            for (String tableName : tableNames) {
                SqlTable table = createSimpleTable(tableName);
                String result = generator.generate(table);

                assertThat(result).as("Table: " + tableName).doesNotContain("%s");

                assertThat(result)
                        .as("Table: " + tableName)
                        .contains("class ")
                        .contains("@Test")
                        .contains("@BeforeEach");
            }
        }
    }

    @Nested
    @DisplayName("Entity Name Consistency")
    class EntityNameConsistencyTests {

        @Test
        @DisplayName("Should use correct entity name in all places")
        void shouldUseCorrectEntityNameInAllPlaces() {
            SqlTable table = createSimpleTable("order_items");

            String result = generator.generate(table);

            // Should use PascalCase for class names
            assertThat(result)
                    .contains("OrderItemDTO dto")
                    .contains("OrderItemDTO.builder()")
                    .contains("class OrderItemDTOTest");
        }

        @Test
        @DisplayName("Should handle single word table name correctly")
        void shouldHandleSingleWordTableNameCorrectly() {
            SqlTable table = createSimpleTable("users");

            String result = generator.generate(table);

            assertThat(result).contains("UserDTO").contains("class UserDTOTest");
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
