package com.jnzader.apigen.security.infrastructure.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.application.dto.AuthResponseDTO;
import com.jnzader.apigen.security.application.dto.LoginRequestDTO;
import com.jnzader.apigen.security.application.dto.RefreshTokenRequestDTO;
import com.jnzader.apigen.security.application.dto.RegisterRequestDTO;
import com.jnzader.apigen.security.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Unit tests for AuthController.
 *
 * <p>Tests the controller layer in isolation with mocked AuthService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock private AuthService authService;

    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks private AuthController authController;

    private AuthResponseDTO mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse =
                new AuthResponseDTO(
                        "access-token-123",
                        "refresh-token-456",
                        Instant.now().plusSeconds(900),
                        new AuthResponseDTO.UserInfoDTO(
                                1L,
                                "testuser",
                                "test@example.com",
                                "Test User",
                                "USER",
                                Set.of("READ")));
    }

    @Nested
    @DisplayName("Login Endpoint")
    class LoginEndpointTests {

        @Test
        @DisplayName("should return 200 OK on successful login")
        void shouldReturnOkOnSuccessfulLogin() {
            LoginRequestDTO request = new LoginRequestDTO("testuser", "password123");
            when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockAuthResponse);

            ResponseEntity<AuthResponseDTO> response = authController.login(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isEqualTo("access-token-123");
            assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token-456");
            assertThat(response.getBody().user().username()).isEqualTo("testuser");

            verify(authService).login(request);
        }

        @Test
        @DisplayName("should propagate BadCredentialsException")
        void shouldPropagateBadCredentialsException() {
            LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword");
            when(authService.login(any(LoginRequestDTO.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authController.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("should call service with correct request")
        void shouldCallServiceWithCorrectRequest() {
            LoginRequestDTO request = new LoginRequestDTO("myuser", "mypassword");
            when(authService.login(any(LoginRequestDTO.class))).thenReturn(mockAuthResponse);

            authController.login(request);

            verify(authService)
                    .login(
                            argThat(
                                    req ->
                                            req.username().equals("myuser")
                                                    && req.password().equals("mypassword")));
        }
    }

    @Nested
    @DisplayName("Register Endpoint")
    class RegisterEndpointTests {

        @Test
        @DisplayName("should return 200 OK on successful registration")
        void shouldReturnOkOnSuccessfulRegistration() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "newuser", "Password123!", "new@example.com", "New", "User");
            when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockAuthResponse);

            ResponseEntity<AuthResponseDTO> response = authController.register(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotNull();

            verify(authService).register(request);
        }

        @Test
        @DisplayName("should propagate RuntimeException for duplicate username")
        void shouldPropagateRuntimeExceptionForDuplicateUsername() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "existinguser", "Password123!", "new@example.com", "New", "User");
            when(authService.register(any(RegisterRequestDTO.class)))
                    .thenThrow(new RuntimeException("El nombre de usuario ya existe"));

            assertThatThrownBy(() -> authController.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("nombre de usuario ya existe");
        }

        @Test
        @DisplayName("should call service with all registration fields")
        void shouldCallServiceWithAllRegistrationFields() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "newuser", "Password123!", "new@example.com", "New", "User");
            when(authService.register(any(RegisterRequestDTO.class))).thenReturn(mockAuthResponse);

            authController.register(request);

            verify(authService)
                    .register(
                            argThat(
                                    req ->
                                            req.username().equals("newuser")
                                                    && req.password().equals("Password123!")
                                                    && req.email().equals("new@example.com")
                                                    && req.firstName().equals("New")
                                                    && req.lastName().equals("User")));
        }
    }

    @Nested
    @DisplayName("Refresh Token Endpoint")
    class RefreshTokenEndpointTests {

        @Test
        @DisplayName("should return 200 OK on successful token refresh")
        void shouldReturnOkOnSuccessfulTokenRefresh() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("valid-refresh-token");
            when(authService.refreshToken(any(RefreshTokenRequestDTO.class)))
                    .thenReturn(mockAuthResponse);

            ResponseEntity<AuthResponseDTO> response = authController.refreshToken(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotNull();
            assertThat(response.getBody().refreshToken()).isNotNull();

            verify(authService).refreshToken(request);
        }

        @Test
        @DisplayName("should propagate RuntimeException for invalid refresh token")
        void shouldPropagateRuntimeExceptionForInvalidToken() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("invalid-token");
            when(authService.refreshToken(any(RefreshTokenRequestDTO.class)))
                    .thenThrow(new RuntimeException("Refresh token inválido o expirado"));

            assertThatThrownBy(() -> authController.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("inválido o expirado");
        }

        @Test
        @DisplayName("should propagate RuntimeException for blacklisted token")
        void shouldPropagateRuntimeExceptionForBlacklistedToken() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("blacklisted-token");
            when(authService.refreshToken(any(RefreshTokenRequestDTO.class)))
                    .thenThrow(new RuntimeException("Refresh token ya fue utilizado o revocado"));

            assertThatThrownBy(() -> authController.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("ya fue utilizado o revocado");
        }
    }

    @Nested
    @DisplayName("Logout Endpoint")
    class LogoutEndpointTests {

        @Test
        @DisplayName("should return 204 No Content on successful logout")
        void shouldReturnNoContentOnSuccessfulLogout() {
            when(httpServletRequest.getHeader("Authorization"))
                    .thenReturn("Bearer valid-token-123");
            doNothing().when(authService).logout(anyString());

            ResponseEntity<Void> response = authController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();

            verify(authService).logout("valid-token-123");
        }

        @Test
        @DisplayName("should return 204 even without Authorization header")
        void shouldReturnNoContentWithoutAuthHeader() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn(null);

            ResponseEntity<Void> response = authController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService, never()).logout(anyString());
        }

        @Test
        @DisplayName("should not call logout with non-Bearer token")
        void shouldNotCallLogoutWithNonBearerToken() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            ResponseEntity<Void> response = authController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService, never()).logout(anyString());
        }

        @Test
        @DisplayName("should extract token correctly from Bearer prefix")
        void shouldExtractTokenCorrectlyFromBearerPrefix() {
            String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
            when(httpServletRequest.getHeader("Authorization"))
                    .thenReturn("Bearer " + expectedToken);
            doNothing().when(authService).logout(anyString());

            authController.logout(httpServletRequest);

            verify(authService).logout(expectedToken);
        }

        @Test
        @DisplayName("should handle empty Bearer token")
        void shouldHandleEmptyBearerToken() {
            when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer ");
            doNothing().when(authService).logout(anyString());

            ResponseEntity<Void> response = authController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(authService).logout("");
        }
    }
}
