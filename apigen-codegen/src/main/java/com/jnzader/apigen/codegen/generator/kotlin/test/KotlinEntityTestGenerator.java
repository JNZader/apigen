package com.jnzader.apigen.codegen.generator.kotlin.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Entity classes in Kotlin/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class KotlinEntityTestGenerator {

    private final String basePackage;

    public KotlinEntityTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Entity test class code in Kotlin. */
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
            String sampleValue = getSampleTestValue(col);

            fieldSetters
                    .append("\n        ")
                    .append(entityVarName)
                    .append(".")
                    .append(fieldName)
                    .append(" = ")
                    .append(sampleValue);

            fieldAssertions
                    .append("\n            assertThat(built.")
                    .append(fieldName)
                    .append(").isEqualTo(")
                    .append(sampleValue)
                    .append(")");

            builderFields
                    .append("\n                .")
                    .append(fieldName)
                    .append("(")
                    .append(sampleValue)
                    .append(")");
        }

        return
"""
package %s.%s.domain.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("%s Entity Tests")
class %sTest {

    private lateinit var %s: %s

    @BeforeEach
    fun setUp() {
        %s = %s()
    }

    @Nested
    @DisplayName("BaseEntity Interface Methods")
    inner class BaseEntityMethods {

        @Test
        @DisplayName("getId() should return the id value")
        fun getIdShouldReturnIdValue() {
            %s.id = 1L

            assertThat(%s.id).isEqualTo(1L)
        }

        @Test
        @DisplayName("getId() should return null when not set")
        fun getIdShouldReturnNullWhenNotSet() {
            assertThat(%s.id).isNull()
        }

        @Test
        @DisplayName("getEstado() should return the estado value")
        fun getEstadoShouldReturnEstadoValue() {
            %s.estado = true

            assertThat(%s.estado).isTrue()
        }

        @Test
        @DisplayName("getEstado() should default to true from builder")
        fun getEstadoShouldDefaultToTrue() {
            val built = %s.builder().build()

            assertThat(built.estado).isTrue()
        }

        @Test
        @DisplayName("getEstado() can be set to false")
        fun getEstadoCanBeSetToFalse() {
            %s.estado = false

            assertThat(%s.estado).isFalse()
        }
    }

    @Nested
    @DisplayName("Business Fields")
    inner class BusinessFields {

        @Test
        @DisplayName("Should set and get all business fields")
        fun shouldSetAndGetAllBusinessFields() {%s

            assertThat(%s.id).isNull() // ID not set
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    inner class BuilderTests {

        @Test
        @DisplayName("Should build entity with all fields")
        fun shouldBuildEntityWithAllFields() {
            val built = %s.builder()
                    .id(1L)
                    .estado(true)%s
                    .build()

            assertThat(built.id).isEqualTo(1L)
            assertThat(built.estado).isTrue()%s
        }

        @Test
        @DisplayName("Should build empty entity with defaults")
        fun shouldBuildEmptyEntityWithDefaults() {
            val built = %s.builder().build()

            assertThat(built.id).isNull()
            assertThat(built.estado).isTrue()
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    inner class EqualsHashCodeTests {

        @Test
        @DisplayName("Entities with same ID should be equal")
        fun entitiesWithSameIdShouldBeEqual() {
            val entity1 = %s.builder().id(1L).build()
            val entity2 = %s.builder().id(1L).build()

            assertThat(entity1).isEqualTo(entity2)
            assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode())
        }

        @Test
        @DisplayName("Entities with different IDs should not be equal")
        fun entitiesWithDifferentIdsShouldNotBeEqual() {
            val entity1 = %s.builder().id(1L).build()
            val entity2 = %s.builder().id(2L).build()

            assertThat(entity1).isNotEqualTo(entity2)
        }

        @Test
        @DisplayName("Entity should equal itself")
        fun entityShouldEqualItself() {
            %s.id = 1L

            assertThat(%s).isEqualTo(%s)
        }

        @Test
        @DisplayName("Entity should not equal null")
        fun entityShouldNotEqualNull() {
            %s.id = 1L

            assertThat(%s).isNotEqualTo(null)
        }

        @Test
        @DisplayName("Entity should not equal different type")
        fun entityShouldNotEqualDifferentType() {
            %s.id = 1L

            assertThat(%s).isNotEqualTo("string")
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    inner class ToStringTests {

        @Test
        @DisplayName("toString should contain class name and field values")
        fun toStringShouldContainFieldValues() {
            %s.id = 1L
            %s.estado = true

            val result = %s.toString()

            assertThat(result).contains("%s")
            assertThat(result).contains("id=1")
        }
    }

    @Nested
    @DisplayName("Audit Fields")
    inner class AuditFields {

        @Test
        @DisplayName("Should set and get createdAt")
        fun shouldSetAndGetCreatedAt() {
            val now = java.time.Instant.now()
            %s.createdAt = now

            assertThat(%s.createdAt).isEqualTo(now)
        }

        @Test
        @DisplayName("Should set and get updatedAt")
        fun shouldSetAndGetUpdatedAt() {
            val now = java.time.Instant.now()
            %s.updatedAt = now

            assertThat(%s.updatedAt).isEqualTo(now)
        }

        @Test
        @DisplayName("Should set and get createdBy")
        fun shouldSetAndGetCreatedBy() {
            %s.createdBy = "testUser"

            assertThat(%s.createdBy).isEqualTo("testUser")
        }

        @Test
        @DisplayName("Should set and get updatedBy")
        fun shouldSetAndGetUpdatedBy() {
            %s.updatedBy = "testUser"

            assertThat(%s.updatedBy).isEqualTo("testUser")
        }

        @Test
        @DisplayName("Should set and get deletedAt")
        fun shouldSetAndGetDeletedAt() {
            val now = java.time.Instant.now()
            %s.deletedAt = now

            assertThat(%s.deletedAt).isEqualTo(now)
        }

        @Test
        @DisplayName("Should set and get deletedBy")
        fun shouldSetAndGetDeletedBy() {
            %s.deletedBy = "testUser"

            assertThat(%s.deletedBy).isEqualTo("testUser")
        }
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        // BaseEntity Methods
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        // Business Fields
                        fieldSetters.toString(),
                        entityVarName,
                        // Builder Tests
                        entityName,
                        builderFields.toString(),
                        fieldAssertions.toString(),
                        entityName,
                        // Equals and HashCode Tests
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
