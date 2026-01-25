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
package com.jnzader.apigen.codegen.generator.rust.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for Rust/Axum applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class RustMailServiceGenerator {

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

        files.put("src/mail/mod.rs", generateModRs());
        files.put("src/mail/service.rs", generateMailService());
        files.put("src/mail/config.rs", generateConfig());

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

    private String generateModRs() {
        return """
        //! Email service module.

        mod config;
        mod service;

        pub use config::MailConfig;
        pub use service::MailService;
        """;
    }

    private String generateMailService() {
        return """
        //! Email sending service using lettre.

        use crate::error::AppError;
        use crate::mail::MailConfig;
        use lettre::{
            message::{header::ContentType, Mailbox},
            transport::smtp::authentication::Credentials,
            AsyncSmtpTransport, AsyncTransport, Message, Tokio1Executor,
        };
        use std::sync::Arc;
        use tera::{Context, Tera};
        use tracing::{info, error};

        /// Email service for sending emails.
        #[derive(Clone)]
        pub struct MailService {
            transport: Arc<AsyncSmtpTransport<Tokio1Executor>>,
            templates: Arc<Tera>,
            config: MailConfig,
        }

        impl MailService {
            /// Create a new mail service.
            pub fn new(config: MailConfig) -> Result<Self, AppError> {
                let creds = Credentials::new(
                    config.username.clone(),
                    config.password.clone(),
                );

                let transport = if config.use_tls {
                    AsyncSmtpTransport::<Tokio1Executor>::relay(&config.host)?
                        .credentials(creds)
                        .port(config.port)
                        .build()
                } else {
                    AsyncSmtpTransport::<Tokio1Executor>::builder_dangerous(&config.host)
                        .credentials(creds)
                        .port(config.port)
                        .build()
                };

                let templates = match Tera::new("templates/email/**/*.html") {
                    Ok(t) => t,
                    Err(e) => {
                        tracing::warn!("Could not load email templates: {}", e);
                        Tera::default()
                    }
                };

                Ok(Self {
                    transport: Arc::new(transport),
                    templates: Arc::new(templates),
                    config,
                })
            }

            /// Send a simple text email.
            pub async fn send_simple(&self, to: &str, subject: &str, body: &str) -> Result<(), AppError> {
                let from: Mailbox = format!("{} <{}>", self.config.from_name, self.config.from_address)
                    .parse()
                    .map_err(|e| AppError::Internal(format!("Invalid from address: {}", e)))?;

                let to: Mailbox = to
                    .parse()
                    .map_err(|e| AppError::Internal(format!("Invalid to address: {}", e)))?;

                let email = Message::builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .header(ContentType::TEXT_PLAIN)
                    .body(body.to_string())
                    .map_err(|e| AppError::Internal(format!("Failed to build email: {}", e)))?;

                self.transport
                    .send(email)
                    .await
                    .map_err(|e| AppError::Internal(format!("Failed to send email: {}", e)))?;

                info!("Simple email sent to: {}", to);
                Ok(())
            }

            /// Send an HTML email using a template.
            pub async fn send_html(
                &self,
                to: &str,
                subject: &str,
                template: &str,
                context: &Context,
            ) -> Result<(), AppError> {
                let body = self
                    .templates
                    .render(template, context)
                    .map_err(|e| AppError::Internal(format!("Template error: {}", e)))?;

                let from: Mailbox = format!("{} <{}>", self.config.from_name, self.config.from_address)
                    .parse()
                    .map_err(|e| AppError::Internal(format!("Invalid from address: {}", e)))?;

                let to: Mailbox = to
                    .parse()
                    .map_err(|e| AppError::Internal(format!("Invalid to address: {}", e)))?;

                let email = Message::builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .header(ContentType::TEXT_HTML)
                    .body(body)
                    .map_err(|e| AppError::Internal(format!("Failed to build email: {}", e)))?;

                self.transport
                    .send(email)
                    .await
                    .map_err(|e| AppError::Internal(format!("Failed to send email: {}", e)))?;

                info!("HTML email sent to: {} using template: {}", to, template);
                Ok(())
            }

            /// Send a welcome email.
            pub async fn send_welcome(&self, to: &str, user_name: &str) -> Result<(), AppError> {
                let mut context = Context::new();
                context.insert("app_name", &self.config.from_name);
                context.insert("user_name", user_name);

                self.send_html(
                    to,
                    &format!("Welcome to {}!", self.config.from_name),
                    "welcome.html",
                    &context,
                )
                .await
            }

            /// Send a password reset email.
            pub async fn send_password_reset(
                &self,
                to: &str,
                user_name: &str,
                reset_link: &str,
                expiration_minutes: i32,
            ) -> Result<(), AppError> {
                let mut context = Context::new();
                context.insert("app_name", &self.config.from_name);
                context.insert("user_name", user_name);
                context.insert("reset_link", reset_link);
                context.insert("expiration_minutes", &expiration_minutes);

                self.send_html(to, "Password Reset Request", "password_reset.html", &context)
                    .await
            }

            /// Send a notification email.
            pub async fn send_notification(
                &self,
                to: &str,
                title: &str,
                message: &str,
            ) -> Result<(), AppError> {
                let mut context = Context::new();
                context.insert("app_name", &self.config.from_name);
                context.insert("title", title);
                context.insert("message", message);

                self.send_html(to, title, "notification.html", &context).await
            }
        }
        """;
    }

    private String generateConfig() {
        return """
        //! Mail configuration.

        use serde::Deserialize;

        /// Mail server configuration.
        #[derive(Debug, Clone, Deserialize)]
        pub struct MailConfig {
            pub host: String,
            pub port: u16,
            pub username: String,
            pub password: String,
            pub from_address: String,
            pub from_name: String,
            pub use_tls: bool,
        }

        impl Default for MailConfig {
            fn default() -> Self {
                Self {
                    host: "localhost".to_string(),
                    port: 25,
                    username: String::new(),
                    password: String::new(),
                    from_address: "noreply@example.com".to_string(),
                    from_name: "Application".to_string(),
                    use_tls: false,
                }
            }
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
                <div class="header"><h1>{{ app_name }}</h1></div>
                <h2>Welcome, {{ user_name }}!</h2>
                <p>Thank you for joining {{ app_name }}. We're excited to have you on board!</p>
                <p>Your account has been successfully created and is ready to use.</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="/login" class="button">Get Started</a>
                </p>
                <p>Best regards,<br/>The {{ app_name }} Team</p>
                <div class="footer"><p>&copy; {{ app_name }}. All rights reserved.</p></div>
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
                <div class="header"><h1>{{ app_name }}</h1></div>
                <h2>Password Reset Request</h2>
                <p>Hello {{ user_name }},</p>
                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="{{ reset_link }}" class="button">Reset Password</a>
                </p>
                <div class="warning">
                    <strong>Important:</strong> This link will expire in {{ expiration_minutes }} minutes.
                    If you didn't request this, you can safely ignore this email.
                </div>
                <p>Best regards,<br/>The {{ app_name }} Team</p>
                <div class="footer"><p>&copy; {{ app_name }}. All rights reserved.</p></div>
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
                <div class="header"><h1>{{ app_name }}</h1></div>
                <h2>{{ title }}</h2>
                <div class="notification"><p>{{ message }}</p></div>
                <p>Best regards,<br/>The {{ app_name }} Team</p>
                <div class="footer"><p>&copy; {{ app_name }}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }
}
