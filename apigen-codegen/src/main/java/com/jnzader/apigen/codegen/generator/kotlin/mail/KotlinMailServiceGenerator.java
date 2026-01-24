package com.jnzader.apigen.codegen.generator.kotlin.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Kotlin mail service code including interface, implementation, and email templates.
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
public class KotlinMailServiceGenerator {

    private static final String PKG_COMMON_MAIL = "common/mail";

    private final String basePackage;

    public KotlinMailServiceGenerator(String basePackage) {
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
        String basePath = "src/main/kotlin/" + basePackage.replace('.', '/');

        // Generate interface
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/MailService.kt", generateInterface());

        // Generate implementation
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/MailServiceImpl.kt", generateImpl());

        // Generate EmailMessage data class
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/EmailMessage.kt", generateEmailMessage());

        // Generate exception
        files.put(basePath + "/" + PKG_COMMON_MAIL + "/MailSendException.kt", generateException());

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
        package %s.common.mail

        import java.util.concurrent.CompletableFuture

        /**
         * Service interface for sending emails.
         *
         * Provides both synchronous and asynchronous email sending capabilities
         * with support for plain text and HTML templates.
         */
        interface MailService {

            /**
             * Sends a simple text email.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param text email body text
             */
            fun sendSimpleEmail(to: String, subject: String, text: String)

            /**
             * Sends an HTML email using a template.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param templateName the Thymeleaf template name (without .html extension)
             * @param variables template variables
             */
            fun sendHtmlEmail(to: String, subject: String, templateName: String, variables: Map<String, Any>)

            /**
             * Sends an HTML email asynchronously.
             *
             * @param to recipient email address
             * @param subject email subject
             * @param templateName the Thymeleaf template name
             * @param variables template variables
             * @return CompletableFuture that completes when email is sent
             */
            fun sendHtmlEmailAsync(
                to: String,
                subject: String,
                templateName: String,
                variables: Map<String, Any>
            ): CompletableFuture<Void>

            /**
             * Sends an email using EmailMessage object.
             *
             * @param message the email message
             */
            fun send(message: EmailMessage)

            /**
             * Sends a welcome email to a new user.
             *
             * @param to recipient email address
             * @param userName user's name
             */
            fun sendWelcomeEmail(to: String, userName: String)

            /**
             * Sends a password reset email.
             *
             * @param to recipient email address
             * @param userName user's name
             * @param resetLink the password reset link
             * @param expirationMinutes how long the link is valid
             */
            fun sendPasswordResetEmail(to: String, userName: String, resetLink: String, expirationMinutes: Int)

            /**
             * Sends a notification email.
             *
             * @param to recipient email address
             * @param title notification title
             * @param message notification message
             */
            fun sendNotificationEmail(to: String, title: String, message: String)
        }
        """
                .formatted(basePackage);
    }

    private String generateImpl() {
        return """
        package %s.common.mail

        import jakarta.mail.MessagingException
        import org.slf4j.LoggerFactory
        import org.springframework.beans.factory.annotation.Value
        import org.springframework.mail.SimpleMailMessage
        import org.springframework.mail.javamail.JavaMailSender
        import org.springframework.mail.javamail.MimeMessageHelper
        import org.springframework.scheduling.annotation.Async
        import org.springframework.stereotype.Service
        import org.thymeleaf.TemplateEngine
        import org.thymeleaf.context.Context
        import java.util.concurrent.CompletableFuture

        /**
         * Implementation of MailService using Spring Boot Mail and Thymeleaf templates.
         */
        @Service
        class MailServiceImpl(
            private val mailSender: JavaMailSender,
            private val templateEngine: TemplateEngine
        ) : MailService {

            private val log = LoggerFactory.getLogger(MailServiceImpl::class.java)

            @Value("\\${spring.mail.from.address:noreply@example.com}")
            private lateinit var fromAddress: String

            @Value("\\${spring.mail.from.name:Application}")
            private lateinit var fromName: String

            @Value("\\${app.name:Application}")
            private lateinit var appName: String

            override fun sendSimpleEmail(to: String, subject: String, text: String) {
                try {
                    val message = SimpleMailMessage().apply {
                        setFrom(fromAddress)
                        setTo(to)
                        setSubject(subject)
                        setText(text)
                    }
                    mailSender.send(message)
                    log.info("Simple email sent to: {}", to)
                } catch (e: Exception) {
                    log.error("Failed to send simple email to: {}", to, e)
                    throw MailSendException("Failed to send email", e)
                }
            }

            override fun sendHtmlEmail(
                to: String,
                subject: String,
                templateName: String,
                variables: Map<String, Any>
            ) {
                try {
                    val context = Context().apply {
                        setVariables(variables)
                        setVariable("appName", appName)
                    }

                    val htmlContent = templateEngine.process("email/$templateName", context)

                    val mimeMessage = mailSender.createMimeMessage()
                    val helper = MimeMessageHelper(mimeMessage, true, "UTF-8")

                    helper.setFrom(fromAddress, fromName)
                    helper.setTo(to)
                    helper.setSubject(subject)
                    helper.setText(htmlContent, true)

                    mailSender.send(mimeMessage)
                    log.info("HTML email sent to: {} using template: {}", to, templateName)
                } catch (e: MessagingException) {
                    log.error("Failed to send HTML email to: {}", to, e)
                    throw MailSendException("Failed to send HTML email", e)
                } catch (e: java.io.UnsupportedEncodingException) {
                    log.error("Failed to send HTML email to: {}", to, e)
                    throw MailSendException("Failed to send HTML email", e)
                }
            }

            @Async
            override fun sendHtmlEmailAsync(
                to: String,
                subject: String,
                templateName: String,
                variables: Map<String, Any>
            ): CompletableFuture<Void> {
                return CompletableFuture.runAsync {
                    sendHtmlEmail(to, subject, templateName, variables)
                }
            }

            override fun send(message: EmailMessage) {
                if (message.html) {
                    sendHtmlEmail(message.to, message.subject, message.templateName!!, message.variables)
                } else {
                    sendSimpleEmail(message.to, message.subject, message.body!!)
                }
            }

            override fun sendWelcomeEmail(to: String, userName: String) {
                val variables = mapOf(
                    "userName" to userName,
                    "loginUrl" to "/login"
                )
                sendHtmlEmail(to, "Welcome to $appName!", "welcome", variables)
            }

            override fun sendPasswordResetEmail(
                to: String,
                userName: String,
                resetLink: String,
                expirationMinutes: Int
            ) {
                val variables = mapOf(
                    "userName" to userName,
                    "resetLink" to resetLink,
                    "expirationMinutes" to expirationMinutes
                )
                sendHtmlEmail(to, "Password Reset Request", "password-reset", variables)
            }

            override fun sendNotificationEmail(to: String, title: String, message: String) {
                val variables = mapOf(
                    "title" to title,
                    "message" to message
                )
                sendHtmlEmail(to, title, "notification", variables)
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateEmailMessage() {
        return """
        package %s.common.mail

        /**
         * Email message data class for building emails programmatically.
         */
        data class EmailMessage(
            val to: String,
            val subject: String,
            val body: String? = null,
            val templateName: String? = null,
            val variables: Map<String, Any> = emptyMap(),
            val html: Boolean = true
        ) {
            companion object {
                /**
                 * Creates a simple text email.
                 */
                fun simple(to: String, subject: String, body: String): EmailMessage =
                    EmailMessage(
                        to = to,
                        subject = subject,
                        body = body,
                        html = false
                    )

                /**
                 * Creates an HTML email using a template.
                 */
                fun fromTemplate(
                    to: String,
                    subject: String,
                    templateName: String,
                    variables: Map<String, Any>
                ): EmailMessage =
                    EmailMessage(
                        to = to,
                        subject = subject,
                        templateName = templateName,
                        variables = variables,
                        html = true
                    )
            }
        }
        """
                .formatted(basePackage);
    }

    private String generateException() {
        return """
        package %s.common.mail

        /**
         * Exception thrown when email sending fails.
         */
        class MailSendException : RuntimeException {
            constructor(message: String) : super(message)
            constructor(message: String, cause: Throwable) : super(message, cause)
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
