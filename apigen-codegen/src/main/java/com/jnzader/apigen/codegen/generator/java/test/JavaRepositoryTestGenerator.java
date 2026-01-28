package com.jnzader.apigen.codegen.generator.java.test;

import static com.jnzader.apigen.codegen.generator.util.TestValueProvider.getSampleTestValue;

import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Repository implementations in Java/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class JavaRepositoryTestGenerator {

    private final String basePackage;

    public JavaRepositoryTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Repository test class code. */
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
package %s.%s.infrastructure.repository;

import %s.%s.domain.entity.%s;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("%sRepository Tests")
class %sRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private %sRepository repository;

    private %s %s;

    @BeforeEach
    void setUp() {
        %s = %s.builder()%s
                .build();
        %s.setEstado(true);
    }

    @Nested
    @DisplayName("Standard CRUD Operations")
    class StandardCrudOperations {

        @Test
        @DisplayName("Should save and retrieve entity")
        void shouldSaveAndRetrieve() {
            %s saved = entityManager.persistAndFlush(%s);

            Optional<%s> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Should find all entities")
        void shouldFindAll() {
            entityManager.persistAndFlush(%s);

            List<%s> all = repository.findAll();

            assertThat(all).isNotEmpty();
        }

        @Test
        @DisplayName("Should update entity")
        void shouldUpdate() {
            %s saved = entityManager.persistAndFlush(%s);
            saved.setEstado(false);
            repository.save(saved);
            entityManager.flush();
            entityManager.clear();

            Optional<%s> updated = repository.findById(saved.getId());

            assertThat(updated).isPresent();
            assertThat(updated.get().getEstado()).isFalse();
        }

        @Test
        @DisplayName("Should delete entity")
        void shouldDelete() {
            %s saved = entityManager.persistAndFlush(%s);
            Long id = saved.getId();

            repository.deleteById(id);
            entityManager.flush();

            Optional<%s> deleted = repository.findById(id);

            assertThat(deleted).isEmpty();
        }

        @Test
        @DisplayName("Should check if entity exists by ID")
        void shouldCheckExistsById() {
            %s saved = entityManager.persistAndFlush(%s);

            boolean exists = repository.existsById(saved.getId());

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existent ID")
        void shouldReturnFalseForNonExistentId() {
            boolean exists = repository.existsById(999999L);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should count all entities")
        void shouldCountAll() {
            entityManager.persistAndFlush(%s);

            long count = repository.count();

            assertThat(count).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Soft Delete Operations")
    class SoftDeleteOperations {

        @Test
        @DisplayName("Should soft delete by ID")
        void shouldSoftDeleteById() {
            %s saved = entityManager.persistAndFlush(%s);

            int deleted = repository.softDeleteAllByIds(
                    List.of(saved.getId()),
                    LocalDateTime.now(),
                    "testUser"
            );

            assertThat(deleted).isEqualTo(1);
        }

        @Test
        @DisplayName("Should hard delete by ID")
        void shouldHardDeleteById() {
            %s saved = entityManager.persistAndFlush(%s);
            Long id = saved.getId();

            int deleted = repository.hardDeleteById(id);

            assertThat(deleted).isEqualTo(1);
            assertThat(repository.findById(id)).isEmpty();
        }

        @Test
        @DisplayName("Should restore soft-deleted entity")
        void shouldRestoreSoftDeleted() {
            %s saved = entityManager.persistAndFlush(%s);
            repository.softDeleteAllByIds(
                    List.of(saved.getId()),
                    LocalDateTime.now(),
                    "testUser"
            );
            entityManager.flush();
            entityManager.clear();

            int restored = repository.restoreAllByIds(List.of(saved.getId()));

            assertThat(restored).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return 0 when soft deleting non-existent IDs")
        void shouldReturnZeroForNonExistentSoftDelete() {
            int deleted = repository.softDeleteAllByIds(
                    List.of(999999L),
                    LocalDateTime.now(),
                    "testUser"
            );

            assertThat(deleted).isZero();
        }

        @Test
        @DisplayName("Should return 0 when hard deleting non-existent ID")
        void shouldReturnZeroForNonExistentHardDelete() {
            int deleted = repository.hardDeleteById(999999L);

            assertThat(deleted).isZero();
        }

        @Test
        @DisplayName("Should return 0 when restoring non-existent IDs")
        void shouldReturnZeroForNonExistentRestore() {
            int restored = repository.restoreAllByIds(List.of(999999L));

            assertThat(restored).isZero();
        }
    }

    @Nested
    @DisplayName("Batch Operations")
    class BatchOperations {

        @Test
        @DisplayName("Should soft delete multiple entities")
        void shouldSoftDeleteMultiple() {
            %s entity1 = %s.builder()%s.build();
            entity1.setEstado(true);
            entity1 = entityManager.persistAndFlush(entity1);
            %s entity2 = %s.builder()%s.build();
            entity2.setEstado(true);
            entity2 = entityManager.persistAndFlush(entity2);

            int deleted = repository.softDeleteAllByIds(
                    List.of(entity1.getId(), entity2.getId()),
                    LocalDateTime.now(),
                    "testUser"
            );

            assertThat(deleted).isEqualTo(2);
        }

        @Test
        @DisplayName("Should restore multiple entities")
        void shouldRestoreMultiple() {
            %s entity1 = %s.builder()%s.build();
            entity1.setEstado(true);
            entity1 = entityManager.persistAndFlush(entity1);
            %s entity2 = %s.builder()%s.build();
            entity2.setEstado(true);
            entity2 = entityManager.persistAndFlush(entity2);

            repository.softDeleteAllByIds(
                    List.of(entity1.getId(), entity2.getId()),
                    LocalDateTime.now(),
                    "testUser"
            );
            entityManager.flush();
            entityManager.clear();

            int restored = repository.restoreAllByIds(List.of(entity1.getId(), entity2.getId()));

            assertThat(restored).isEqualTo(2);
        }
    }
}
"""
                .formatted(
                        // Package and imports (5)
                        basePackage,
                        moduleName,
                        basePackage,
                        moduleName,
                        entityName,
                        // Class declaration (4)
                        entityName,
                        entityName,
                        entityName,
                        // Fields (2)
                        entityName,
                        entityVarName,
                        // setUp (4)
                        entityVarName,
                        entityName,
                        fieldAssignments.toString(),
                        entityVarName,
                        // Standard CRUD - save/retrieve (3)
                        entityName,
                        entityVarName,
                        entityName,
                        // findAll (2)
                        entityVarName,
                        entityName,
                        // update (3)
                        entityName,
                        entityVarName,
                        entityName,
                        // delete (3)
                        entityName,
                        entityVarName,
                        entityName,
                        // exists (2)
                        entityName,
                        entityVarName,
                        // count (1)
                        entityVarName,
                        // Soft Delete - softDeleteById (2)
                        entityName,
                        entityVarName,
                        // hardDeleteById (2)
                        entityName,
                        entityVarName,
                        // restore (2)
                        entityName,
                        entityVarName,
                        // Batch - softDeleteMultiple (6)
                        entityName,
                        entityName,
                        fieldAssignments.toString(),
                        entityName,
                        entityName,
                        fieldAssignments.toString(),
                        // restoreMultiple (6)
                        entityName,
                        entityName,
                        fieldAssignments.toString(),
                        entityName,
                        entityName,
                        fieldAssignments.toString());
    }
}
