package com.jnzader.apigen.codegen.generator.java.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Entity classes in Java/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class JavaEntityTestGenerator {

    private final String basePackage;

    public JavaEntityTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Entity test class code. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        // Generate field setters for business columns
        StringBuilder fieldSetters = new StringBuilder();
        StringBuilder fieldAssertions = new StringBuilder();
        StringBuilder builderFields = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            String capitalField =
                    Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String sampleValue = getSampleTestValue(col);

            fieldSetters
                    .append("\n        ")
                    .append(entityVarName)
                    .append(".set")
                    .append(capitalField)
                    .append("(")
                    .append(sampleValue)
                    .append(");");

            fieldAssertions
                    .append("\n            assertThat(built.get")
                    .append(capitalField)
                    .append("()).isEqualTo(")
                    .append(sampleValue)
                    .append(");");

            builderFields
                    .append("\n                .")
                    .append(fieldName)
                    .append("(")
                    .append(sampleValue)
                    .append(")");
        }

        return
"""
package %s.%s.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("%s Entity Tests")
class %sTest {

    private %s %s;

    @BeforeEach
    void setUp() {
        %s = new %s();
    }

    @Nested
    @DisplayName("BaseEntity Interface Methods")
    class BaseEntityMethods {

        @Test
        @DisplayName("getId() should return the id value")
        void getIdShouldReturnIdValue() {
            %s.setId(1L);

            assertThat(%s.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getId() should return null when not set")
        void getIdShouldReturnNullWhenNotSet() {
            assertThat(%s.getId()).isNull();
        }

        @Test
        @DisplayName("getEstado() should return the estado value")
        void getEstadoShouldReturnEstadoValue() {
            %s.setEstado(true);

            assertThat(%s.getEstado()).isTrue();
        }

        @Test
        @DisplayName("getEstado() should default to true from builder")
        void getEstadoShouldDefaultToTrue() {
            %s built = %s.builder().build();

            assertThat(built.getEstado()).isTrue();
        }

        @Test
        @DisplayName("getEstado() can be set to false")
        void getEstadoCanBeSetToFalse() {
            %s.setEstado(false);

            assertThat(%s.getEstado()).isFalse();
        }
    }

    @Nested
    @DisplayName("Business Fields")
    class BusinessFields {

        @Test
        @DisplayName("Should set and get all business fields")
        void shouldSetAndGetAllBusinessFields() {%s

            assertThat(%s.getId()).isNull(); // ID not set
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should build entity with all fields")
        void shouldBuildEntityWithAllFields() {
            %s built = %s.builder()
                    .id(1L)
                    .estado(true)%s
                    .build();

            assertThat(built.getId()).isEqualTo(1L);
            assertThat(built.getEstado()).isTrue();%s
        }

        @Test
        @DisplayName("Should build empty entity with defaults")
        void shouldBuildEmptyEntityWithDefaults() {
            %s built = %s.builder().build();

            assertThat(built.getId()).isNull();
            assertThat(built.getEstado()).isTrue();
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("Entities with same ID should be equal")
        void entitiesWithSameIdShouldBeEqual() {
            %s entity1 = %s.builder().id(1L).build();
            %s entity2 = %s.builder().id(1L).build();

            assertThat(entity1).isEqualTo(entity2);
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
        }

        @Test
        @DisplayName("Entities with different IDs should not be equal")
        void entitiesWithDifferentIdsShouldNotBeEqual() {
            %s entity1 = %s.builder().id(1L).build();
            %s entity2 = %s.builder().id(2L).build();

            assertThat(entity1).isNotEqualTo(entity2);
        }

        @Test
        @DisplayName("Entity should equal itself")
        void entityShouldEqualItself() {
            %s.setId(1L);

            assertThat(%s).isEqualTo(%s);
        }

        @Test
        @DisplayName("Entity should not equal null")
        void entityShouldNotEqualNull() {
            %s.setId(1L);

            assertThat(%s).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Entity should not equal different type")
        void entityShouldNotEqualDifferentType() {
            %s.setId(1L);

            assertThat(%s).isNotEqualTo("string");
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("toString should contain class name and field values")
        void toStringShouldContainFieldValues() {
            %s.setId(1L);
            %s.setEstado(true);

            String result = %s.toString();

            assertThat(result).contains("%s");
            assertThat(result).contains("id=1");
        }
    }

    @Nested
    @DisplayName("Audit Fields")
    class AuditFields {

        @Test
        @DisplayName("Should set and get createdAt")
        void shouldSetAndGetCreatedAt() {
            java.time.Instant now = java.time.Instant.now();
            %s.setCreatedAt(now);

            assertThat(%s.getCreatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should set and get updatedAt")
        void shouldSetAndGetUpdatedAt() {
            java.time.Instant now = java.time.Instant.now();
            %s.setUpdatedAt(now);

            assertThat(%s.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should set and get createdBy")
        void shouldSetAndGetCreatedBy() {
            %s.setCreatedBy("testUser");

            assertThat(%s.getCreatedBy()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("Should set and get updatedBy")
        void shouldSetAndGetUpdatedBy() {
            %s.setUpdatedBy("testUser");

            assertThat(%s.getUpdatedBy()).isEqualTo("testUser");
        }

        @Test
        @DisplayName("Should set and get deletedAt")
        void shouldSetAndGetDeletedAt() {
            java.time.Instant now = java.time.Instant.now();
            %s.setDeletedAt(now);

            assertThat(%s.getDeletedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should set and get deletedBy")
        void shouldSetAndGetDeletedBy() {
            %s.setDeletedBy("testUser");

            assertThat(%s.getDeletedBy()).isEqualTo("testUser");
        }
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        // BaseEntity Methods
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        // Business Fields
                        fieldSetters.toString(),
                        entityVarName,
                        // Builder Tests
                        entityName,
                        entityName,
                        builderFields.toString(),
                        fieldAssertions.toString(),
                        entityName,
                        entityName,
                        // Equals and HashCode Tests
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        // ToString Tests
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        // Audit Fields
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName);
    }
}
