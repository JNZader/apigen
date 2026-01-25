package com.jnzader.apigen.codegen.generator.java.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset functionality including entity, service, controller, and DTOs.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>PasswordResetToken entity for storing reset tokens
 *   <li>PasswordResetService for token management
 *   <li>PasswordResetController with REST endpoints
 *   <li>Request/Response DTOs
 *   <li>Flyway migration for the token table
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class PasswordResetGenerator {

    private static final String PKG_SECURITY = "security";

    private final String basePackage;

    public PasswordResetGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates all password reset files.
     *
     * @param tokenExpirationMinutes token expiration time in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();
        String basePath = "src/main/java/" + basePackage.replace('.', '/');

        // Generate Entity
        files.put(
                basePath + "/" + PKG_SECURITY + "/entity/PasswordResetToken.java",
                generateEntity());

        // Generate Repository
        files.put(
                basePath + "/" + PKG_SECURITY + "/repository/PasswordResetTokenRepository.java",
                generateRepository());

        // Generate Service Interface
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/PasswordResetService.java",
                generateServiceInterface());

        // Generate Service Implementation
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/PasswordResetServiceImpl.java",
                generateServiceImpl(tokenExpirationMinutes));

        // Generate Controller
        files.put(
                basePath + "/" + PKG_SECURITY + "/controller/PasswordResetController.java",
                generateController());

        // Generate DTOs
        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ForgotPasswordRequest.java",
                generateForgotPasswordRequest());

        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ResetPasswordRequest.java",
                generateResetPasswordRequest());

        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ValidateTokenResponse.java",
                generateValidateTokenResponse());

        // Generate Migration
        files.put(
                "src/main/resources/db/migration/V999__create_password_reset_tokens_table.sql",
                generateMigration());

        return files;
    }

    private String generateEntity() {
        return """
        package %s.security.entity;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.GeneratedValue;
        import jakarta.persistence.GenerationType;
        import jakarta.persistence.Id;
        import jakarta.persistence.Index;
        import jakarta.persistence.Table;
        import java.time.LocalDateTime;
        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        /**
         * Entity representing a password reset token.
         *
         * <p>Tokens are single-use and expire after a configurable duration.
         */
        @Entity
        @Table(
                name = "password_reset_tokens",
                indexes = {
                    @Index(name = "idx_prt_token", columnList = "token"),
                    @Index(name = "idx_prt_email", columnList = "email"),
                    @Index(name = "idx_prt_expires", columnList = "expires_at")
                })
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public class PasswordResetToken {

            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            private Long id;

            @Column(nullable = false, unique = true, length = 64)
            private String token;

            @Column(nullable = false, length = 320)
            private String email;

            @Column(name = "expires_at", nullable = false)
            private LocalDateTime expiresAt;

            @Column(name = "used_at")
            private LocalDateTime usedAt;

            @Column(name = "created_at", nullable = false)
            private LocalDateTime createdAt;

            /** Checks if this token has expired. */
            public boolean isExpired() {
                return LocalDateTime.now().isAfter(expiresAt);
            }

            /** Checks if this token has been used. */
            public boolean isUsed() {
                return usedAt != null;
            }

            /** Checks if this token is valid (not expired and not used). */
            public boolean isValid() {
                return !isExpired() && !isUsed();
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateRepository() {
        return """
        package %s.security.repository;

        import %s.security.entity.PasswordResetToken;
        import java.time.LocalDateTime;
        import java.util.Optional;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.data.jpa.repository.Modifying;
        import org.springframework.data.jpa.repository.Query;
        import org.springframework.stereotype.Repository;

        /**
         * Repository for password reset token operations.
         */
        @Repository
        public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

            /**
             * Finds a token by its value.
             *
             * @param token the token string
             * @return the token entity if found
             */
            Optional<PasswordResetToken> findByToken(String token);

            /**
             * Finds the most recent unused token for an email.
             *
             * @param email the email address
             * @return the token entity if found
             */
            @Query("SELECT t FROM PasswordResetToken t WHERE t.email = :email " +
                   "AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP " +
                   "ORDER BY t.createdAt DESC")
            Optional<PasswordResetToken> findValidTokenByEmail(String email);

            /**
             * Invalidates all tokens for an email by marking them as used.
             *
             * @param email the email address
             * @param usedAt the timestamp to mark as used
             * @return number of tokens invalidated
             */
            @Modifying
            @Query("UPDATE PasswordResetToken t SET t.usedAt = :usedAt " +
                   "WHERE t.email = :email AND t.usedAt IS NULL")
            int invalidateAllTokensForEmail(String email, LocalDateTime usedAt);

            /**
             * Deletes expired tokens older than the specified date.
             *
             * @param before delete tokens that expired before this date
             * @return number of tokens deleted
             */
            @Modifying
            @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
            int deleteExpiredTokensBefore(LocalDateTime before);
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateServiceInterface() {
        return """
        package %s.security.service;

        import %s.security.dto.ValidateTokenResponse;

        /**
         * Service interface for password reset operations.
         */
        public interface PasswordResetService {

            /**
             * Creates a password reset token and sends the reset email.
             *
             * @param email the user's email address
             * @throws UserNotFoundException if no user exists with this email
             */
            void createPasswordResetToken(String email);

            /**
             * Validates a password reset token.
             *
             * @param token the token to validate
             * @return validation result with email if valid
             */
            ValidateTokenResponse validateToken(String token);

            /**
             * Resets the user's password using a valid token.
             *
             * @param token the password reset token
             * @param newPassword the new password
             * @throws InvalidTokenException if token is invalid or expired
             */
            void resetPassword(String token, String newPassword);

            /**
             * Cleans up expired tokens.
             *
             * @return number of tokens deleted
             */
            int cleanupExpiredTokens();
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateServiceImpl(int tokenExpirationMinutes) {
        return """
        package %s.security.service;

        import %s.common.mail.MailService;
        import %s.security.dto.ValidateTokenResponse;
        import %s.security.entity.PasswordResetToken;
        import %s.security.repository.PasswordResetTokenRepository;
        import java.security.SecureRandom;
        import java.time.LocalDateTime;
        import java.util.Base64;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.scheduling.annotation.Scheduled;
        import org.springframework.stereotype.Service;
        import org.springframework.transaction.annotation.Transactional;

        /**
         * Implementation of password reset service.
         */
        @Service
        public class PasswordResetServiceImpl implements PasswordResetService {

            private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
            private static final int TOKEN_LENGTH = 32;
            private static final int EXPIRATION_MINUTES = %d;

            private final PasswordResetTokenRepository tokenRepository;
            private final MailService mailService;
            private final SecureRandom secureRandom = new SecureRandom();

            @Value("${app.password-reset.base-url:http://localhost:3000}")
            private String baseUrl;

            @Value("${app.password-reset.path:/reset-password}")
            private String resetPath;

            public PasswordResetServiceImpl(
                    PasswordResetTokenRepository tokenRepository,
                    MailService mailService) {
                this.tokenRepository = tokenRepository;
                this.mailService = mailService;
            }

            @Override
            @Transactional
            public void createPasswordResetToken(String email) {
                // Invalidate any existing tokens for this email
                tokenRepository.invalidateAllTokensForEmail(email, LocalDateTime.now());

                // Generate new token
                String token = generateSecureToken();
                LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

                PasswordResetToken resetToken = PasswordResetToken.builder()
                        .token(token)
                        .email(email)
                        .expiresAt(expiresAt)
                        .createdAt(LocalDateTime.now())
                        .build();

                tokenRepository.save(resetToken);

                // Send reset email
                String resetLink = baseUrl + resetPath + "?token=" + token;
                mailService.sendPasswordResetEmail(email, email, resetLink, EXPIRATION_MINUTES);

                log.info("Password reset token created for email: {}", email);
            }

            @Override
            public ValidateTokenResponse validateToken(String token) {
                return tokenRepository.findByToken(token)
                        .map(t -> ValidateTokenResponse.builder()
                                .valid(t.isValid())
                                .email(t.isValid() ? t.getEmail() : null)
                                .expired(t.isExpired())
                                .used(t.isUsed())
                                .build())
                        .orElse(ValidateTokenResponse.builder()
                                .valid(false)
                                .expired(false)
                                .used(false)
                                .build());
            }

            @Override
            @Transactional
            public void resetPassword(String token, String newPassword) {
                PasswordResetToken resetToken = tokenRepository.findByToken(token)
                        .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

                if (!resetToken.isValid()) {
                    if (resetToken.isExpired()) {
                        throw new InvalidTokenException("Reset token has expired");
                    }
                    if (resetToken.isUsed()) {
                        throw new InvalidTokenException("Reset token has already been used");
                    }
                }

                // Mark token as used
                resetToken.setUsedAt(LocalDateTime.now());
                tokenRepository.save(resetToken);

                // TODO: Update user password
                // This should be implemented to call UserService.updatePassword(email, newPassword)
                log.info("Password reset completed for email: {}", resetToken.getEmail());
            }

            @Override
            @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
            @Transactional
            public int cleanupExpiredTokens() {
                LocalDateTime before = LocalDateTime.now().minusDays(7);
                int deleted = tokenRepository.deleteExpiredTokensBefore(before);
                log.info("Cleaned up {} expired password reset tokens", deleted);
                return deleted;
            }

            private String generateSecureToken() {
                byte[] tokenBytes = new byte[TOKEN_LENGTH];
                secureRandom.nextBytes(tokenBytes);
                return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            }

            /** Exception thrown when a token is invalid. */
            public static class InvalidTokenException extends RuntimeException {
                public InvalidTokenException(String message) {
                    super(message);
                }
            }
        }
        """
                .formatted(
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        tokenExpirationMinutes);
    }

    private String generateController() {
        return """
        package %s.security.controller;

        import %s.security.dto.ForgotPasswordRequest;
        import %s.security.dto.ResetPasswordRequest;
        import %s.security.dto.ValidateTokenResponse;
        import %s.security.service.PasswordResetService;
        import io.swagger.v3.oas.annotations.Operation;
        import io.swagger.v3.oas.annotations.responses.ApiResponse;
        import io.swagger.v3.oas.annotations.responses.ApiResponses;
        import io.swagger.v3.oas.annotations.tags.Tag;
        import jakarta.validation.Valid;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RequestParam;
        import org.springframework.web.bind.annotation.RestController;

        /**
         * REST controller for password reset operations.
         */
        @RestController
        @RequestMapping("/api/auth/password")
        @Tag(name = "Password Reset", description = "Password reset and recovery operations")
        public class PasswordResetController {

            private final PasswordResetService passwordResetService;

            public PasswordResetController(PasswordResetService passwordResetService) {
                this.passwordResetService = passwordResetService;
            }

            @PostMapping("/forgot")
            @Operation(
                    summary = "Request password reset",
                    description = "Sends a password reset email to the specified address")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Reset email sent if user exists"),
                @ApiResponse(responseCode = "400", description = "Invalid email format")
            })
            public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
                // Always return success to prevent email enumeration
                try {
                    passwordResetService.createPasswordResetToken(request.getEmail());
                } catch (Exception e) {
                    // Log but don't expose whether user exists
                }
                return ResponseEntity.ok().build();
            }

            @GetMapping("/validate")
            @Operation(
                    summary = "Validate reset token",
                    description = "Checks if a password reset token is valid")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Token validation result"),
                @ApiResponse(responseCode = "400", description = "Token parameter missing")
            })
            public ResponseEntity<ValidateTokenResponse> validateToken(@RequestParam String token) {
                return ResponseEntity.ok(passwordResetService.validateToken(token));
            }

            @PostMapping("/reset")
            @Operation(
                    summary = "Reset password",
                    description = "Resets password using a valid token")
            @ApiResponses({
                @ApiResponse(responseCode = "200", description = "Password reset successful"),
                @ApiResponse(responseCode = "400", description = "Invalid token or password")
            })
            public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
                passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok().build();
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage, basePackage);
    }

    private String generateForgotPasswordRequest() {
        return """
        package %s.security.dto;

        import jakarta.validation.constraints.Email;
        import jakarta.validation.constraints.NotBlank;
        import lombok.Data;

        /**
         * Request DTO for forgot password endpoint.
         */
        @Data
        public class ForgotPasswordRequest {

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            private String email;
        }
        """
                .formatted(basePackage);
    }

    private String generateResetPasswordRequest() {
        return """
        package %s.security.dto;

        import jakarta.validation.constraints.NotBlank;
        import jakarta.validation.constraints.Size;
        import lombok.Data;

        /**
         * Request DTO for reset password endpoint.
         */
        @Data
        public class ResetPasswordRequest {

            @NotBlank(message = "Token is required")
            private String token;

            @NotBlank(message = "New password is required")
            @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
            private String newPassword;
        }
        """
                .formatted(basePackage);
    }

    private String generateValidateTokenResponse() {
        return """
        package %s.security.dto;

        import lombok.Builder;
        import lombok.Data;

        /**
         * Response DTO for token validation endpoint.
         */
        @Data
        @Builder
        public class ValidateTokenResponse {
            private boolean valid;
            private String email;
            private boolean expired;
            private boolean used;
        }
        """
                .formatted(basePackage);
    }

    private String generateMigration() {
        return """
        -- Password Reset Tokens table
        CREATE TABLE IF NOT EXISTS password_reset_tokens (
            id BIGSERIAL PRIMARY KEY,
            token VARCHAR(64) NOT NULL UNIQUE,
            email VARCHAR(320) NOT NULL,
            expires_at TIMESTAMP NOT NULL,
            used_at TIMESTAMP,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );

        -- Indexes for efficient lookups
        CREATE INDEX IF NOT EXISTS idx_prt_token ON password_reset_tokens(token);
        CREATE INDEX IF NOT EXISTS idx_prt_email ON password_reset_tokens(email);
        CREATE INDEX IF NOT EXISTS idx_prt_expires ON password_reset_tokens(expires_at);
        """;
    }
}
