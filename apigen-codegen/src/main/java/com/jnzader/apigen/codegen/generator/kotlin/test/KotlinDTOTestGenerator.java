package com.jnzader.apigen.codegen.generator.kotlin.test;

import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates DTO test classes for Kotlin/Spring Boot. */
public class KotlinDTOTestGenerator {

    private final String basePackage;

    public KotlinDTOTestGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /** Generates the DTO test class code in Kotlin. */
    public String generate(SqlTable table) {
        String entityName = table.getEntityName();
        String moduleName = table.getModuleName();

        return
"""
package %s.%s.application.dto

import com.jnzader.apigen.core.application.validation.ValidationGroups
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("%sDTO Validation Tests")
class %sDTOTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Nested
    @DisplayName("Create Validation")
    inner class CreateValidation {

        @Test
        @DisplayName("Should pass when id is null for create")
        fun `should pass when id is null for create`() {
            val dto = %sDTO(id = null, activo = true)

            val violations = validator.validate(dto, ValidationGroups.Create::class.java)

            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("Should fail when id is present for create")
        fun `should fail when id is present for create`() {
            val dto = %sDTO(id = 1L, activo = true)

            val violations = validator.validate(dto, ValidationGroups.Create::class.java)

            assertThat(violations)
                .anyMatch { it.propertyPath.toString() == "id" }
        }
    }

    @Nested
    @DisplayName("Update Validation")
    inner class UpdateValidation {

        @Test
        @DisplayName("Should pass when id is present for update")
        fun `should pass when id is present for update`() {
            val dto = %sDTO(id = 1L, activo = true)

            val violations = validator.validate(dto, ValidationGroups.Update::class.java)

            assertThat(violations).isEmpty()
        }

        @Test
        @DisplayName("Should fail when id is null for update")
        fun `should fail when id is null for update`() {
            val dto = %sDTO(id = null, activo = true)

            val violations = validator.validate(dto, ValidationGroups.Update::class.java)

            assertThat(violations)
                .anyMatch { it.propertyPath.toString() == "id" }
        }
    }

    @Nested
    @DisplayName("BaseDTO Interface")
    inner class BaseDTOInterface {

        @Test
        @DisplayName("Should implement id() method correctly")
        fun `should implement id method correctly`() {
            val dto = %sDTO(id = 42L, activo = true)

            assertThat(dto.id()).isEqualTo(42L)
        }

        @Test
        @DisplayName("Should implement activo() method correctly")
        fun `should implement activo method correctly`() {
            val dto = %sDTO(id = 1L, activo = false)

            assertThat(dto.activo()).isFalse()
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
                        entityName,
                        entityName,
                        entityName,
                        entityName,
                        entityName);
    }
}
