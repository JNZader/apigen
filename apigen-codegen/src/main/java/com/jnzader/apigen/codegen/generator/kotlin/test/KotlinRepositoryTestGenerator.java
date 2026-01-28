package com.jnzader.apigen.codegen.generator.kotlin.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Repository implementations in Kotlin/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class KotlinRepositoryTestGenerator {

    private final String basePackage;

    public KotlinRepositoryTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Repository test class code in Kotlin. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        // Generate sample field assignments for creating test data
        StringBuilder fieldAssignments = new StringBuilder();

        for (SqlColumn col : table.getBusinessColumns()) {
            String fieldName = col.getJavaFieldName();
            String sampleValue = getSampleTestValue(col);

            fieldAssignments
                    .append("\n                .")
                    .append(fieldName)
                    .append("(")
                    .append(sampleValue)
                    .append(")");
        }

        return
"""
package %s.%s.infrastructure.repository

import %s.%s.domain.entity.%s
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("%sRepository Tests")
class %sRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var repository: %sRepository

    private lateinit var %s: %s

    @BeforeEach
    fun setUp() {
        %s = %s.builder()
                .estado(true)%s
                .build()
    }

    @Nested
    @DisplayName("Standard CRUD Operations")
    inner class StandardCrudOperations {

        @Test
        @DisplayName("Should save and retrieve entity")
        fun shouldSaveAndRetrieve() {
            val saved = entityManager.persistAndFlush(%s)

            val found = repository.findById(saved.id!!)

            assertThat(found).isPresent
            assertThat(found.get().id).isEqualTo(saved.id)
        }

        @Test
        @DisplayName("Should find all entities")
        fun shouldFindAll() {
            entityManager.persistAndFlush(%s)

            val all = repository.findAll()

            assertThat(all).isNotEmpty
        }

        @Test
        @DisplayName("Should update entity")
        fun shouldUpdate() {
            val saved = entityManager.persistAndFlush(%s)
            saved.estado = false
            repository.save(saved)
            entityManager.flush()
            entityManager.clear()

            val updated = repository.findById(saved.id!!)

            assertThat(updated).isPresent
            assertThat(updated.get().estado).isFalse()
        }

        @Test
        @DisplayName("Should delete entity")
        fun shouldDelete() {
            val saved = entityManager.persistAndFlush(%s)
            val id = saved.id!!

            repository.deleteById(id)
            entityManager.flush()

            val deleted = repository.findById(id)

            assertThat(deleted).isEmpty
        }

        @Test
        @DisplayName("Should check if entity exists by ID")
        fun shouldCheckExistsById() {
            val saved = entityManager.persistAndFlush(%s)

            val exists = repository.existsById(saved.id!!)

            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("Should return false for non-existent ID")
        fun shouldReturnFalseForNonExistentId() {
            val exists = repository.existsById(999999L)

            assertThat(exists).isFalse()
        }

        @Test
        @DisplayName("Should count all entities")
        fun shouldCountAll() {
            entityManager.persistAndFlush(%s)

            val count = repository.count()

            assertThat(count).isGreaterThan(0)
        }
    }

    @Nested
    @DisplayName("Soft Delete Operations")
    inner class SoftDeleteOperations {

        @Test
        @DisplayName("Should soft delete by ID")
        fun shouldSoftDeleteById() {
            val saved = entityManager.persistAndFlush(%s)

            val deleted = repository.softDeleteAllByIds(
                    listOf(saved.id!!),
                    Instant.now(),
                    "testUser"
            )

            assertThat(deleted).isEqualTo(1)
        }

        @Test
        @DisplayName("Should hard delete by ID")
        fun shouldHardDeleteById() {
            val saved = entityManager.persistAndFlush(%s)
            val id = saved.id!!

            val deleted = repository.hardDeleteById(id)

            assertThat(deleted).isEqualTo(1)
            assertThat(repository.findById(id)).isEmpty
        }

        @Test
        @DisplayName("Should restore soft-deleted entity")
        fun shouldRestoreSoftDeleted() {
            val saved = entityManager.persistAndFlush(%s)
            repository.softDeleteAllByIds(
                    listOf(saved.id!!),
                    Instant.now(),
                    "testUser"
            )
            entityManager.flush()
            entityManager.clear()

            val restored = repository.restoreAllByIds(listOf(saved.id!!))

            assertThat(restored).isEqualTo(1)
        }

        @Test
        @DisplayName("Should return 0 when soft deleting non-existent IDs")
        fun shouldReturnZeroForNonExistentSoftDelete() {
            val deleted = repository.softDeleteAllByIds(
                    listOf(999999L),
                    Instant.now(),
                    "testUser"
            )

            assertThat(deleted).isZero()
        }

        @Test
        @DisplayName("Should return 0 when hard deleting non-existent ID")
        fun shouldReturnZeroForNonExistentHardDelete() {
            val deleted = repository.hardDeleteById(999999L)

            assertThat(deleted).isZero()
        }

        @Test
        @DisplayName("Should return 0 when restoring non-existent IDs")
        fun shouldReturnZeroForNonExistentRestore() {
            val restored = repository.restoreAllByIds(listOf(999999L))

            assertThat(restored).isZero()
        }
    }

    @Nested
    @DisplayName("Batch Operations")
    inner class BatchOperations {

        @Test
        @DisplayName("Should soft delete multiple entities")
        fun shouldSoftDeleteMultiple() {
            val entity1 = entityManager.persistAndFlush(%s.builder().estado(true)%s.build())
            val entity2 = entityManager.persistAndFlush(%s.builder().estado(true)%s.build())

            val deleted = repository.softDeleteAllByIds(
                    listOf(entity1.id!!, entity2.id!!),
                    Instant.now(),
                    "testUser"
            )

            assertThat(deleted).isEqualTo(2)
        }

        @Test
        @DisplayName("Should restore multiple entities")
        fun shouldRestoreMultiple() {
            val entity1 = entityManager.persistAndFlush(%s.builder().estado(true)%s.build())
            val entity2 = entityManager.persistAndFlush(%s.builder().estado(true)%s.build())

            repository.softDeleteAllByIds(
                    listOf(entity1.id!!, entity2.id!!),
                    Instant.now(),
                    "testUser"
            )
            entityManager.flush()
            entityManager.clear()

            val restored = repository.restoreAllByIds(listOf(entity1.id!!, entity2.id!!))

            assertThat(restored).isEqualTo(2)
        }
    }
}
"""
                .formatted(
                        // Package and imports
                        basePackage,
                        moduleName,
                        basePackage,
                        moduleName,
                        entityName,
                        // Class declaration
                        entityName,
                        entityName,
                        entityName,
                        // Fields
                        entityVarName,
                        entityName,
                        // setUp
                        entityVarName,
                        entityName,
                        fieldAssignments.toString(),
                        // Standard CRUD - save/retrieve
                        entityVarName,
                        // findAll
                        entityVarName,
                        // update
                        entityVarName,
                        // delete
                        entityVarName,
                        // exists
                        entityVarName,
                        // count
                        entityVarName,
                        // Soft Delete - softDeleteById
                        entityVarName,
                        // hardDeleteById
                        entityVarName,
                        // restore
                        entityVarName,
                        // Batch - softDeleteMultiple
                        entityName,
                        fieldAssignments.toString(),
                        entityName,
                        fieldAssignments.toString(),
                        // restoreMultiple
                        entityName,
                        fieldAssignments.toString(),
                        entityName,
                        fieldAssignments.toString());
    }
}
