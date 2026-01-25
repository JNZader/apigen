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
package com.jnzader.apigen.codegen.generator.go.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for Go/Gin applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S2479",
    "java:S1192",
    "java:S3400"
}) // S2479: Literal tabs for Go code; S1192: template strings; S3400: template methods return
// constants
public class GoMailServiceGenerator {

    private final String moduleName;

    public GoMailServiceGenerator(String moduleName) {
        this.moduleName = moduleName;
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

        files.put("internal/mail/mail_service.go", generateMailService());
        files.put("internal/mail/config.go", generateConfig());

        if (generateWelcomeTemplate) {
            files.put("templates/email/welcome.html", generateWelcomeTemplate());
        }
        if (generatePasswordResetTemplate) {
            files.put("templates/email/password_reset.html", generatePasswordResetTemplate());
        }
        if (generateNotificationTemplate) {
            files.put("templates/email/notification.html", generateNotificationTemplate());
        }

        return files;
    }

    private String generateMailService() {
        return String.format(
                """
                package mail

                import (
                \t"bytes"
                \t"crypto/tls"
                \t"fmt"
                \t"html/template"
                \t"log"
                \t"net/smtp"
                \t"path/filepath"

                \t"github.com/spf13/viper"
                )

                // MailService handles email sending operations.
                type MailService struct {
                \tconfig    *Config
                \ttemplates *template.Template
                }

                // EmailData represents common email template data.
                type EmailData struct {
                \tAppName   string
                \tUserName  string
                \tYear      int
                \tExtraData map[string]interface{}
                }

                // NewMailService creates a new mail service instance.
                func NewMailService() (*MailService, error) {
                \tconfig := &Config{
                \t\tHost:     viper.GetString("mail.host"),
                \t\tPort:     viper.GetInt("mail.port"),
                \t\tUsername: viper.GetString("mail.username"),
                \t\tPassword: viper.GetString("mail.password"),
                \t\tFromAddr: viper.GetString("mail.from_address"),
                \t\tFromName: viper.GetString("mail.from_name"),
                \t\tUseTLS:   viper.GetBool("mail.use_tls"),
                \t}

                \tif config.Host == "" {
                \t\tconfig.Host = "localhost"
                \t}
                \tif config.Port == 0 {
                \t\tconfig.Port = 25
                \t}
                \tif config.FromName == "" {
                \t\tconfig.FromName = viper.GetString("app.name")
                \t}

                \t// Load templates
                \ttemplates, err := template.ParseGlob("templates/email/*.html")
                \tif err != nil {
                \t\tlog.Printf("Warning: could not load email templates: %%v", err)
                \t\ttemplates = template.New("")
                \t}

                \treturn &MailService{
                \t\tconfig:    config,
                \t\ttemplates: templates,
                \t}, nil
                }

                // SendSimpleEmail sends a plain text email.
                func (s *MailService) SendSimpleEmail(to, subject, body string) error {
                \treturn s.sendEmail(to, subject, body, "text/plain")
                }

                // SendHTMLEmail sends an HTML email using a template.
                func (s *MailService) SendHTMLEmail(to, subject, templateName string, data interface{}) error {
                \tvar buf bytes.Buffer
                \tif err := s.templates.ExecuteTemplate(&buf, filepath.Base(templateName), data); err != nil {
                \t\treturn fmt.Errorf("failed to render template: %%w", err)
                \t}
                \treturn s.sendEmail(to, subject, buf.String(), "text/html")
                }

                // SendWelcomeEmail sends a welcome email to a new user.
                func (s *MailService) SendWelcomeEmail(to, userName string) error {
                \tdata := EmailData{
                \t\tAppName:  viper.GetString("app.name"),
                \t\tUserName: userName,
                \t}
                \treturn s.SendHTMLEmail(to, "Welcome to "+data.AppName+"!", "welcome.html", data)
                }

                // SendPasswordResetEmail sends a password reset email.
                func (s *MailService) SendPasswordResetEmail(to, userName, resetLink string, expirationMinutes int) error {
                \tdata := struct {
                \t\tEmailData
                \t\tResetLink         string
                \t\tExpirationMinutes int
                \t}{
                \t\tEmailData: EmailData{
                \t\t\tAppName:  viper.GetString("app.name"),
                \t\t\tUserName: userName,
                \t\t},
                \t\tResetLink:         resetLink,
                \t\tExpirationMinutes: expirationMinutes,
                \t}
                \treturn s.SendHTMLEmail(to, "Password Reset Request", "password_reset.html", data)
                }

                // SendNotificationEmail sends a notification email.
                func (s *MailService) SendNotificationEmail(to, title, message string) error {
                \tdata := struct {
                \t\tEmailData
                \t\tTitle   string
                \t\tMessage string
                \t}{
                \t\tEmailData: EmailData{
                \t\t\tAppName: viper.GetString("app.name"),
                \t\t},
                \t\tTitle:   title,
                \t\tMessage: message,
                \t}
                \treturn s.SendHTMLEmail(to, title, "notification.html", data)
                }

                func (s *MailService) sendEmail(to, subject, body, contentType string) error {
                \tfrom := fmt.Sprintf("%%s <%%s>", s.config.FromName, s.config.FromAddr)

                \theaders := make(map[string]string)
                \theaders["From"] = from
                \theaders["To"] = to
                \theaders["Subject"] = subject
                \theaders["MIME-Version"] = "1.0"
                \theaders["Content-Type"] = contentType + "; charset=UTF-8"

                \tvar msg bytes.Buffer
                \tfor k, v := range headers {
                \t\tmsg.WriteString(fmt.Sprintf("%%s: %%s\\r\\n", k, v))
                \t}
                \tmsg.WriteString("\\r\\n")
                \tmsg.WriteString(body)

                \taddr := fmt.Sprintf("%%s:%%d", s.config.Host, s.config.Port)

                \tvar auth smtp.Auth
                \tif s.config.Username != "" {
                \t\tauth = smtp.PlainAuth("", s.config.Username, s.config.Password, s.config.Host)
                \t}

                \tif s.config.UseTLS {
                \t\treturn s.sendWithTLS(addr, auth, s.config.FromAddr, []string{to}, msg.Bytes())
                \t}

                \treturn smtp.SendMail(addr, auth, s.config.FromAddr, []string{to}, msg.Bytes())
                }

                func (s *MailService) sendWithTLS(addr string, auth smtp.Auth, from string, to []string, msg []byte) error {
                \tconn, err := tls.Dial("tcp", addr, &tls.Config{
                \t\tServerName: s.config.Host,
                \t})
                \tif err != nil {
                \t\treturn fmt.Errorf("TLS dial failed: %%w", err)
                \t}
                \tdefer conn.Close()

                \tclient, err := smtp.NewClient(conn, s.config.Host)
                \tif err != nil {
                \t\treturn fmt.Errorf("SMTP client creation failed: %%w", err)
                \t}
                \tdefer client.Close()

                \tif auth != nil {
                \t\tif err = client.Auth(auth); err != nil {
                \t\t\treturn fmt.Errorf("SMTP auth failed: %%w", err)
                \t\t}
                \t}

                \tif err = client.Mail(from); err != nil {
                \t\treturn fmt.Errorf("SMTP MAIL command failed: %%w", err)
                \t}

                \tfor _, recipient := range to {
                \t\tif err = client.Rcpt(recipient); err != nil {
                \t\t\treturn fmt.Errorf("SMTP RCPT command failed: %%w", err)
                \t\t}
                \t}

                \tw, err := client.Data()
                \tif err != nil {
                \t\treturn fmt.Errorf("SMTP DATA command failed: %%w", err)
                \t}

                \t_, err = w.Write(msg)
                \tif err != nil {
                \t\treturn fmt.Errorf("writing message failed: %%w", err)
                \t}

                \terr = w.Close()
                \tif err != nil {
                \t\treturn fmt.Errorf("closing message failed: %%w", err)
                \t}

                \treturn client.Quit()
                }
                """,
                moduleName);
    }

