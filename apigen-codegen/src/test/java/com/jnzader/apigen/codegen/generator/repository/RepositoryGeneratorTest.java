package com.jnzader.apigen.codegen.generator.repository;

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

@DisplayName("RepositoryGenerator Tests")
@SuppressWarnings(
        "java:S5976") // Tests validate different specific repository features, not the same feature
// with different inputs
class RepositoryGeneratorTest {

    private RepositoryGenerator repositoryGenerator;

    @BeforeEach
    void setUp() {
        repositoryGenerator = new RepositoryGenerator("com.example");
    }

    @Nested
    @DisplayName("generate()")
    class GenerateTests {

        @Test
        @DisplayName("Should generate repository interface with correct package")
        void shouldGenerateRepositoryInterfaceWithCorrectPackage() {
            SqlTable table = createSimpleTable("products");

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result).contains("package com.example.products.infrastructure.repository;");
        }

        @Test
        @DisplayName("Should generate correct imports")
        void shouldGenerateCorrectImports() {
            SqlTable table = createSimpleTable("products");

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result)
                    .contains("import com.jnzader.apigen.core.domain.repository.BaseRepository;")
                    .contains("import com.example.products.domain.entity.Product;")
                    .contains("import org.springframework.stereotype.Repository;");
        }

        @Test
        @DisplayName("Should generate @Repository annotation")
        void shouldGenerateRepositoryAnnotation() {
            SqlTable table = createSimpleTable("products");

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result).contains("@Repository");
        }

        @Test
        @DisplayName("Should extend BaseRepository with correct types")
        void shouldExtendBaseRepositoryWithCorrectTypes() {
            SqlTable table = createSimpleTable("products");

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result)
                    .contains(
                            "public interface ProductRepository extends BaseRepository<Product,"
                                    + " Long>");
        }

        @Test
        @DisplayName("Should include comment about custom query methods")
        void shouldIncludeCommentAboutCustomQueryMethods() {
            SqlTable table = createSimpleTable("products");

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result).contains("// Custom query methods");
        }

        @Test
        @DisplayName("Should generate findBy method for unique column")
        void shouldGenerateFindByMethodForUniqueColumn() {
            SqlColumn uniqueColumn =
                    SqlColumn.builder()
                            .name("email")
                            .javaType("String")
                            .nullable(false)
                            .unique(true)
                            .primaryKey(false)
                            .build();

            SqlTable table = createTableWithColumn("users", uniqueColumn);

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result)
                    .contains("import java.util.Optional;")
                    .contains("Optional<User> findByEmail(String email);");
        }

        @Test
        @DisplayName("Should not generate findBy method for primary key column")
        void shouldNotGenerateFindByMethodForPrimaryKeyColumn() {
            SqlColumn pkColumn =
                    SqlColumn.builder()
                            .name("id")
                            .javaType("Long")
                            .nullable(false)
                            .unique(true)
                            .primaryKey(true)
                            .build();

            SqlTable table =
                    SqlTable.builder()
                            .name("products")
                            .columns(List.of(pkColumn))
                            .foreignKeys(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .build();

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result).doesNotContain("findById");
        }

        @Test
        @DisplayName("Should generate multiple findBy methods for multiple unique columns")
        void shouldGenerateMultipleFindByMethodsForMultipleUniqueColumns() {
            List<SqlColumn> columns =
                    List.of(
                            SqlColumn.builder()
                                    .name("id")
                                    .javaType("Long")
                                    .primaryKey(true)
                                    .build(),
                            SqlColumn.builder()
                                    .name("email")
                                    .javaType("String")
                                    .unique(true)
                                    .primaryKey(false)
                                    .build(),
                            SqlColumn.builder()
                                    .name("username")
                                    .javaType("String")
                                    .unique(true)
                                    .primaryKey(false)
                                    .build());

            SqlTable table =
                    SqlTable.builder()
                            .name("users")
                            .columns(columns)
                            .foreignKeys(new ArrayList<>())
                            .indexes(new ArrayList<>())
                            .build();

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result)
                    .contains("Optional<User> findByEmail(String email);")
                    .contains("Optional<User> findByUsername(String username);");
        }

        @Test
        @DisplayName("Should generate method for SQL function")
        void shouldGenerateMethodForSqlFunction() {
            SqlTable table = createSimpleTable("products");

            SqlFunction function =
                    SqlFunction.builder()
                            .name("get_product_stats")
                            .returnType("ProductStats")
                            .parameters(
                                    List.of(
                                            SqlFunction.SqlParameter.builder()
                                                    .name("product_id")
                                                    .sqlType("BIGINT")
                                                    .javaType("Long")
                                                    .build()))
                            .build();

            String result = repositoryGenerator.generate(table, List.of(function));

            assertThat(result)
                    .contains("import org.springframework.data.jpa.repository.Query;")
                    .contains(
                            "@Query(value = \"SELECT * FROM get_product_stats(:product_id)\","
                                    + " nativeQuery = true)");
        }

        @ParameterizedTest(name = "Should handle table {0} -> {1}Repository")
        @CsvSource({
            "order_items, orderitems, OrderItem",
            "user_profiles, userprofiles, UserProfile",
            "categories, categories, Category"
        })
        @DisplayName("Should handle various table names")
        void shouldHandleTableWithDifferentModuleName(
                String tableName, String moduleName, String entityName) {
            SqlTable table = createSimpleTable(tableName);

            String result = repositoryGenerator.generate(table, List.of());

            assertThat(result)
                    .contains("package com.example." + moduleName + ".infrastructure.repository;")
                    .contains(
                            "import com.example."
                                    + moduleName
                                    + ".domain.entity."
                                    + entityName
                                    + ";")
                    .contains(
                            "public interface "
                                    + entityName
                                    + "Repository extends BaseRepository<"
                                    + entityName
                                    + ", Long>");
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

    private SqlTable createTableWithColumn(String tableName, SqlColumn column) {
        return SqlTable.builder()
                .name(tableName)
                .columns(
                        List.of(
                                SqlColumn.builder()
                                        .name("id")
                                        .javaType("Long")
                                        .primaryKey(true)
                                        .build(),
                                column))
                .foreignKeys(new ArrayList<>())
                .indexes(new ArrayList<>())
                .build();
    }
}
