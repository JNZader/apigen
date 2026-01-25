package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates unit test classes for Service implementations in Kotlin/Spring Boot. */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class KotlinServiceTestGenerator {

    private static final String APIGEN_CORE_PKG = "com.jnzader.apigen.core";

    private final String basePackage;

    public KotlinServiceTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the Service test class code in Kotlin. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String entityVarName = table.getEntityVariableName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.service

import %s.application.service.CacheEvictionService
import %s.application.util.Result
import %s.%s.domain.entity.%s
import %s.%s.infrastructure.repository.%sRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import jakarta.persistence.EntityManager
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.AuditorAware
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

import java.util.Optional

import org.assertj.core.api.Assertions.assertThat

@ExtendWith(MockitoExtension::class)
@DisplayName("%sService Tests")
class %sServiceImplTest {

    @Mock
    private lateinit var repository: %sRepository

    @Mock
    private lateinit var cacheEvictionService: CacheEvictionService

    @Mock
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    private lateinit var auditorAware: AuditorAware<String>

    @Mock
    private lateinit var entityManager: EntityManager

    private lateinit var service: %sServiceImpl

    private lateinit var %s: %s

    @BeforeEach
    fun setUp() {
        service = %sServiceImpl(repository, cacheEvictionService, eventPublisher, auditorAware)
        // Inject mock EntityManager for batch operations (saveAll uses flush/clear)
        ReflectionTestUtils.setField(service, "entityManager", entityManager)
        %s = %s().apply {
            id = 1L
            estado = true
        }
    }

    @Nested
    @DisplayName("Find Operations")
    inner class FindOperations {

        @Test
        @DisplayName("Should find %s by ID successfully")
        fun `should find %s by id`() {
            whenever(repository.findById(1L)).thenReturn(Optional.of(%s))

            val result: Result<%s, Exception> = service.findById(1L)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isEqualTo(%s)
            verify(repository).findById(1L)
        }

        @Test
        @DisplayName("Should return failure when %s not found by ID")
        fun `should return failure when not found`() {
            whenever(repository.findById(any())).thenReturn(Optional.empty())

            val result: Result<%s, Exception> = service.findById(999L)

            assertThat(result.isFailure).isTrue()
            verify(repository).findById(999L)
        }

        @Test
        @DisplayName("Should find all %s with pagination")
        fun `should find all with pagination`() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val page: Page<%s> = PageImpl(listOf(%s))
            whenever(repository.findAll(pageable)).thenReturn(page)

            val result: Result<Page<%s>, Exception> = service.findAll(pageable)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow().content).hasSize(1)
            verify(repository).findAll(pageable)
        }

        @Test
        @DisplayName("Should find all active %s with pagination")
        fun `should find all active with pagination`() {
            val pageable: Pageable = PageRequest.of(0, 10)
            val page: Page<%s> = PageImpl(listOf(%s))
            whenever(repository.findAll(any<Specification<%s>>(), eq(pageable))).thenReturn(page)

            val result: Result<Page<%s>, Exception> = service.findAllActive(pageable)

            assertThat(result.isSuccess).isTrue()
            verify(repository).findAll(any<Specification<%s>>(), eq(pageable))
        }

        @Test
        @DisplayName("Should check if %s exists by ID")
        fun `should check exists by id`() {
            whenever(repository.existsById(1L)).thenReturn(true)

            val result: Result<Boolean, Exception> = service.existsById(1L)

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isTrue()
            verify(repository).existsById(1L)
        }

        @Test
        @DisplayName("Should count all %s")
        fun `should count all`() {
            whenever(repository.count()).thenReturn(5L)

            val result: Result<Long, Exception> = service.count()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isEqualTo(5L)
            verify(repository).count()
        }

        @Test
        @DisplayName("Should count active %s")
        fun `should count active`() {
            whenever(repository.count(any<Specification<%s>>())).thenReturn(3L)

            val result: Result<Long, Exception> = service.countActive()

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isEqualTo(3L)
        }
    }

    @Nested
    @DisplayName("Save Operations")
    inner class SaveOperations {

        @Test
        @DisplayName("Should save new %s successfully")
        fun `should save new entity`() {
            val new%s = %s()
            whenever(repository.save(any())).thenReturn(%s)

            val result: Result<%s, Exception> = service.save(new%s)

            assertThat(result.isSuccess).isTrue()
            verify(repository).save(any())
        }

        @Test
        @DisplayName("Should update existing %s")
        fun `should update existing entity`() {
            whenever(repository.findById(1L)).thenReturn(Optional.of(%s))
            whenever(repository.save(any())).thenReturn(%s)

            val result: Result<%s, Exception> = service.update(1L, %s)

            assertThat(result.isSuccess).isTrue()
            verify(repository).findById(1L)
            verify(repository).save(any())
            verify(cacheEvictionService).evictListsByEntityName(any())
        }

        @Test
        @DisplayName("Should partial update %s")
        fun `should partial update entity`() {
            whenever(repository.findById(1L)).thenReturn(Optional.of(%s))
            whenever(repository.save(any())).thenReturn(%s)

            val result: Result<%s, Exception> = service.partialUpdate(1L, %s)

            assertThat(result.isSuccess).isTrue()
            verify(repository).findById(1L)
            verify(repository).save(any())
        }

        @Test
        @DisplayName("Should save all %s in batch")
        fun `should save all in batch`() {
            val entities = listOf(%s, %s())
            whenever(repository.saveAll(anyList())).thenReturn(entities)

            val result: Result<List<%s>, Exception> = service.saveAll(entities)

            assertThat(result.isSuccess).isTrue()
            verify(repository).saveAll(anyList())
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    inner class DeleteOperations {

        @Test
        @DisplayName("Should soft delete %s")
        fun `should soft delete entity`() {
            whenever(auditorAware.currentAuditor).thenReturn(Optional.of("testUser"))
            whenever(repository.findById(1L)).thenReturn(Optional.of(%s))
            whenever(repository.save(any())).thenReturn(%s)

            val result: Result<Void, Exception> = service.softDelete(1L)

            assertThat(result.isSuccess).isTrue()
            verify(repository).findById(1L)
            verify(repository).save(any())
            verify(cacheEvictionService).evictListsByEntityName(any())
        }

        @Test
        @DisplayName("Should hard delete %s")
        fun `should hard delete entity`() {
            whenever(repository.hardDeleteById(1L)).thenReturn(1)

            val result: Result<Void, Exception> = service.hardDelete(1L)

            assertThat(result.isSuccess).isTrue()
            verify(repository).hardDeleteById(1L)
        }

        @Test
        @DisplayName("Should soft delete all %s in batch")
        fun `should soft delete all in batch`() {
            whenever(auditorAware.currentAuditor).thenReturn(Optional.of("testUser"))
            whenever(repository.softDeleteAllByIds(anyList(), any(), any())).thenReturn(2)

            val result: Result<Int, Exception> = service.softDeleteAll(listOf(1L, 2L))

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("Restore Operations")
    inner class RestoreOperations {

        @Test
        @DisplayName("Should restore all %s in batch")
        fun `should restore all in batch`() {
            whenever(repository.restoreAllByIds(anyList())).thenReturn(2)

            val result: Result<Int, Exception> = service.restoreAll(listOf(1L, 2L))

            assertThat(result.isSuccess).isTrue()
            assertThat(result.orElseThrow()).isEqualTo(2)
        }
    }
}
"""
                .formatted(
                        basePackage,
                        moduleName,
                        APIGEN_CORE_PKG,
                        APIGEN_CORE_PKG,
                        basePackage,
                        moduleName,
                        entityName,
                        basePackage,
                        moduleName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        // Find Operations
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        // Save Operations
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityVarName,
                        entityName,
                        entityName,
                        // Delete Operations
                        entityName,
                        entityVarName,
                        entityVarName,
                        entityName,
                        entityName,
                        // Restore Operations
                        entityName);
    }
}
