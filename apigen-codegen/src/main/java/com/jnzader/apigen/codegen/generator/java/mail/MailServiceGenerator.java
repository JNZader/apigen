package com.jnzader.apigen.codegen.generator.java.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code including interface, implementation, and email templates.
 *
 * <p>This generator creates a complete email service with:
 *
 * <ul>
 *   <li>MailService interface with common email operations
 *   <li>MailServiceImpl using Spring Boot Mail
 *   <li>Thymeleaf email templates (welcome, password reset, notification)
 *   <li>Mail configuration properties
 * </ul>
 */
public class MailServiceGenerator {

    private static final String PKG_COMMON_MAIL = "common/mail";

    private final String basePackage;

    public MailServiceGenerator(String basePackage) {
        this.basePackage = basePackage;
    }

    /**
     * Generates all mail service files.
     *
     * @param generateWelcomeTemplate whether to generate welcome email template
     * @param generatePasswordResetTemplate whether to generate password reset email template
     * @param generateNotificationTemplate whether to generate notification email template
     * @return map of file path to content
     */
    public Map<String, String> generate(
            boolean generateWelcomeTemplate,
            boolean generatePasswordResetTemplate,
            boolean generateNotificationTemplate) {

        Map<String, String> files = new LinkedHashMap<>();
        String basePath = "src/main/java/" + basePackage.replace('.', '/');

        // Generate interface
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/MailService.java", generateInterface());

        // Generate implementation
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/MailServiceImpl.java", generateImpl());

        // Generate EmailMessage DTO
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/EmailMessage.java", generateEmailMessage());

        // Generate templates
        if (generateWelcomeTemplate) {
            files.put("src/main/resources/templates/email/welcome.html", generateWelcomeTemplate());
        }

        if (generatePasswordResetTemplate) {
            files.put(
                    "src/main/resources/templates/email/password-reset.html",
                    generatePasswordResetTemplate());
        }

        if (generateNotificationTemplate) {
            files.put(
                    "src/main/resources/templates/email/notification.html",
                    generateNotificationTemplate());
        }

        // Generate base layout
        files.put("src/main/resources/templates/email/layout.html", generateLayoutTemplate());

        return files;
    }

    private String generateInterface() {
        return """
        package %s.common.mail;

        import java.util.Map;
        import java.util.concurrent.CompletableFuture;

        /**
         * Service interface for sending emails.
         *
         * <p>Provides both synchronous and asynchronous email sending capabilities
         * with support for plain text and HTML templates.
         */
        public interface MailService {

            /**
             * Sends a simple text email.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param text email body text
             */
            void sendSimpleEmail(String to, String subject, String text);

            /**
             * Sends an HTML email using a template.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param templateName the Thymeleaf template name (without .html extension)
             * @param variables template variables
             */
            void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);

            /**
             * Sends an HTML email asynchronously.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param templateName the Thymeleaf template name
             * @param variables template variables
             * @return CompletableFuture that completes when email is sent
             */
            CompletableFuture<Void> sendHtmlEmailAsync(
                    String to, String subject, String templateName, Map<String, Object> variables);

            /**
             * Sends an email using EmailMessage object.
             *
             * @param message the email message
             */
            void send(EmailMessage message);

            /**
             * Sends a welcome email to a new user.
             *
             * @param to recipient email address
             * @param userName user's name
             */
            void sendWelcomeEmail(String to, String userName);

            /**
             * Sends a password reset email.
             *
             * @param to recipient email address
             * @param userName user's name
             * @param resetLink the password reset link
             * @param expirationMinutes how long the link is valid
             */
            void sendPasswordResetEmail(String to, String userName, String resetLink, int expirationMinutes);

            /**
             * Sends a notification email.
             *
             * @param to recipient email address
             * @param title notification title
             * @param message notification message
             */
            void sendNotificationEmail(String to, String title, String message);
        }
        """
                .formatted(basePackage);
    }

