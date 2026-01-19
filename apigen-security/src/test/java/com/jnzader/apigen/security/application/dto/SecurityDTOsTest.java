package com.jnzader.apigen.security.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Security DTOs Tests")
class SecurityDTOsTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("LoginRequestDTO")
    class LoginRequestDTOTests {

        @Test
        @DisplayName("should be valid with correct data")
        void shouldBeValidWithCorrectData() {
            LoginRequestDTO dto = new LoginRequestDTO("testuser", "password123");

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should be invalid with null username")
        void shouldBeInvalidWithNullUsername() {
            LoginRequestDTO dto = new LoginRequestDTO(null, "password123");

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should be invalid with blank username")
        void shouldBeInvalidWithBlankUsername() {
            LoginRequestDTO dto = new LoginRequestDTO("   ", "password123");

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isNotEmpty();
        }

        @Test
        @DisplayName("should be invalid with null password")
        void shouldBeInvalidWithNullPassword() {
            LoginRequestDTO dto = new LoginRequestDTO("testuser", null);

            Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
        }

        @Test
        @DisplayName("should get username and password")
        void shouldGetUsernameAndPassword() {
            LoginRequestDTO dto = new LoginRequestDTO("testuser", "password123");

            assertThat(dto.username()).isEqualTo("testuser");
            assertThat(dto.password()).isEqualTo("password123");
        }
    }

    @Nested
    @DisplayName("RegisterRequestDTO")
    class RegisterRequestDTOTests {

        @Test
        @DisplayName("should be valid with correct data")
        void shouldBeValidWithCorrectData() {
            RegisterRequestDTO dto =
                    new RegisterRequestDTO(
                            "testuser", "StrongP@ss123", "test@example.com", "John", "Doe");

            Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should be invalid with short username")
        void shouldBeInvalidWithShortUsername() {
            RegisterRequestDTO dto =
                    new RegisterRequestDTO(
                            "ab", "StrongP@ss123", "test@example.com", "John", "Doe");

            Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        }

        @Test
        @DisplayName("should be invalid with invalid email")
        void shouldBeInvalidWithInvalidEmail() {
            RegisterRequestDTO dto =
                    new RegisterRequestDTO(
                            "testuser", "StrongP@ss123", "invalid-email", "John", "Doe");

            Set<ConstraintViolation<RegisterRequestDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        }

        @Test
        @DisplayName("should get all fields")
        void shouldGetAllFields() {
            RegisterRequestDTO dto =
                    new RegisterRequestDTO(
                            "testuser", "StrongP@ss123", "test@example.com", "John", "Doe");

            assertThat(dto.username()).isEqualTo("testuser");
            assertThat(dto.password()).isEqualTo("StrongP@ss123");
            assertThat(dto.email()).isEqualTo("test@example.com");
            assertThat(dto.firstName()).isEqualTo("John");
            assertThat(dto.lastName()).isEqualTo("Doe");
        }
    }

    @Nested
    @DisplayName("RefreshTokenRequestDTO")
    class RefreshTokenRequestDTOTests {

        @Test
        @DisplayName("should be valid with refresh token")
        void shouldBeValidWithRefreshToken() {
            RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO("valid.refresh.token");

            Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(dto);

            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("should be invalid with null refresh token")
        void shouldBeInvalidWithNullRefreshToken() {
            RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO(null);

            Set<ConstraintViolation<RefreshTokenRequestDTO>> violations = validator.validate(dto);

            assertThat(violations)
                    .isNotEmpty()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("refreshToken"));
        }

        @Test
        @DisplayName("should get refresh token")
        void shouldGetRefreshToken() {
            RefreshTokenRequestDTO dto = new RefreshTokenRequestDTO("valid.refresh.token");

            assertThat(dto.refreshToken()).isEqualTo("valid.refresh.token");
        }
    }

    @Nested
    @DisplayName("AuthResponseDTO")
    class AuthResponseDTOTests {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            Instant expiresAt = Instant.now().plusSeconds(3600);
            AuthResponseDTO.UserInfoDTO userInfo =
                    new AuthResponseDTO.UserInfoDTO(
                            1L,
                            "testuser",
                            "test@example.com",
                            "Test User",
                            "USER",
                            Set.of("READ"));

            AuthResponseDTO dto =
                    new AuthResponseDTO(
                            "access.token", "refresh.token", "Bearer", expiresAt, userInfo);

            assertThat(dto.accessToken()).isEqualTo("access.token");
            assertThat(dto.refreshToken()).isEqualTo("refresh.token");
            assertThat(dto.tokenType()).isEqualTo("Bearer");
            assertThat(dto.expiresAt()).isEqualTo(expiresAt);
            assertThat(dto.user()).isEqualTo(userInfo);
        }

        @Test
        @DisplayName("should create with convenience constructor")
        void shouldCreateWithConvenienceConstructor() {
            Instant expiresAt = Instant.now().plusSeconds(3600);
            AuthResponseDTO.UserInfoDTO userInfo =
                    new AuthResponseDTO.UserInfoDTO(
                            1L,
                            "testuser",
                            "test@example.com",
                            "Test User",
                            "USER",
                            Set.of("READ"));

            AuthResponseDTO dto =
                    new AuthResponseDTO("access.token", "refresh.token", expiresAt, userInfo);

            assertThat(dto.tokenType()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("should create UserInfoDTO correctly")
        void shouldCreateUserInfoDTOCorrectly() {
            AuthResponseDTO.UserInfoDTO userInfo =
                    new AuthResponseDTO.UserInfoDTO(
                            1L,
                            "testuser",
                            "test@example.com",
                            "Test User",
                            "ADMIN",
                            Set.of("READ", "WRITE"));

            assertThat(userInfo.id()).isEqualTo(1L);
            assertThat(userInfo.username()).isEqualTo("testuser");
            assertThat(userInfo.email()).isEqualTo("test@example.com");
            assertThat(userInfo.fullName()).isEqualTo("Test User");
            assertThat(userInfo.role()).isEqualTo("ADMIN");
            assertThat(userInfo.permissions()).containsExactlyInAnyOrder("READ", "WRITE");
        }
    }
}
