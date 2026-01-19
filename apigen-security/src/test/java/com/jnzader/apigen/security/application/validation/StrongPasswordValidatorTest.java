package com.jnzader.apigen.security.application.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests para StrongPasswordValidator. */
@DisplayName("StrongPasswordValidator Tests")
class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;
    private ConstraintValidatorContext context;
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    private StrongPassword annotation;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        context = mock(ConstraintValidatorContext.class);
        violationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        annotation = mock(StrongPassword.class);

        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);

        // Default annotation values
        when(annotation.minLength()).thenReturn(12);
        when(annotation.maxLength()).thenReturn(128);

        validator.initialize(annotation);
    }

    @Nested
    @DisplayName("Valid Passwords")
    class ValidPasswordsTests {

        @ParameterizedTest
        @ValueSource(
                strings = {
                    "SecurePass123!",
                    "MyP@ssw0rd2024",
                    "Complex#Pass99",
                    "Str0ng&Password!",
                    "Valid123$Password",
                    "SuperSecure@123ABC"
                })
        @DisplayName("should accept valid strong passwords")
        void shouldAcceptValidStrongPasswords(String password) {
            assertThat(validator.isValid(password, context)).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid Passwords - Length")
    class InvalidLengthTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should reject null or empty passwords")
        void shouldRejectNullOrEmptyPasswords(String password) {
            assertThat(validator.isValid(password, context)).isFalse();
        }

        @Test
        @DisplayName("should reject password shorter than minimum")
        void shouldRejectShortPassword() {
            assertThat(validator.isValid("Short1$", context)).isFalse();
            verify(context).disableDefaultConstraintViolation();
        }

        @Test
        @DisplayName("should reject password longer than maximum")
        void shouldRejectLongPassword() {
            String longPassword = "A1$" + "a".repeat(130);
            assertThat(validator.isValid(longPassword, context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Invalid Passwords - Missing Requirements")
    class MissingRequirementsTests {

        @Test
        @DisplayName("should reject password without uppercase")
        void shouldRejectWithoutUppercase() {
            assertThat(validator.isValid("nouppercase123!", context)).isFalse();
        }

        @Test
        @DisplayName("should reject password without lowercase")
        void shouldRejectWithoutLowercase() {
            assertThat(validator.isValid("NOLOWERCASE123!", context)).isFalse();
        }

        @Test
        @DisplayName("should reject password without digit")
        void shouldRejectWithoutDigit() {
            assertThat(validator.isValid("NoDigitsHere!!", context)).isFalse();
        }

        @Test
        @DisplayName("should reject password without special character")
        void shouldRejectWithoutSpecialChar() {
            assertThat(validator.isValid("NoSpecialChar123", context)).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("should accept password at minimum length boundary")
        void shouldAcceptAtMinimumLength() {
            // Exactly 12 characters with all requirements
            assertThat(validator.isValid("Abcdefgh1!23", context)).isTrue();
        }

        @Test
        @DisplayName("should accept password with multiple special characters")
        void shouldAcceptMultipleSpecialChars() {
            assertThat(validator.isValid("P@ss#w0rd$123!", context)).isTrue();
        }

        @Test
        @DisplayName("should accept password with unicode special characters")
        void shouldAcceptUnicodeSpecialChars() {
            assertThat(validator.isValid("Passw0rd123@#", context)).isTrue();
        }
    }
}
