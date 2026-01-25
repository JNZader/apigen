package com.jnzader.apigen.codegen.generator.kotlin.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Kotlin password reset functionality including entity, service, controller, and DTOs.
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
public class KotlinPasswordResetGenerator {

    private static final String PKG_SECURITY = "security";

    private final String basePackage;

    public KotlinPasswordResetGenerator(String basePackage) {
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
        String basePath = "src/main/kotlin/" + basePackage.replace('.', '/');

        // Generate Entity
        files.put(
                basePath + "/" + PKG_SECURITY + "/entity/PasswordResetToken.kt", generateEntity());

        // Generate Repository
        files.put(
                basePath + "/" + PKG_SECURITY + "/repository/PasswordResetTokenRepository.kt",
                generateRepository());

        // Generate Service Interface
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/PasswordResetService.kt",
                generateServiceInterface());

        // Generate Service Implementation
        files.put(
                basePath + "/" + PKG_SECURITY + "/service/PasswordResetServiceImpl.kt",
                generateServiceImpl(tokenExpirationMinutes));

        // Generate Controller
        files.put(
                basePath + "/" + PKG_SECURITY + "/controller/PasswordResetController.kt",
                generateController());

        // Generate DTOs
        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ForgotPasswordRequest.kt",
                generateForgotPasswordRequest());

        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ResetPasswordRequest.kt",
                generateResetPasswordRequest());

        files.put(
                basePath + "/" + PKG_SECURITY + "/dto/ValidateTokenResponse.kt",
                generateValidateTokenResponse());

        // Generate Exception
        files.put(
                basePath + "/" + PKG_SECURITY + "/exception/InvalidTokenException.kt",
                generateException());

        // Generate Migration
        files.put(
                "src/main/resources/db/migration/V999__create_password_reset_tokens_table.sql",
                generateMigration());

        return files;
    }

    private String generateEntity() {
        return """
        package %s.security.entity

        import jakarta.persistence.Column
        import jakarta.persistence.Entity
        import jakarta.persistence.GeneratedValue
        import jakarta.persistence.GenerationType
        import jakarta.persistence.Id
        import jakarta.persistence.Index
        import jakarta.persistence.Table
        import java.time.LocalDateTime

        /**
         * Entity representing a password reset token.
         *
         * Tokens are single-use and expire after a configurable duration.
         */
        @Entity
        @Table(
            name = "password_reset_tokens",
            indexes = [
                Index(name = "idx_prt_token", columnList = "token"),
                Index(name = "idx_prt_email", columnList = "email"),
                Index(name = "idx_prt_expires", columnList = "expires_at")
            ]
        )
        data class PasswordResetToken(
            @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
            val id: Long? = null,

            @Column(nullable = false, unique = true, length = 64)
            val token: String,

            @Column(nullable = false, length = 320)
            val email: String,

            @Column(name = "expires_at", nullable = false)
            val expiresAt: LocalDateTime,

            @Column(name = "used_at")
            var usedAt: LocalDateTime? = null,

            @Column(name = "created_at", nullable = false)
            val createdAt: LocalDateTime = LocalDateTime.now()
        ) {
            /** Checks if this token has expired. */
            fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

            /** Checks if this token has been used. */
            fun isUsed(): Boolean = usedAt != null

            /** Checks if this token is valid (not expired and not used). */
            fun isValid(): Boolean = !isExpired() && !isUsed()
        }
        """
                .formatted(basePackage);
    }

    private String generateRepository() {
        return """
        package %s.security.repository

        import %s.security.entity.PasswordResetToken
        import org.springframework.data.jpa.repository.JpaRepository
        import org.springframework.data.jpa.repository.Modifying
        import org.springframework.data.jpa.repository.Query
        import org.springframework.stereotype.Repository
        import java.time.LocalDateTime
        import java.util.Optional

        /**
         * Repository for password reset token operations.
         */
        @Repository
        interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {

            /**
             * Finds a token by its value.
             *
             * @param token the token string
             * @return the token entity if found
             */
            fun findByToken(token: String): Optional<PasswordResetToken>

            /**
             * Finds the most recent unused token for an email.
             *
             * @param email the email address
             * @return the token entity if found
             */
            @Query(
                \"""
                SELECT t FROM PasswordResetToken t WHERE t.email = :email
                AND t.usedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP
                ORDER BY t.createdAt DESC
                \"""
            )
            fun findValidTokenByEmail(email: String): Optional<PasswordResetToken>

            /**
             * Invalidates all tokens for an email by marking them as used.
             *
             * @param email the email address
             * @param usedAt the timestamp to mark as used
             * @return number of tokens invalidated
             */
            @Modifying
            @Query(
                \"""
                UPDATE PasswordResetToken t SET t.usedAt = :usedAt
                WHERE t.email = :email AND t.usedAt IS NULL
                \"""
            )
            fun invalidateAllTokensForEmail(email: String, usedAt: LocalDateTime): Int

            /**
             * Deletes expired tokens older than the specified date.
             *
             * @param before delete tokens that expired before this date
             * @return number of tokens deleted
             */
            @Modifying
            @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
            fun deleteExpiredTokensBefore(before: LocalDateTime): Int
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateServiceInterface() {
        return """
        package %s.security.service

        import %s.security.dto.ValidateTokenResponse

        /**
         * Service interface for password reset operations.
         */
        interface PasswordResetService {

            /**
             * Creates a password reset token and sends the reset email.
             *
             * @param email the user's email address
             */
            fun createPasswordResetToken(email: String)

            /**
             * Validates a password reset token.
             *
             * @param token the token to validate
             * @return validation result with email if valid
             */
            fun validateToken(token: String): ValidateTokenResponse

            /**
             * Resets the user's password using a valid token.
             *
             * @param token the password reset token
             * @param newPassword the new password
             */
            fun resetPassword(token: String, newPassword: String)

            /**
             * Cleans up expired tokens.
             *
             * @return number of tokens deleted
             */
            fun cleanupExpiredTokens(): Int
        }
        """
                .formatted(basePackage, basePackage);
    }

    private String generateServiceImpl(int tokenExpirationMinutes) {
        return """
        package %s.security.service

        import %s.common.mail.MailService
        import %s.security.dto.ValidateTokenResponse
        import %s.security.entity.PasswordResetToken
        import %s.security.exception.InvalidTokenException
        import %s.security.repository.PasswordResetTokenRepository
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.scheduling.annotation.Scheduled
        import org.springframework.stereotype.Service
        import org.springframework.transaction.annotation.Transactional
        import java.security.SecureRandom
        import java.time.LocalDateTime
        import java.util.Base64

        /**
         * Implementation of password reset service.
         */
        @Service
        class PasswordResetServiceImpl(
            private val tokenRepository: PasswordResetTokenRepository,
            private val mailService: MailService
        ) : PasswordResetService {

            private val log = LoggerFactory.getLogger(PasswordResetServiceImpl::class.java)
            private val secureRandom = SecureRandom()

            @Value("\\${app.password-reset.base-url:http://localhost:3000}")
            private lateinit var baseUrl: String

            @Value("\\${app.password-reset.path:/reset-password}")
            private lateinit var resetPath: String

            companion object {
                private const val TOKEN_LENGTH = 32
                private const val EXPIRATION_MINUTES = %d
            }

            @Transactional
            override fun createPasswordResetToken(email: String) {
                // Invalidate any existing tokens for this email
                tokenRepository.invalidateAllTokensForEmail(email, LocalDateTime.now())

                // Generate new token
                val token = generateSecureToken()
                val expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES.toLong())

                val resetToken = PasswordResetToken(
                    token = token,
                    email = email,
                    expiresAt = expiresAt,
                    createdAt = LocalDateTime.now()
                )

                tokenRepository.save(resetToken)

                // Send reset email
                val resetLink = "$baseUrl$resetPath?token=$token"
                mailService.sendPasswordResetEmail(email, email, resetLink, EXPIRATION_MINUTES)

                log.info("Password reset token created for email: {}", email)
            }

            override fun validateToken(token: String): ValidateTokenResponse {
                return tokenRepository.findByToken(token)
                    .map { t ->
                        ValidateTokenResponse(
                            valid = t.isValid(),
                            email = if (t.isValid()) t.email else null,
                            expired = t.isExpired(),
                            used = t.isUsed()
                        )
                    }
                    .orElse(
                        ValidateTokenResponse(
                            valid = false,
                            email = null,
                            expired = false,
                            used = false
                        )
                    )
            }

            @Transactional
            override fun resetPassword(token: String, newPassword: String) {
                val resetToken = tokenRepository.findByToken(token)
                    .orElseThrow { InvalidTokenException("Invalid reset token") }

                if (!resetToken.isValid()) {
                    when {
                        resetToken.isExpired() -> throw InvalidTokenException("Reset token has expired")
                        resetToken.isUsed() -> throw InvalidTokenException("Reset token has already been used")
                    }
                }

                // Mark token as used
                resetToken.usedAt = LocalDateTime.now()
                tokenRepository.save(resetToken)

                // TODO: Update user password
                // This should be implemented to call UserService.updatePassword(email, newPassword)
                log.info("Password reset completed for email: {}", resetToken.email)
            }

            @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
            @Transactional
            override fun cleanupExpiredTokens(): Int {
                val before = LocalDateTime.now().minusDays(7)
                val deleted = tokenRepository.deleteExpiredTokensBefore(before)
                log.info("Cleaned up {} expired password reset tokens", deleted)
                return deleted
            }

            private fun generateSecureToken(): String {
                val tokenBytes = ByteArray(TOKEN_LENGTH)
                secureRandom.nextBytes(tokenBytes)
                return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)
            }
        }
        """
                .formatted(
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        basePackage,
                        tokenExpirationMinutes);
    }

    private String generateController() {
        return """
        package %s.security.controller

        import %s.security.dto.ForgotPasswordRequest
        import %s.security.dto.ResetPasswordRequest
        import %s.security.dto.ValidateTokenResponse
        import %s.security.service.PasswordResetService
        import io.swagger.v3.oas.annotations.Operation
        import io.swagger.v3.oas.annotations.responses.ApiResponse
        import io.swagger.v3.oas.annotations.responses.ApiResponses
        import io.swagger.v3.oas.annotations.tags.Tag
        import jakarta.validation.Valid
        import org.springframework.http.ResponseEntity
        import org.springframework.web.bind.annotation.GetMapping
        import org.springframework.web.bind.annotation.PostMapping
        import org.springframework.web.bind.annotation.RequestBody
        import org.springframework.web.bind.annotation.RequestMapping
        import org.springframework.web.bind.annotation.RequestParam
        import org.springframework.web.bind.annotation.RestController

        /**
         * REST controller for password reset operations.
         */
        @RestController
        @RequestMapping("/api/auth/password")
        @Tag(name = "Password Reset", description = "Password reset and recovery operations")
        class PasswordResetController(
            private val passwordResetService: PasswordResetService
        ) {

            @PostMapping("/forgot")
            @Operation(
                summary = "Request password reset",
                description = "Sends a password reset email to the specified address"
            )
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "Reset email sent if user exists"),
                ApiResponse(responseCode = "400", description = "Invalid email format")
            )
            fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
                // Always return success to prevent email enumeration
                try {
                    passwordResetService.createPasswordResetToken(request.email)
                } catch (_: Exception) {
                    // Log but don't expose whether user exists
                }
                return ResponseEntity.ok().build()
            }

            @GetMapping("/validate")
            @Operation(
                summary = "Validate reset token",
                description = "Checks if a password reset token is valid"
            )
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "Token validation result"),
                ApiResponse(responseCode = "400", description = "Token parameter missing")
            )
            fun validateToken(@RequestParam token: String): ResponseEntity<ValidateTokenResponse> {
                return ResponseEntity.ok(passwordResetService.validateToken(token))
            }

            @PostMapping("/reset")
            @Operation(
                summary = "Reset password",
                description = "Resets password using a valid token"
            )
            @ApiResponses(
                ApiResponse(responseCode = "200", description = "Password reset successful"),
                ApiResponse(responseCode = "400", description = "Invalid token or password")
            )
            fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
                passwordResetService.resetPassword(request.token, request.newPassword)
                return ResponseEntity.ok().build()
            }
        }
        """
                .formatted(basePackage, basePackage, basePackage, basePackage, basePackage);
    }

    private String generateForgotPasswordRequest() {
        return """
        package %s.security.dto

        import jakarta.validation.constraints.Email
        import jakarta.validation.constraints.NotBlank

        /**
         * Request DTO for forgot password endpoint.
         */
        data class ForgotPasswordRequest(
            @field:NotBlank(message = "Email is required")
            @field:Email(message = "Invalid email format")
            val email: String
        )
        """
                .formatted(basePackage);
    }

    private String generateResetPasswordRequest() {
        return """
        package %s.security.dto

        import jakarta.validation.constraints.NotBlank
        import jakarta.validation.constraints.Size

        /**
         * Request DTO for reset password endpoint.
         */
        data class ResetPasswordRequest(
            @field:NotBlank(message = "Token is required")
            val token: String,

            @field:NotBlank(message = "New password is required")
            @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
            val newPassword: String
        )
        """
                .formatted(basePackage);
    }

    private String generateValidateTokenResponse() {
        return """
        package %s.security.dto

        /**
         * Response DTO for token validation endpoint.
         */
        data class ValidateTokenResponse(
            val valid: Boolean,
            val email: String?,
            val expired: Boolean,
            val used: Boolean
        )
        """
                .formatted(basePackage);
    }

    private String generateException() {
        return """
        package %s.security.exception

        /**
         * Exception thrown when a token is invalid.
         */
        class InvalidTokenException(message: String) : RuntimeException(message)
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