    private String generateImpl() {
        return """
        package %s.common.mail;

        import jakarta.mail.MessagingException;
        import jakarta.mail.internet.MimeMessage;
        import java.util.HashMap;
        import java.util.Map;
        import java.util.concurrent.CompletableFuture;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.mail.SimpleMailMessage;
        import org.springframework.mail.javamail.JavaMailSender;
        import org.springframework.mail.javamail.MimeMessageHelper;
        import org.springframework.scheduling.annotation.Async;
        import org.springframework.stereotype.Service;
        import org.thymeleaf.TemplateEngine;
        import org.thymeleaf.context.Context;

        /**
         * Implementation of MailService using Spring Boot Mail and Thymeleaf templates.
         */
        @Service
        public class MailServiceImpl implements MailService {

            private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

            private final JavaMailSender mailSender;
            private final TemplateEngine templateEngine;

            @Value("${spring.mail.from.address:noreply@example.com}")
            private String fromAddress;

            @Value("${spring.mail.from.name:Application}")
            private String fromName;

            @Value("${app.name:Application}")
            private String appName;

            public MailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
                this.mailSender = mailSender;
                this.templateEngine = templateEngine;
            }

            @Override
            public void sendSimpleEmail(String to, String subject, String text) {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromAddress);
                    message.setTo(to);
                    message.setSubject(subject);
                    message.setText(text);
                    mailSender.send(message);
                    log.info("Simple email sent to: {}", to);
                } catch (Exception e) {
                    log.error("Failed to send simple email to: {}", to, e);
                    throw new MailSendException("Failed to send email", e);
                }
            }

            @Override
            public void sendHtmlEmail(
                    String to, String subject, String templateName, Map<String, Object> variables) {
                try {
                    Context context = new Context();
                    context.setVariables(variables);
                    context.setVariable("appName", appName);

                    String htmlContent = templateEngine.process("email/" + templateName, context);

                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(fromAddress, fromName);
                    helper.setTo(to);
                    helper.setSubject(subject);
                    helper.setText(htmlContent, true);

                    mailSender.send(mimeMessage);
                    log.info("HTML email sent to: {} using template: {}", to, templateName);
                } catch (MessagingException | java.io.UnsupportedEncodingException e) {
                    log.error("Failed to send HTML email to: {}", to, e);
                    throw new MailSendException("Failed to send HTML email", e);
                }
            }

            @Override
            @Async
            public CompletableFuture<Void> sendHtmlEmailAsync(
                    String to, String subject, String templateName, Map<String, Object> variables) {
                return CompletableFuture.runAsync(
                        () -> sendHtmlEmail(to, subject, templateName, variables));
            }

            @Override
            public void send(EmailMessage message) {
                if (message.isHtml()) {
                    sendHtmlEmail(
                            message.getTo(),
                            message.getSubject(),
                            message.getTemplateName(),
                            message.getVariables());
                } else {
                    sendSimpleEmail(message.getTo(), message.getSubject(), message.getBody());
                }
            }

            @Override
            public void sendWelcomeEmail(String to, String userName) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", userName);
                variables.put("loginUrl", "/login");

                sendHtmlEmail(to, "Welcome to " + appName + "!", "welcome", variables);
            }

            @Override
            public void sendPasswordResetEmail(
                    String to, String userName, String resetLink, int expirationMinutes) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("userName", userName);
                variables.put("resetLink", resetLink);
                variables.put("expirationMinutes", expirationMinutes);

                sendHtmlEmail(to, "Password Reset Request", "password-reset", variables);
            }

            @Override
            public void sendNotificationEmail(String to, String title, String message) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("title", title);
                variables.put("message", message);

                sendHtmlEmail(to, title, "notification", variables);
            }

            /** Exception thrown when email sending fails. */
            public static class MailSendException extends RuntimeException {
                public MailSendException(String message, Throwable cause) {
                    super(message, cause);
                }
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateEmailMessage() {
        return """
        package %s.common.mail;

        import java.util.HashMap;
        import java.util.Map;
        import lombok.Builder;
        import lombok.Data;

        /**
         * Email message DTO for building emails programmatically.
         */
        @Data
        @Builder
        public class EmailMessage {
            private String to;
            private String subject;
            private String body;
            private String templateName;
            @Builder.Default
            private Map<String, Object> variables = new HashMap<>();
            @Builder.Default
            private boolean html = true;

            /**
             * Creates a simple text email.
             */
            public static EmailMessage simple(String to, String subject, String body) {
                return EmailMessage.builder()
                        .to(to)
                        .subject(subject)
                        .body(body)
                        .html(false)
                        .build();
            }

            /**
             * Creates an HTML email using a template.
             */
            public static EmailMessage fromTemplate(
                    String to, String subject, String templateName, Map<String, Object> variables) {
                return EmailMessage.builder()
                        .to(to)
                        .subject(subject)
                        .templateName(templateName)
                        .variables(variables)
                        .html(true)
                        .build();
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateLayoutTemplate() {
        return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title th:text="${subject}">Email</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 40px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #2563eb;
                    margin: 0;
                    font-size: 24px;
                }
                .content {
                    margin-bottom: 30px;
                }
                .button {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #2563eb;
                    color: #ffffff !important;
                    text-decoration: none;
                    border-radius: 6px;
                    font-weight: 600;
                }
                .button:hover {
                    background-color: #1d4ed8;
                }
                .footer {
                    text-align: center;
                    color: #6b7280;
                    font-size: 12px;
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1 th:text="${appName}">Application</h1>
                </div>
                <div class="content" th:replace="${content}">
                    <!-- Content will be inserted here -->
                </div>
                <div class="footer">
                    <p>&copy; <span th:text="${#dates.year(#dates.createNow())}">2024</span>
                       <span th:text="${appName}">Application</span>. All rights reserved.</p>
                    <p>This is an automated message. Please do not reply directly to this email.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateWelcomeTemplate() {
        return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Welcome</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 40px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #2563eb;
                    margin: 0;
                    font-size: 24px;
                }
                .button {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #2563eb;
                    color: #ffffff !important;
                    text-decoration: none;
                    border-radius: 6px;
                    font-weight: 600;
                }
                .footer {
                    text-align: center;
                    color: #6b7280;
                    font-size: 12px;
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1 th:text="${appName}">Application</h1>
                </div>
                <div class="content">
                    <h2>Welcome, <span th:text="${userName}">User</span>!</h2>
                    <p>Thank you for joining <span th:text="${appName}">our application</span>.
                       We're excited to have you on board!</p>
                    <p>Your account has been successfully created and is ready to use.</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a th:href="${loginUrl}" class="button">Get Started</a>
                    </p>
                    <p>If you have any questions, feel free to reach out to our support team.</p>
                    <p>Best regards,<br/>The <span th:text="${appName}">Application</span> Team</p>
                </div>
                <div class="footer">
                    <p>&copy; <span th:text="${#dates.year(#dates.createNow())}">2024</span>
                       <span th:text="${appName}">Application</span>. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String generatePasswordResetTemplate() {
        return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Password Reset</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 40px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #2563eb;
                    margin: 0;
                    font-size: 24px;
                }
                .button {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #dc2626;
                    color: #ffffff !important;
                    text-decoration: none;
                    border-radius: 6px;
                    font-weight: 600;
                }
                .warning {
                    background-color: #fef3c7;
                    border: 1px solid #f59e0b;
                    border-radius: 6px;
                    padding: 15px;
                    margin: 20px 0;
                }
                .footer {
                    text-align: center;
                    color: #6b7280;
                    font-size: 12px;
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1 th:text="${appName}">Application</h1>
                </div>
                <div class="content">
                    <h2>Password Reset Request</h2>
                    <p>Hello <span th:text="${userName}">User</span>,</p>
                    <p>We received a request to reset your password. Click the button below to
                       create a new password:</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a th:href="${resetLink}" class="button">Reset Password</a>
                    </p>
                    <div class="warning">
                        <strong>Important:</strong> This link will expire in
                        <span th:text="${expirationMinutes}">30</span> minutes.
                        If you didn't request this, you can safely ignore this email.
                    </div>
                    <p>If the button doesn't work, copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #2563eb;" th:text="${resetLink}">
                        https://example.com/reset
                    </p>
                    <p>Best regards,<br/>The <span th:text="${appName}">Application</span> Team</p>
                </div>
                <div class="footer">
                    <p>&copy; <span th:text="${#dates.year(#dates.createNow())}">2024</span>
                       <span th:text="${appName}">Application</span>. All rights reserved.</p>
                    <p>For security reasons, never share this email with anyone.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateNotificationTemplate() {
        return """
        <!DOCTYPE html>
        <html xmlns:th="http://www.thymeleaf.org">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Notification</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .container {
                    background-color: #ffffff;
                    border-radius: 8px;
                    padding: 40px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .header h1 {
                    color: #2563eb;
                    margin: 0;
                    font-size: 24px;
                }
                .notification-box {
                    background-color: #eff6ff;
                    border: 1px solid #bfdbfe;
                    border-radius: 6px;
                    padding: 20px;
                    margin: 20px 0;
                }
                .footer {
                    text-align: center;
                    color: #6b7280;
                    font-size: 12px;
                    margin-top: 30px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1 th:text="${appName}">Application</h1>
                </div>
                <div class="content">
                    <h2 th:text="${title}">Notification</h2>
                    <div class="notification-box">
                        <p th:utext="${message}">Notification message content.</p>
                    </div>
                    <p>Best regards,<br/>The <span th:text="${appName}">Application</span> Team</p>
                </div>
                <div class="footer">
                    <p>&copy; <span th:text="${#dates.year(#dates.createNow())}">2024</span>
                       <span th:text="${appName}">Application</span>. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }
}
