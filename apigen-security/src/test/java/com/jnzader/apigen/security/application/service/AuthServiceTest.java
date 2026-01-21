package com.jnzader.apigen.security.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.jnzader.apigen.security.application.dto.AuthResponseDTO;
import com.jnzader.apigen.security.application.dto.LoginRequestDTO;
import com.jnzader.apigen.security.application.dto.RefreshTokenRequestDTO;
import com.jnzader.apigen.security.application.dto.RegisterRequestDTO;
import com.jnzader.apigen.security.domain.entity.Permission;
import com.jnzader.apigen.security.domain.entity.Role;
import com.jnzader.apigen.security.domain.entity.TokenBlacklist.BlacklistReason;
import com.jnzader.apigen.security.domain.entity.User;
import com.jnzader.apigen.security.domain.repository.RoleRepository;
import com.jnzader.apigen.security.domain.repository.UserRepository;
import com.jnzader.apigen.security.infrastructure.jwt.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Tests para AuthService. */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private RoleRepository roleRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @Mock private AuthenticationManager authenticationManager;

    @Mock private TokenBlacklistService tokenBlacklistService;

    @InjectMocks private AuthService authService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = createTestRole();
        testUser = createTestUser();
    }

    private Role createTestRole() {
        Permission readPermission = new Permission();
        readPermission.setId(1L);
        readPermission.setName("READ");

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setPermissions(Set.of(readPermission));
        return role;
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(testRole);
        user.setEnabled(true);
        return user;
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            LoginRequestDTO request = new LoginRequestDTO("testuser", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(new UsernamePasswordAuthenticationToken("testuser", "password123"));
            when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
            when(jwtService.extractExpiration("access-token"))
                    .thenReturn(Instant.now().plusSeconds(900));

            AuthResponseDTO response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user()).isNotNull();
            assertThat(response.user().username()).isEqualTo("testuser");
            assertThat(response.user().email()).isEqualTo("test@example.com");

            verify(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("should fail login with invalid credentials")
        void shouldFailLoginWithInvalidCredentials() {
            LoginRequestDTO request = new LoginRequestDTO("testuser", "wrongpassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findActiveByUsername(anyString());
        }

        @Test
        @DisplayName("should fail login when user not found")
        void shouldFailLoginWhenUserNotFound() {
            LoginRequestDTO request = new LoginRequestDTO("nonexistent", "password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(
                            new UsernamePasswordAuthenticationToken("nonexistent", "password123"));
            when(userRepository.findActiveByUsername("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "newuser", "password123", "new@example.com", "New", "User");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(testRole));
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(
                            invocation -> {
                                User user = invocation.getArgument(0);
                                user.setId(2L);
                                return user;
                            });
            when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
            when(jwtService.extractExpiration("access-token"))
                    .thenReturn(Instant.now().plusSeconds(900));

            AuthResponseDTO response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("access-token");

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
            assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
            assertThat(savedUser.getRole()).isEqualTo(testRole);
            assertThat(savedUser.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should fail registration when username already exists")
        void shouldFailRegistrationWhenUsernameExists() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "testuser", "password123", "new@example.com", "New", "User");

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fail registration when email already exists")
        void shouldFailRegistrationWhenEmailExists() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "newuser", "password123", "test@example.com", "New", "User");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should fail registration when default role not found")
        void shouldFailRegistrationWhenDefaultRoleNotFound() {
            RegisterRequestDTO request =
                    new RegisterRequestDTO(
                            "newuser", "password123", "new@example.com", "New", "User");

            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Default role not found");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            String oldRefreshToken = "old-refresh-token";
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(oldRefreshToken);

            when(jwtService.isRefreshToken(oldRefreshToken)).thenReturn(true);
            when(jwtService.isTokenStructureValid(oldRefreshToken)).thenReturn(true);
            when(jwtService.extractTokenId(oldRefreshToken)).thenReturn("token-id-123");
            when(tokenBlacklistService.isBlacklisted("token-id-123")).thenReturn(false);
            when(jwtService.extractUsername(oldRefreshToken)).thenReturn("testuser");
            when(userRepository.findActiveByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(jwtService.extractExpiration(oldRefreshToken))
                    .thenReturn(Instant.now().plusSeconds(3600));
            when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");
            when(jwtService.extractExpiration("new-access-token"))
                    .thenReturn(Instant.now().plusSeconds(900));

            AuthResponseDTO response = authService.refreshToken(request);

            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo("new-access-token");
            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");

            // Verify token rotation: old token blacklisted
            verify(tokenBlacklistService)
                    .blacklistToken(
                            eq("token-id-123"),
                            eq("testuser"),
                            any(Instant.class),
                            eq(BlacklistReason.TOKEN_ROTATED));
        }

        @Test
        @DisplayName("should fail refresh when token is not a refresh token")
        void shouldFailRefreshWhenNotRefreshToken() {
            String accessToken = "access-token";
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(accessToken);

            when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not a refresh token");
        }

        @Test
        @DisplayName("should fail refresh when token is invalid")
        void shouldFailRefreshWhenTokenInvalid() {
            String invalidToken = "invalid-token";
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(invalidToken);

            when(jwtService.isRefreshToken(invalidToken)).thenReturn(true);
            when(jwtService.isTokenStructureValid(invalidToken)).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        @DisplayName("should fail refresh when token is blacklisted")
        void shouldFailRefreshWhenTokenBlacklisted() {
            String blacklistedToken = "blacklisted-token";
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(blacklistedToken);

            when(jwtService.isRefreshToken(blacklistedToken)).thenReturn(true);
            when(jwtService.isTokenStructureValid(blacklistedToken)).thenReturn(true);
            when(jwtService.extractTokenId(blacklistedToken)).thenReturn("token-id-123");
            when(tokenBlacklistService.isBlacklisted("token-id-123")).thenReturn(true);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already used or revoked");
        }

        @Test
        @DisplayName("should fail refresh when user not found")
        void shouldFailRefreshWhenUserNotFound() {
            String refreshToken = "valid-refresh-token";
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(refreshToken);

            when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
            when(jwtService.isTokenStructureValid(refreshToken)).thenReturn(true);
            when(jwtService.extractTokenId(refreshToken)).thenReturn("token-id-123");
            when(tokenBlacklistService.isBlacklisted("token-id-123")).thenReturn(false);
            when(jwtService.extractUsername(refreshToken)).thenReturn("deleteduser");
            when(userRepository.findActiveByUsername("deleteduser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("should logout successfully")
        void shouldLogoutSuccessfully() {
            String token = "access-token";

            when(jwtService.extractTokenId(token)).thenReturn("token-id-123");
            when(jwtService.extractUsername(token)).thenReturn("testuser");
            when(jwtService.extractExpiration(token)).thenReturn(Instant.now().plusSeconds(900));

            authService.logout(token);

            verify(tokenBlacklistService)
                    .blacklistToken(
                            eq("token-id-123"),
                            eq("testuser"),
                            any(Instant.class),
                            eq(BlacklistReason.LOGOUT));
        }
    }

    @Nested
    @DisplayName("Revoke All User Tokens Tests")
    class RevokeAllUserTokensTests {

        @Test
        @DisplayName("should revoke all user tokens")
        void shouldRevokeAllUserTokens() {
            authService.revokeAllUserTokens("testuser");

            verify(tokenBlacklistService).revokeAllUserTokens("testuser");
        }
    }
}
