package com.jnzader.apigen.codegen.generator.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.codegen.model.SqlColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ImportManager Tests")
@SuppressWarnings({
    "java:S1874",
    "deprecation"
}) // Tests deprecated classes for backward compatibility
class ImportManagerTest {

    private ImportManager importManager;

    @BeforeEach
    void setUp() {
        importManager = new ImportManager();
    }

    @Nested
    @DisplayName("addImport()")
    class AddImportTests {

        @Test
        @DisplayName("Should add single import")
        void shouldAddSingleImport() {
            importManager.addImport("import java.util.List;");

            assertThat(importManager.getImports()).containsExactly("import java.util.List;");
        }

        @Test
        @DisplayName("Should add multiple imports")
        void shouldAddMultipleImports() {
            importManager.addImport("import java.util.List;");
            importManager.addImport("import java.util.Map;");

            assertThat(importManager.getImports())
                    .containsExactlyInAnyOrder("import java.util.List;", "import java.util.Map;");
        }

        @Test
        @DisplayName("Should deduplicate imports")
        void shouldDeduplicateImports() {
            importManager.addImport("import java.util.List;");
            importManager.addImport("import java.util.List;");

            assertThat(importManager.getImports()).hasSize(1);
        }

        @Test
        @DisplayName("Should return this for fluent API")
        void shouldReturnThisForFluentApi() {
            ImportManager result = importManager.addImport("import java.util.List;");
            assertThat(result).isSameAs(importManager);
        }
    }

    @Nested
    @DisplayName("addImportForClass()")
    class AddImportForClassTests {

        @Test
        @DisplayName("Should add import statement for class")
        void shouldAddImportStatementForClass() {
            importManager.addImportForClass("java.util.List");

            assertThat(importManager.getImports()).containsExactly("import java.util.List;");
        }

        @Test
        @DisplayName("Should handle nested classes")
        void shouldHandleNestedClasses() {
            importManager.addImportForClass("java.util.Map.Entry");

            assertThat(importManager.getImports()).containsExactly("import java.util.Map.Entry;");
        }
    }

    @Nested
    @DisplayName("addImportsForColumn()")
    class AddImportsForColumnTests {

        @ParameterizedTest
        @CsvSource({
            "BigDecimal, import java.math.BigDecimal;",
            "LocalDate, import java.time.LocalDate;",
            "LocalDateTime, import java.time.LocalDateTime;",
            "LocalTime, import java.time.LocalTime;",
            "Instant, import java.time.Instant;",
            "Duration, import java.time.Duration;",
            "UUID, import java.util.UUID;"
        })
        @DisplayName("Should add correct import for Java type")
        void shouldAddCorrectImportForJavaType(String javaType, String expectedImport) {
            SqlColumn column = createColumn(javaType);

            importManager.addImportsForColumn(column);

            assertThat(importManager.getImports()).containsExactly(expectedImport);
        }

        @Test
        @DisplayName("Should not add imports for primitive types")
        void shouldNotAddImportsForPrimitiveTypes() {
            SqlColumn stringColumn = createColumn("String");
            SqlColumn intColumn = createColumn("Integer");
            SqlColumn longColumn = createColumn("Long");
            SqlColumn boolColumn = createColumn("Boolean");

            importManager.addImportsForColumn(stringColumn);
            importManager.addImportsForColumn(intColumn);
            importManager.addImportsForColumn(longColumn);
            importManager.addImportsForColumn(boolColumn);

            assertThat(importManager.getImports()).isEmpty();
        }

        private SqlColumn createColumn(String javaType) {
            return SqlColumn.builder().name("testColumn").javaType(javaType).build();
        }
    }

    @Nested
    @DisplayName("addEntityImports()")
    class AddEntityImportsTests {

        @Test
        @DisplayName("Should add all entity imports")
        void shouldAddAllEntityImports() {
            importManager.addEntityImports("com.example.core");

            assertThat(importManager.getImports())
                    .contains(
                            "import com.example.core.domain.entity.Base;",
                            "import jakarta.persistence.*;",
                            "import jakarta.validation.constraints.*;",
                            "import lombok.*;",
                            "import org.hibernate.envers.Audited;");
        }
    }

    @Nested
    @DisplayName("addDTOImports()")
    class AddDTOImportsTests {

        @Test
        @DisplayName("Should add all DTO imports")
        void shouldAddAllDtoImports() {
            importManager.addDTOImports("com.example.core");

            assertThat(importManager.getImports())
                    .contains(
                            "import com.example.core.application.dto.BaseDTO;",
                            "import com.example.core.application.validation.ValidationGroups;",
                            "import jakarta.validation.constraints.*;",
                            "import lombok.*;");
        }
    }

    @Nested
    @DisplayName("addListImport()")
    class AddListImportTests {

        @Test
        @DisplayName("Should add List import")
        void shouldAddListImport() {
            importManager.addListImport();

            assertThat(importManager.getImports()).containsExactly("import java.util.List;");
        }
    }

    @Nested
    @DisplayName("addArrayListImport()")
    class AddArrayListImportTests {

        @Test
        @DisplayName("Should add ArrayList import")
        void shouldAddArrayListImport() {
            importManager.addArrayListImport();

            assertThat(importManager.getImports()).containsExactly("import java.util.ArrayList;");
        }
    }

    @Nested
    @DisplayName("build()")
    class BuildTests {

        @Test
        @DisplayName("Should build sorted imports string")
        void shouldBuildSortedImportsString() {
            importManager.addImport("import java.util.Map;");
            importManager.addImport("import java.util.List;");
            importManager.addImport("import java.util.ArrayList;");

            String result = importManager.build();

            // TreeSet sorts alphabetically
            assertThat(result)
                    .isEqualTo(
                            """
                            import java.util.ArrayList;
                            import java.util.List;
                            import java.util.Map;\
                            """);
        }

        @Test
        @DisplayName("Should return empty string for no imports")
        void shouldReturnEmptyStringForNoImports() {
            String result = importManager.build();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fluent API chaining")
    class FluentApiTests {

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            String result =
                    importManager
                            .addImport("import java.util.List;")
                            .addImportForClass("java.util.Map")
                            .addListImport()
                            .addArrayListImport()
                            .build();

            assertThat(result)
                    .contains("import java.util.List;")
                    .contains("import java.util.Map;")
                    .contains("import java.util.ArrayList;");
        }
    }
}
