/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.csharp.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for C#/ASP.NET Core applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class CSharpPasswordResetGenerator {

    private final String namespace;

    public CSharpPasswordResetGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("Models/PasswordResetToken.cs", generateModel());
        files.put("DTOs/PasswordResetDtos.cs", generateDtos());
        files.put("Services/IPasswordResetService.cs", generateInterface(tokenExpirationMinutes));
        files.put("Services/PasswordResetService.cs", generateService(tokenExpirationMinutes));
        files.put("Controllers/PasswordResetController.cs", generateController());

        return files;
    }

    private String generateModel() {
        return String.format(
                """
                using System.ComponentModel.DataAnnotations;

                namespace %s.Models;

                /// <summary>
                /// Password reset token entity.
                /// </summary>
                public class PasswordResetToken
                {
                    [Key]
                    public Guid Id { get; set; }

                    [Required]
                    public Guid UserId { get; set; }

                    [Required]
                    [MaxLength(128)]
                    public string Token { get; set; } = string.Empty;

                    public DateTime ExpiresAt { get; set; }

                    public bool Used { get; set; }

                    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

                    // Navigation
                    public virtual User? User { get; set; }
                }
                """,
                namespace);
    }

    private String generateDtos() {
        return String.format(
                """
                using System.ComponentModel.DataAnnotations;

                namespace %s.DTOs;

                /// <summary>
                /// Forgot password request.
                /// </summary>
                public record ForgotPasswordRequest
                {
                    [Required]
                    [EmailAddress]
                    public string Email { get; init; } = string.Empty;
                }

                /// <summary>
                /// Forgot password response.
                /// </summary>
                public record ForgotPasswordResponse
                {
                    public string Message { get; init; } = string.Empty;
                }

                /// <summary>
                /// Validate token request.
                /// </summary>
                public record ValidateTokenRequest
                {
                    [Required]
                    public string Token { get; init; } = string.Empty;
                }

                /// <summary>
                /// Validate token response.
                /// </summary>
                public record ValidateTokenResponse
                {
                    public bool Valid { get; init; }
                    public string? Message { get; init; }
                }

                /// <summary>
                /// Reset password request.
                /// </summary>
                public record ResetPasswordRequest
                {
                    [Required]
                    public string Token { get; init; } = string.Empty;

                    [Required]
                    [MinLength(8)]
                    [MaxLength(128)]
                    public string NewPassword { get; init; } = string.Empty;

                    [Required]
                    [Compare(nameof(NewPassword))]
                    public string ConfirmPassword { get; init; } = string.Empty;
                }

                /// <summary>
                /// Reset password response.
                /// </summary>
                public record ResetPasswordResponse
                {
                    public bool Success { get; init; }
                    public string Message { get; init; } = string.Empty;
                }
                """,
                namespace);
    }

    private String generateInterface(int tokenExpirationMinutes) {
        return String.format(
                """
                namespace %s.Services;

                /// <summary>
                /// Password reset service interface.
                /// </summary>
                public interface IPasswordResetService
                {
                    /// <summary>
                    /// Token expiration in minutes.
                    /// </summary>
                    const int TokenExpirationMinutes = %d;

                    /// <summary>
                    /// Request a password reset.
                    /// </summary>
                    Task RequestPasswordResetAsync(string email, string baseUrl);

                    /// <summary>
                    /// Validate a reset token.
                    /// </summary>
                    Task<(bool Valid, string? Message)> ValidateTokenAsync(string token);

                    /// <summary>
                    /// Reset password with token.
                    /// </summary>
                    Task<(bool Success, string Message)> ResetPasswordAsync(string token, string newPassword);
                }
                """,
                namespace, tokenExpirationMinutes);
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                using System.Security.Cryptography;
                using Microsoft.EntityFrameworkCore;
                using %1$s.Data;
                using %1$s.Models;
                using %1$s.Services.Mail;

                namespace %1$s.Services;

                /// <summary>
                /// Password reset service implementation.
                /// </summary>
                public class PasswordResetService : IPasswordResetService
                {
                    private readonly ApplicationDbContext _context;
                    private readonly IEmailService _emailService;
                    private readonly IPasswordHasher<User> _passwordHasher;
                    private readonly ILogger<PasswordResetService> _logger;

                    public PasswordResetService(
                        ApplicationDbContext context,
                        IEmailService emailService,
                        IPasswordHasher<User> passwordHasher,
                        ILogger<PasswordResetService> logger)
                    {
                        _context = context;
                        _emailService = emailService;
                        _passwordHasher = passwordHasher;
                        _logger = logger;
                    }

                    /// <inheritdoc />
                    public async Task RequestPasswordResetAsync(string email, string baseUrl)
                    {
                        var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == email);
                        if (user == null)
                        {
                            // Don't reveal if email exists
                            return;
                        }

                        // Invalidate existing tokens
                        var existingTokens = await _context.PasswordResetTokens
                            .Where(t => t.UserId == user.Id && !t.Used)
                            .ToListAsync();

                        _context.PasswordResetTokens.RemoveRange(existingTokens);

                        // Generate new token
                        var token = GenerateSecureToken();
                        var resetToken = new PasswordResetToken
                        {
                            Id = Guid.NewGuid(),
                            UserId = user.Id,
                            Token = token,
                            ExpiresAt = DateTime.UtcNow.AddMinutes(IPasswordResetService.TokenExpirationMinutes),
                            Used = false
                        };

                        _context.PasswordResetTokens.Add(resetToken);
                        await _context.SaveChangesAsync();

                        // Send email
                        var resetLink = $"{baseUrl}/reset-password?token={token}";
                        var userName = user.Username ?? user.Email;

                        try
                        {
                            await _emailService.SendPasswordResetEmailAsync(
                                user.Email,
                                userName,
                                resetLink,
                                IPasswordResetService.TokenExpirationMinutes);

                            _logger.LogInformation("Password reset email sent to: {Email}", email);
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError(ex, "Failed to send password reset email to: {Email}", email);
                        }
                    }

                    /// <inheritdoc />
                    public async Task<(bool Valid, string? Message)> ValidateTokenAsync(string token)
                    {
                        var resetToken = await _context.PasswordResetTokens
                            .FirstOrDefaultAsync(t => t.Token == token && !t.Used);

                        if (resetToken == null)
                        {
                            return (false, "Invalid or expired token");
                        }

                        if (resetToken.ExpiresAt < DateTime.UtcNow)
                        {
                            return (false, "Token has expired");
                        }

                        return (true, null);
                    }

                    /// <inheritdoc />
                    public async Task<(bool Success, string Message)> ResetPasswordAsync(string token, string newPassword)
                    {
                        var (valid, message) = await ValidateTokenAsync(token);
                        if (!valid)
                        {
                            return (false, message ?? "Invalid token");
                        }

                        var resetToken = await _context.PasswordResetTokens
                            .Include(t => t.User)
                            .FirstOrDefaultAsync(t => t.Token == token);

                        if (resetToken?.User == null)
                        {
                            return (false, "User not found");
                        }

                        // Hash new password
                        resetToken.User.Password = _passwordHasher.HashPassword(resetToken.User, newPassword);

                        // Mark token as used
                        resetToken.Used = true;

                        await _context.SaveChangesAsync();

                        _logger.LogInformation("Password reset for user: {UserId}", resetToken.UserId);

                        return (true, "Password has been reset successfully");
                    }

                    private static string GenerateSecureToken()
                    {
                        var bytes = new byte[64];
                        using var rng = RandomNumberGenerator.Create();
                        rng.GetBytes(bytes);
                        return Convert.ToBase64String(bytes)
                            .Replace("+", "-")
                            .Replace("/", "_")
                            .TrimEnd('=');
                    }
                }
                """,
                namespace, tokenExpirationMinutes);
    }

    private String generateController() {
        return String.format(
                """
                using Microsoft.AspNetCore.Mvc;
                using %1$s.DTOs;
                using %1$s.Services;

                namespace %1$s.Controllers;

                /// <summary>
                /// Password reset endpoints.
                /// </summary>
                [ApiController]
                [Route("api/auth/password")]
                public class PasswordResetController : ControllerBase
                {
                    private readonly IPasswordResetService _passwordResetService;
                    private readonly ILogger<PasswordResetController> _logger;

                    public PasswordResetController(
                        IPasswordResetService passwordResetService,
                        ILogger<PasswordResetController> logger)
                    {
                        _passwordResetService = passwordResetService;
                        _logger = logger;
                    }

                    /// <summary>
                    /// Request password reset email.
                    /// </summary>
                    [HttpPost("forgot")]
                    [ProducesResponseType(typeof(ForgotPasswordResponse), StatusCodes.Status200OK)]
                    public async Task<ActionResult<ForgotPasswordResponse>> ForgotPassword(
                        [FromBody] ForgotPasswordRequest request)
                    {
                        var baseUrl = $"{Request.Scheme}://{Request.Host}";
                        await _passwordResetService.RequestPasswordResetAsync(request.Email, baseUrl);

                        // Always return success to prevent email enumeration
                        return Ok(new ForgotPasswordResponse
                        {
                            Message = "If the email exists, a password reset link has been sent."
                        });
                    }

                    /// <summary>
                    /// Validate reset token.
                    /// </summary>
                    [HttpPost("validate")]
                    [ProducesResponseType(typeof(ValidateTokenResponse), StatusCodes.Status200OK)]
                    public async Task<ActionResult<ValidateTokenResponse>> ValidateToken(
                        [FromBody] ValidateTokenRequest request)
                    {
                        var (valid, message) = await _passwordResetService.ValidateTokenAsync(request.Token);

                        return Ok(new ValidateTokenResponse
                        {
                            Valid = valid,
                            Message = message
                        });
                    }

                    /// <summary>
                    /// Reset password with token.
                    /// </summary>
                    [HttpPost("reset")]
                    [ProducesResponseType(typeof(ResetPasswordResponse), StatusCodes.Status200OK)]
                    [ProducesResponseType(typeof(ResetPasswordResponse), StatusCodes.Status400BadRequest)]
                    public async Task<ActionResult<ResetPasswordResponse>> ResetPassword(
                        [FromBody] ResetPasswordRequest request)
                    {
                        var (success, message) = await _passwordResetService.ResetPasswordAsync(
                            request.Token,
                            request.NewPassword);

                        var response = new ResetPasswordResponse
                        {
                            Success = success,
                            Message = message
                        };

                        return success ? Ok(response) : BadRequest(response);
                    }
                }
                """,
                namespace);
    }
}