    private String generateConfig() {
        return """
        package mail

        // Config holds mail server configuration.
        type Config struct {
        \tHost     string
        \tPort     int
        \tUsername string
        \tPassword string
        \tFromAddr string
        \tFromName string
        \tUseTLS   bool
        }
        """;
    }

    private String generateWelcomeTemplate() {
        return """
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
                <div class="header"><h1>{{.AppName}}</h1></div>
                <h2>Welcome, {{.UserName}}!</h2>
                <p>Thank you for joining {{.AppName}}. We're excited to have you on board!</p>
                <p>Your account has been successfully created and is ready to use.</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="/login" class="button">Get Started</a>
                </p>
                <p>Best regards,<br/>The {{.AppName}} Team</p>
                <div class="footer"><p>&copy; {{.AppName}}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }

    private String generatePasswordResetTemplate() {
        return """
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
                <div class="header"><h1>{{.AppName}}</h1></div>
                <h2>Password Reset Request</h2>
                <p>Hello {{.UserName}},</p>
                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="{{.ResetLink}}" class="button">Reset Password</a>
                </p>
                <div class="warning">
                    <strong>Important:</strong> This link will expire in {{.ExpirationMinutes}} minutes.
                    If you didn't request this, you can safely ignore this email.
                </div>
                <p>Best regards,<br/>The {{.AppName}} Team</p>
                <div class="footer"><p>&copy; {{.AppName}}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateNotificationTemplate() {
        return """
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
                <div class="header"><h1>{{.AppName}}</h1></div>
                <h2>{{.Title}}</h2>
                <div class="notification"><p>{{.Message}}</p></div>
                <p>Best regards,<br/>The {{.AppName}} Team</p>
                <div class="footer"><p>&copy; {{.AppName}}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }
}
