package com.jnzader.apigen.core.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.jnzader.apigen.core.application.validation.ValidationGroups;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests de validación para DTOs.
 *
 * <p>Verifica que las anotaciones de validación funcionan correctamente para diferentes grupos de
 * validación (Create, Update, PartialUpdate).
 */
@DisplayName("DTO Validation Tests")
@Tag("integration")
class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // DTO de prueba con anotaciones de validación (las anotaciones en interfaces NO se heredan a
    // records)
    record TestValidatedDTO(
            @jakarta.validation.constraints.Null(
                            groups = ValidationGroups.Create.class,
                            message = "ID debe ser nulo al crear una nueva entidad")
                    @jakarta.validation.constraints.NotNull(
                            groups = ValidationGroups.Update.class,
                            message = "ID es requerido para actualizar")
                    @jakarta.validation.constraints.Positive(
                            message = "ID debe ser un número positivo")
                    Long id,
            Boolean activo,
            String name)
            implements BaseDTOValidated {}

    // ==================== Create Validation Tests ====================

    @Nested
    @DisplayName("Create Validation Group")
    class CreateValidationTests {

        @Test
        @DisplayName("should fail when ID is not null on create")
        void shouldFailWhenIdNotNullOnCreate() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(1L, true, "Test");

            // When
            Set<ConstraintViolation<TestValidatedDTO>> violations =
                    validator.validate(dto, ValidationGroups.Create.class);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("ID debe ser nulo al crear una nueva entidad");
        }

        @Test
        @DisplayName("should pass when ID is null on create")
        void shouldPassWhenIdNullOnCreate() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(null, true, "Test");

            // When
            Set<ConstraintViolation<TestValidatedDTO>> violations =
                    validator.validate(dto, ValidationGroups.Create.class);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    // ==================== Update Validation Tests ====================

    @Nested
    @DisplayName("Update Validation Group")
    class UpdateValidationTests {

        @Test
        @DisplayName("should fail when ID is null on update")
        void shouldFailWhenIdNullOnUpdate() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(null, true, "Test");

            // When
            Set<ConstraintViolation<TestValidatedDTO>> violations =
                    validator.validate(dto, ValidationGroups.Update.class);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("ID es requerido para actualizar");
        }

        @Test
        @DisplayName("should pass when ID is present on update")
        void shouldPassWhenIdPresentOnUpdate() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(1L, true, "Test");

            // When
            Set<ConstraintViolation<TestValidatedDTO>> violations =
                    validator.validate(dto, ValidationGroups.Update.class);

            // Then
            assertThat(violations).isEmpty();
        }
    }

    // ==================== ID Validation Tests ====================

    @Nested
    @DisplayName("ID Validation")
    class IdValidationTests {

        @Test
        @DisplayName("should fail when ID is negative")
        void shouldFailWhenIdNegative() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(-1L, true, "Test");

            // When - validate without groups to check @Positive
            Set<ConstraintViolation<TestValidatedDTO>> violations = validator.validate(dto);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("ID debe ser un número positivo");
        }

        @Test
        @DisplayName("should fail when ID is zero")
        void shouldFailWhenIdZero() {
            // Given
            TestValidatedDTO dto = new TestValidatedDTO(0L, true, "Test");

            // When
            Set<ConstraintViolation<TestValidatedDTO>> violations = validator.validate(dto);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("ID debe ser un número positivo");
        }
    }

    // ==================== ValidationGroups Tests ====================

    @Nested
    @DisplayName("ValidationGroups Interface")
    class ValidationGroupsTests {

        @Test
        @DisplayName("Create group should extend jakarta Default")
        void createGroupShouldExtendJakartaDefault() {
            // Verify that Create interface extends jakarta.validation.groups.Default
            assertThat(ValidationGroups.Create.class.getInterfaces())
                    .contains(jakarta.validation.groups.Default.class);
        }

        @Test
        @DisplayName("Update group should extend jakarta Default")
        void updateGroupShouldExtendJakartaDefault() {
            // Verify that Update interface extends jakarta.validation.groups.Default
            assertThat(ValidationGroups.Update.class.getInterfaces())
                    .contains(jakarta.validation.groups.Default.class);
        }

        @Test
        @DisplayName("PartialUpdate group should exist")
        void partialUpdateGroupShouldExist() {
            // Verify PartialUpdate interface exists
            assertThat(ValidationGroups.PartialUpdate.class).isInterface();
        }

        @Test
        @DisplayName("All validation groups should exist")
        void allValidationGroupsShouldExist() {
            // Verify all groups exist
            assertThat(ValidationGroups.Create.class).isInterface();
            assertThat(ValidationGroups.Update.class).isInterface();
            assertThat(ValidationGroups.PartialUpdate.class).isInterface();
            assertThat(ValidationGroups.Delete.class).isInterface();
            assertThat(ValidationGroups.Search.class).isInterface();
            assertThat(ValidationGroups.Import.class).isInterface();
        }
    }

    // ==================== Complex DTO Validation Example ====================

    @Nested
    @DisplayName("Complex DTO Validation")
    class ComplexDTOValidationTests {

        // Ejemplo de DTO con múltiples validaciones
        record ComplexDTO(
                Long id,
                Boolean activo,
                @jakarta.validation.constraints.NotBlank(
                                groups = {
                                    ValidationGroups.Create.class,
                                    ValidationGroups.Update.class
                                },
                                message = "Name is required")
                        @jakarta.validation.constraints.Size(
                                min = 2,
                                max = 100,
                                message = "Name must be between 2 and 100 characters")
                        String name,
                @jakarta.validation.constraints.Email(message = "Invalid email format")
                        String email,
                @jakarta.validation.constraints.Min(value = 0, message = "Age must be positive")
                        @jakarta.validation.constraints.Max(
                                value = 150,
                                message = "Age must be realistic")
                        Integer age)
                implements BaseDTOValidated {}

        @Test
        @DisplayName("should validate all fields correctly on create")
        void shouldValidateAllFieldsCorrectlyOnCreate() {
            // Given - valid DTO for creation
            ComplexDTO validDto =
                    new ComplexDTO(
                            null, // ID null for create
                            true,
                            "Valid Name", // Valid name
                            "test@email.com", // Valid email
                            25 // Valid age
                            );

            // When
            Set<ConstraintViolation<ComplexDTO>> violations =
                    validator.validate(validDto, ValidationGroups.Create.class);

            // Then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should collect multiple violations")
        void shouldCollectMultipleViolations() {
            // Given - DTO with multiple issues
            ComplexDTO invalidDto =
                    new ComplexDTO(
                            -1L, // Invalid: negative ID
                            true,
                            "X", // Invalid: too short
                            "invalid-email", // Invalid: not an email
                            200 // Invalid: too high
                            );

            // When
            Set<ConstraintViolation<ComplexDTO>> violations = validator.validate(invalidDto);

            // Then
            assertThat(violations).hasSizeGreaterThan(1);
        }

        @Test
        @DisplayName("should validate email format")
        void shouldValidateEmailFormat() {
            // Given
            ComplexDTO dtoWithInvalidEmail =
                    new ComplexDTO(null, true, "Valid Name", "not-an-email", 25);

            // When
            Set<ConstraintViolation<ComplexDTO>> violations =
                    validator.validate(dtoWithInvalidEmail);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Invalid email format");
        }

        @Test
        @DisplayName("should validate name length")
        void shouldValidateNameLength() {
            // Given - name too short
            ComplexDTO dtoWithShortName = new ComplexDTO(null, true, "A", "test@email.com", 25);

            // When
            Set<ConstraintViolation<ComplexDTO>> violations = validator.validate(dtoWithShortName);

            // Then
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("Name must be between 2 and 100 characters");
        }
    }
}
