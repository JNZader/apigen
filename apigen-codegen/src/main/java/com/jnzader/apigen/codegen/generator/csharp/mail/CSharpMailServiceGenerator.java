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
package com.jnzader.apigen.codegen.generator.csharp.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for C#/ASP.NET Core applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class CSharpMailServiceGenerator {

    private final String namespace;

    public CSharpMailServiceGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Generates all mail service files.
     *
     * @param generateWelcomeTemplate whether to generate welcome template
     * @param generatePasswordResetTemplate whether to generate password reset template
     * @param generateNotificationTemplate whether to generate notification template
     * @return map of file path to content
     */
    public Map<String, String> generate(
            boolean generateWelcomeTemplate,
            boolean generatePasswordResetTemplate,
            boolean generateNotificationTemplate) {

        Map<String, String> files = new LinkedHashMap<>();

        files.put("Services/Mail/IEmailService.cs", generateInterface());
        files.put("Services/Mail/EmailService.cs", generateService());
        files.put("Services/Mail/EmailSettings.cs", generateSettings());

        if (generateWelcomeTemplate) {
            files.put("Templates/Email/Welcome.cshtml", generateWelcomeTemplate());
        }
        if (generatePasswordResetTemplate) {
            files.put("Templates/Email/PasswordReset.cshtml", generatePasswordResetTemplate());
        }
        if (generateNotificationTemplate) {
            files.put("Templates/Email/Notification.cshtml", generateNotificationTemplate());
        }

        return files;
    }

    private String generateInterface() {
        return String.format(
                """
                namespace %s.Services.Mail;

                /// <summary>
                /// Email service interface.
                /// </summary>
                public interface IEmailService
                {
                    /// <summary>
                    /// Send a simple text email.
                    /// </summary>
                    Task SendEmailAsync(string to, string subject, string body);

                    /// <summary>
                    /// Send an HTML email using a template.
                    /// </summary>
                    Task SendTemplateEmailAsync<T>(string to, string subject, string templateName, T model);

                    /// <summary>
                    /// Send a welcome email.
                    /// </summary>
                    Task SendWelcomeEmailAsync(string to, string userName);

                    /// <summary>
                    /// Send a password reset email.
                    /// </summary>
                    Task SendPasswordResetEmailAsync(string to, string userName, string resetLink, int expirationMinutes);

                    /// <summary>
                    /// Send a notification email.
                    /// </summary>
                    Task SendNotificationEmailAsync(string to, string title, string message);
                }
                """,
                namespace);
    }

    private String generateService() {
        return String.format(
                """
                using System.Net;
                using System.Net.Mail;
                using Microsoft.Extensions.Options;
                using RazorLight;

                namespace %s.Services.Mail;

                /// <summary>
                /// Email service implementation using SMTP.
                /// </summary>
                public class EmailService : IEmailService
                {
                    private readonly EmailSettings _settings;
                    private readonly RazorLightEngine _razorEngine;
                    private readonly ILogger<EmailService> _logger;

                    public EmailService(
                        IOptions<EmailSettings> settings,
                        ILogger<EmailService> logger)
                    {
                        _settings = settings.Value;
                        _logger = logger;
                        _razorEngine = new RazorLightEngineBuilder()
                            .UseFileSystemProject(Path.Combine(AppContext.BaseDirectory, "Templates", "Email"))
                            .UseMemoryCachingProvider()
                            .Build();
                    }

                    /// <inheritdoc />
                    public async Task SendEmailAsync(string to, string subject, string body)
                    {
                        using var client = CreateSmtpClient();
                        var message = CreateMailMessage(to, subject, body, isHtml: false);

                        try
                        {
                            await client.SendMailAsync(message);
                            _logger.LogInformation("Email sent to {To}: {Subject}", to, subject);
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError(ex, "Failed to send email to {To}", to);
                            throw;
                        }
                    }

                    /// <inheritdoc />
                    public async Task SendTemplateEmailAsync<T>(string to, string subject, string templateName, T model)
                    {
                        var body = await _razorEngine.CompileRenderAsync(templateName, model);
                        using var client = CreateSmtpClient();
                        var message = CreateMailMessage(to, subject, body, isHtml: true);

                        try
                        {
                            await client.SendMailAsync(message);
                            _logger.LogInformation("Template email sent to {To}: {Subject}", to, subject);
                        }
                        catch (Exception ex)
                        {
                            _logger.LogError(ex, "Failed to send template email to {To}", to);
                            throw;
                        }
                    }

                    /// <inheritdoc />
                    public async Task SendWelcomeEmailAsync(string to, string userName)
                    {
                        var model = new { AppName = _settings.FromName, UserName = userName };
                        await SendTemplateEmailAsync(to, $"Welcome to {_settings.FromName}!", "Welcome.cshtml", model);
                    }

                    /// <inheritdoc />
                    public async Task SendPasswordResetEmailAsync(string to, string userName, string resetLink, int expirationMinutes)
                    {
                        var model = new
                        {
                            AppName = _settings.FromName,
                            UserName = userName,
                            ResetLink = resetLink,
                            ExpirationMinutes = expirationMinutes
                        };
                        await SendTemplateEmailAsync(to, "Password Reset Request", "PasswordReset.cshtml", model);
                    }

                    /// <inheritdoc />
                    public async Task SendNotificationEmailAsync(string to, string title, string message)
                    {
                        var model = new { AppName = _settings.FromName, Title = title, Message = message };
                        await SendTemplateEmailAsync(to, title, "Notification.cshtml", model);
                    }

                    private SmtpClient CreateSmtpClient()
                    {
                        var client = new SmtpClient(_settings.Host, _settings.Port)
                        {
                            Credentials = new NetworkCredential(_settings.Username, _settings.Password),
                            EnableSsl = _settings.UseTls
                        };
                        return client;
                    }

                    private MailMessage CreateMailMessage(string to, string subject, string body, bool isHtml)
                    {
                        return new MailMessage
                        {
                            From = new MailAddress(_settings.FromAddress, _settings.FromName),
                            To = { to },
                            Subject = subject,
                            Body = body,
                            IsBodyHtml = isHtml
                        };
                    }
                }
                """,
                namespace);
    }

    private String generateSettings() {
        return String.format(
                """
                namespace %s.Services.Mail;

                /// <summary>
                /// Email configuration settings.
                /// </summary>
                public class EmailSettings
                {
                    /// <summary>
                    /// SMTP host.
                    /// </summary>
                    public string Host { get; set; } = "localhost";

                    /// <summary>
                    /// SMTP port.
                    /// </summary>
                    public int Port { get; set; } = 25;

                    /// <summary>
                    /// SMTP username.
                    /// </summary>
                    public string Username { get; set; } = string.Empty;

                    /// <summary>
                    /// SMTP password.
                    /// </summary>
                    public string Password { get; set; } = string.Empty;

                    /// <summary>
                    /// From email address.
                    /// </summary>
                    public string FromAddress { get; set; } = "noreply@example.com";

                    /// <summary>
                    /// From display name.
                    /// </summary>
                    public string FromName { get; set; } = "Application";

                    /// <summary>
                    /// Use TLS/SSL.
                    /// </summary>
                    public bool UseTls { get; set; } = true;
                }
                """,
                namespace);
    }

    private String generateWelcomeTemplate() {
        return """
        @model dynamic
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                .container { background: #fff; border-radius: 8px; padding: 40px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .header { text-align: center; margin-bottom: 30px; }
                .header h1 { color: #2563eb; }
                .button { display: inline-block; padding: 12px 24px; background: #2563eb; color: #fff; text-decoration: none; border-radius: 6px; }
                .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>@Model.AppName</h1></div>
                <h2>Welcome, @Model.UserName!</h2>
                <p>Thank you for joining @Model.AppName. We're excited to have you on board!</p>
                <p>Your account has been successfully created and is ready to use.</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="/login" class="button">Get Started</a>
                </p>
                <p>Best regards,<br/>The @Model.AppName Team</p>
                <div class="footer"><p>&copy; @Model.AppName. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }

    private String generatePasswordResetTemplate() {
        return """
        @model dynamic
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                .container { background: #fff; border-radius: 8px; padding: 40px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .header { text-align: center; margin-bottom: 30px; }
                .header h1 { color: #2563eb; }
                .button { display: inline-block; padding: 12px 24px; background: #dc2626; color: #fff; text-decoration: none; border-radius: 6px; }
                .warning { background: #fef3c7; border: 1px solid #f59e0b; border-radius: 6px; padding: 15px; margin: 20px 0; }
                .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>@Model.AppName</h1></div>
                <h2>Password Reset Request</h2>
                <p>Hello @Model.UserName,</p>
                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="@Model.ResetLink" class="button">Reset Password</a>
                </p>
                <div class="warning">
                    <strong>Important:</strong> This link will expire in @Model.ExpirationMinutes minutes.
                    If you didn't request this, you can safely ignore this email.
                </div>
                <p>Best regards,<br/>The @Model.AppName Team</p>
                <div class="footer"><p>&copy; @Model.AppName. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateNotificationTemplate() {
        return """
        @model dynamic
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                .container { background: #fff; border-radius: 8px; padding: 40px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .header { text-align: center; margin-bottom: 30px; }
                .header h1 { color: #2563eb; }
                .notification { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; padding: 20px; margin: 20px 0; }
                .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>@Model.AppName</h1></div>
                <h2>@Model.Title</h2>
                <div class="notification"><p>@Model.Message</p></div>
                <p>Best regards,<br/>The @Model.AppName Team</p>
                <div class="footer"><p>&copy; @Model.AppName. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }
}
