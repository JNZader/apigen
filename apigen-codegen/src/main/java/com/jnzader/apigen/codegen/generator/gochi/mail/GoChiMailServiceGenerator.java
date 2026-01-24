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
package com.jnzader.apigen.codegen.generator.gochi.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for Go/Chi applications.
 *
 * @author APiGen
 * @since 2.16.0
 */
@SuppressWarnings("UnusedVariable") // moduleName kept for API consistency with other generators
public class GoChiMailServiceGenerator {

    private final String moduleName;

    public GoChiMailServiceGenerator(String moduleName) {
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
        return """
        package mail

        import (
        	"bytes"
        	"crypto/tls"
        	"fmt"
        	"html/template"
        	"log/slog"
        	"net/smtp"
        	"path/filepath"

        	"github.com/spf13/viper"
        )

        // MailService handles email sending operations.
        type MailService struct {
        	config    *Config
        	templates *template.Template
        	logger    *slog.Logger
        }

        // EmailData represents common email template data.
        type EmailData struct {
        	AppName   string
        	UserName  string
        	Year      int
        	ExtraData map[string]interface{}
        }

        // NewMailService creates a new mail service instance.
        func NewMailService(logger *slog.Logger) (*MailService, error) {
        	config := &Config{
        		Host:     viper.GetString("mail.host"),
        		Port:     viper.GetInt("mail.port"),
        		Username: viper.GetString("mail.username"),
        		Password: viper.GetString("mail.password"),
        		FromAddr: viper.GetString("mail.from_address"),
        		FromName: viper.GetString("mail.from_name"),
        		UseTLS:   viper.GetBool("mail.use_tls"),
        	}

        	if config.Host == "" {
        		config.Host = "localhost"
        	}
        	if config.Port == 0 {
        		config.Port = 25
        	}
        	if config.FromName == "" {
        		config.FromName = viper.GetString("app.name")
        	}

        	// Load templates
        	templates, err := template.ParseGlob("templates/email/*.html")
        	if err != nil {
        		logger.Warn("could not load email templates", "error", err)
        		templates = template.New("")
        	}

        	return &MailService{
        		config:    config,
        		templates: templates,
        		logger:    logger.With("service", "mail"),
        	}, nil
        }

        // SendSimpleEmail sends a plain text email.
        func (s *MailService) SendSimpleEmail(to, subject, body string) error {
        	return s.sendEmail(to, subject, body, "text/plain")
        }

        // SendHTMLEmail sends an HTML email using a template.
        func (s *MailService) SendHTMLEmail(to, subject, templateName string, data interface{}) error {
        	var buf bytes.Buffer
        	if err := s.templates.ExecuteTemplate(&buf, filepath.Base(templateName), data); err != nil {
        		return fmt.Errorf("failed to render template: %%w", err)
        	}
        	return s.sendEmail(to, subject, buf.String(), "text/html")
        }

        // SendWelcomeEmail sends a welcome email to a new user.
        func (s *MailService) SendWelcomeEmail(to, userName string) error {
        	data := EmailData{
        		AppName:  viper.GetString("app.name"),
        		UserName: userName,
        	}
        	return s.SendHTMLEmail(to, "Welcome to "+data.AppName+"!", "welcome.html", data)
        }

        // SendPasswordResetEmail sends a password reset email.
        func (s *MailService) SendPasswordResetEmail(to, userName, resetLink string, expirationMinutes int) error {
        	data := struct {
        		EmailData
        		ResetLink         string
        		ExpirationMinutes int
        	}{
        		EmailData: EmailData{
        			AppName:  viper.GetString("app.name"),
        			UserName: userName,
        		},
        		ResetLink:         resetLink,
        		ExpirationMinutes: expirationMinutes,
        	}
        	return s.SendHTMLEmail(to, "Password Reset Request", "password_reset.html", data)
        }

        // SendNotificationEmail sends a notification email.
        func (s *MailService) SendNotificationEmail(to, title, message string) error {
        	data := struct {
        		EmailData
        		Title   string
        		Message string
        	}{
        		EmailData: EmailData{
        			AppName: viper.GetString("app.name"),
        		},
        		Title:   title,
        		Message: message,
        	}
        	return s.SendHTMLEmail(to, title, "notification.html", data)
        }

        func (s *MailService) sendEmail(to, subject, body, contentType string) error {
        	from := fmt.Sprintf("%%s <%%s>", s.config.FromName, s.config.FromAddr)

        	headers := make(map[string]string)
        	headers["From"] = from
        	headers["To"] = to
        	headers["Subject"] = subject
        	headers["MIME-Version"] = "1.0"
        	headers["Content-Type"] = contentType + "; charset=UTF-8"

        	var msg bytes.Buffer
        	for k, v := range headers {
        		msg.WriteString(fmt.Sprintf("%%s: %%s\\r\\n", k, v))
        	}
        	msg.WriteString("\\r\\n")
        	msg.WriteString(body)

        	addr := fmt.Sprintf("%%s:%%d", s.config.Host, s.config.Port)

        	var auth smtp.Auth
        	if s.config.Username != "" {
        		auth = smtp.PlainAuth("", s.config.Username, s.config.Password, s.config.Host)
        	}

        	if s.config.UseTLS {
        		return s.sendWithTLS(addr, auth, s.config.FromAddr, []string{to}, msg.Bytes())
        	}

        	err := smtp.SendMail(addr, auth, s.config.FromAddr, []string{to}, msg.Bytes())
        	if err != nil {
        		s.logger.Error("failed to send email", "to", to, "error", err)
        		return err
        	}
        	s.logger.Info("email sent", "to", to, "subject", subject)
        	return nil
        }

        func (s *MailService) sendWithTLS(addr string, auth smtp.Auth, from string, to []string, msg []byte) error {
        	conn, err := tls.Dial("tcp", addr, &tls.Config{
        		ServerName: s.config.Host,
        	})
        	if err != nil {
        		return fmt.Errorf("TLS dial failed: %%w", err)
        	}
        	defer conn.Close()

        	client, err := smtp.NewClient(conn, s.config.Host)
        	if err != nil {
        		return fmt.Errorf("SMTP client creation failed: %%w", err)
        	}
        	defer client.Close()

        	if auth != nil {
        		if err = client.Auth(auth); err != nil {
        			return fmt.Errorf("SMTP auth failed: %%w", err)
        		}
        	}

        	if err = client.Mail(from); err != nil {
        		return fmt.Errorf("SMTP MAIL command failed: %%w", err)
        	}

        	for _, recipient := range to {
        		if err = client.Rcpt(recipient); err != nil {
        			return fmt.Errorf("SMTP RCPT command failed: %%w", err)
        		}
        	}

        	w, err := client.Data()
        	if err != nil {
        		return fmt.Errorf("SMTP DATA command failed: %%w", err)
        	}

        	_, err = w.Write(msg)
        	if err != nil {
        		return fmt.Errorf("writing message failed: %%w", err)
        	}

        	err = w.Close()
        	if err != nil {
        		return fmt.Errorf("closing message failed: %%w", err)
        	}

        	s.logger.Info("email sent via TLS", "to", to[0])
        	return client.Quit()
        }
        """;
    }

    private String generateConfig() {
        return """
        package mail

        // Config holds mail server configuration.
        type Config struct {
        	Host     string
        	Port     int
        	Username string
        	Password string
        	FromAddr string
        	FromName string
        	UseTLS   bool
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
