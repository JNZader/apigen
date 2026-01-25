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
package com.jnzader.apigen.codegen.generator.python.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for Python/FastAPI applications.
 *
 * <p>Creates email service using fastapi-mail with Jinja2 templates.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings("java:S3400") // Template methods return constants for code generation
public class PythonMailServiceGenerator {

    /**
     * Generates all mail service files.
     *
     * @param generateWelcomeTemplate whether to generate welcome email template
     * @param generatePasswordResetTemplate whether to generate password reset template
     * @param generateNotificationTemplate whether to generate notification template
     * @return map of file path to content
     */
    public Map<String, String> generate(
            boolean generateWelcomeTemplate,
            boolean generatePasswordResetTemplate,
            boolean generateNotificationTemplate) {

        Map<String, String> files = new LinkedHashMap<>();

        // Mail service module
        files.put("app/services/mail_service.py", generateMailService());

        // Mail schemas
        files.put("app/schemas/mail.py", generateMailSchemas());

        // Mail configuration
        files.put("app/core/mail_config.py", generateMailConfig());

        // Templates
        if (generateWelcomeTemplate) {
            files.put("app/templates/email/welcome.html", generateWelcomeTemplate());
        }

        if (generatePasswordResetTemplate) {
            files.put("app/templates/email/password_reset.html", generatePasswordResetTemplate());
        }

        if (generateNotificationTemplate) {
            files.put("app/templates/email/notification.html", generateNotificationTemplate());
        }

        // Base layout
        files.put("app/templates/email/base.html", generateBaseTemplate());

        return files;
    }

    private String generateMailService() {
        return """
        \"""Email service for sending emails using fastapi-mail.\"""

        import asyncio
        from pathlib import Path
        from typing import Any

        from fastapi_mail import ConnectionConfig, FastMail, MessageSchema, MessageType
        from jinja2 import Environment, FileSystemLoader
        from pydantic import EmailStr

        from app.core.mail_config import get_mail_config
        from app.schemas.mail import EmailMessage


        class MailService:
            \"""Service for sending emails with template support.\"""

            def __init__(self):
                self.config = get_mail_config()
                self.fm = FastMail(self.config)
                template_dir = Path(__file__).parent.parent / "templates" / "email"
                self.jinja_env = Environment(
                    loader=FileSystemLoader(str(template_dir)),
                    autoescape=True,
                )

            def _render_template(self, template_name: str, context: dict[str, Any]) -> str:
                \"""Render a Jinja2 template with the given context.\"""
                template = self.jinja_env.get_template(f"{template_name}.html")
                return template.render(**context)

            async def send_simple_email(
                self, to: EmailStr, subject: str, body: str
            ) -> None:
                \"""Send a simple text email.\"""
                message = MessageSchema(
                    subject=subject,
                    recipients=[to],
                    body=body,
                    subtype=MessageType.plain,
                )
                await self.fm.send_message(message)

            async def send_html_email(
                self,
                to: EmailStr,
                subject: str,
                template_name: str,
                context: dict[str, Any] | None = None,
            ) -> None:
                \"""Send an HTML email using a template.\"""
                context = context or {}
                html_content = self._render_template(template_name, context)

                message = MessageSchema(
                    subject=subject,
                    recipients=[to],
                    body=html_content,
                    subtype=MessageType.html,
                )
                await self.fm.send_message(message)

            async def send(self, email_message: EmailMessage) -> None:
                \"""Send an email using EmailMessage object.\"""
                if email_message.template_name:
                    await self.send_html_email(
                        to=email_message.to,
                        subject=email_message.subject,
                        template_name=email_message.template_name,
                        context=email_message.context,
                    )
                else:
                    await self.send_simple_email(
                        to=email_message.to,
                        subject=email_message.subject,
                        body=email_message.body or "",
                    )

            async def send_welcome_email(self, to: EmailStr, user_name: str) -> None:
                \"""Send a welcome email to a new user.\"""
                await self.send_html_email(
                    to=to,
                    subject="Welcome!",
                    template_name="welcome",
                    context={"user_name": user_name},
                )

            async def send_password_reset_email(
                self,
                to: EmailStr,
                user_name: str,
                reset_link: str,
                expiration_minutes: int = 30,
            ) -> None:
                \"""Send a password reset email.\"""
                await self.send_html_email(
                    to=to,
                    subject="Password Reset Request",
                    template_name="password_reset",
                    context={
                        "user_name": user_name,
                        "reset_link": reset_link,
                        "expiration_minutes": expiration_minutes,
                    },
                )

            async def send_notification_email(
                self, to: EmailStr, title: str, message: str
            ) -> None:
                \"""Send a notification email.\"""
                await self.send_html_email(
                    to=to,
                    subject=title,
                    template_name="notification",
                    context={"title": title, "message": message},
                )

            def send_sync(self, email_message: EmailMessage) -> None:
                \"""Synchronous wrapper for sending emails.\"""
                asyncio.run(self.send(email_message))


        # Singleton instance
        _mail_service: MailService | None = None


        def get_mail_service() -> MailService:
            \"""Get or create mail service singleton.\"""
            global _mail_service
            if _mail_service is None:
                _mail_service = MailService()
            return _mail_service
        """;
    }

    private String generateMailSchemas() {
        return """
        \"""Email message schemas.\"""

        from typing import Any

        from pydantic import BaseModel, EmailStr


        class EmailMessage(BaseModel):
            \"""Email message DTO.\"""

            to: EmailStr
            subject: str
            body: str | None = None
            template_name: str | None = None
            context: dict[str, Any] = {}

            @classmethod
            def simple(cls, to: EmailStr, subject: str, body: str) -> "EmailMessage":
                \"""Create a simple text email.\"""
                return cls(to=to, subject=subject, body=body)

            @classmethod
            def from_template(
                cls,
                to: EmailStr,
                subject: str,
                template_name: str,
                context: dict[str, Any] | None = None,
            ) -> "EmailMessage":
                \"""Create an HTML email from template.\"""
                return cls(
                    to=to,
                    subject=subject,
                    template_name=template_name,
                    context=context or {},
                )


        class SendEmailRequest(BaseModel):
            \"""Request schema for sending emails via API.\"""

            to: EmailStr
            subject: str
            body: str | None = None
            template_name: str | None = None
            context: dict[str, Any] = {}


        class SendEmailResponse(BaseModel):
            \"""Response schema for email sending.\"""

            success: bool
            message: str
        """;
    }

    private String generateMailConfig() {
        return """
        \"""Mail configuration using fastapi-mail.\"""

        import os

        from fastapi_mail import ConnectionConfig


        def get_mail_config() -> ConnectionConfig:
            \"""Get mail connection configuration from environment.\"""
            return ConnectionConfig(
                MAIL_USERNAME=os.getenv("MAIL_USERNAME", ""),
                MAIL_PASSWORD=os.getenv("MAIL_PASSWORD", ""),
                MAIL_FROM=os.getenv("MAIL_FROM", "noreply@example.com"),
                MAIL_PORT=int(os.getenv("MAIL_PORT", "587")),
                MAIL_SERVER=os.getenv("MAIL_SERVER", "smtp.example.com"),
                MAIL_FROM_NAME=os.getenv("MAIL_FROM_NAME", "Application"),
                MAIL_STARTTLS=os.getenv("MAIL_STARTTLS", "true").lower() == "true",
                MAIL_SSL_TLS=os.getenv("MAIL_SSL_TLS", "false").lower() == "true",
                USE_CREDENTIALS=os.getenv("MAIL_USE_CREDENTIALS", "true").lower() == "true",
                VALIDATE_CERTS=os.getenv("MAIL_VALIDATE_CERTS", "true").lower() == "true",
            )
        """;
    }

    private String generateBaseTemplate() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>{% block title %}Email{% endblock %}</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
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
                    <h1>{{ app_name | default('Application') }}</h1>
                </div>
                <div class="content">
                    {% block content %}{% endblock %}
                </div>
                <div class="footer">
                    <p>&copy; {{ current_year | default('2024') }} {{ app_name | default('Application') }}. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateWelcomeTemplate() {
        return """
        {% extends "base.html" %}

        {% block title %}Welcome{% endblock %}

        {% block content %}
        <h2>Welcome, {{ user_name }}!</h2>
        <p>Thank you for joining our application. We're excited to have you on board!</p>
        <p>Your account has been successfully created and is ready to use.</p>
        <p style="text-align: center; margin: 30px 0;">
            <a href="{{ login_url | default('/login') }}" class="button">Get Started</a>
        </p>
        <p>If you have any questions, feel free to reach out to our support team.</p>
        <p>Best regards,<br/>The Team</p>
        {% endblock %}
        """;
    }

    private String generatePasswordResetTemplate() {
        return """
        {% extends "base.html" %}

        {% block title %}Password Reset{% endblock %}

        {% block content %}
        <h2>Password Reset Request</h2>
        <p>Hello {{ user_name }},</p>
        <p>We received a request to reset your password. Click the button below to create a new password:</p>
        <p style="text-align: center; margin: 30px 0;">
            <a href="{{ reset_link }}" class="button" style="background-color: #dc2626;">Reset Password</a>
        </p>
        <div style="background-color: #fef3c7; border: 1px solid #f59e0b; border-radius: 6px; padding: 15px; margin: 20px 0;">
            <strong>Important:</strong> This link will expire in {{ expiration_minutes }} minutes.
            If you didn't request this, you can safely ignore this email.
        </div>
        <p>If the button doesn't work, copy and paste this link into your browser:</p>
        <p style="word-break: break-all; color: #2563eb;">{{ reset_link }}</p>
        <p>Best regards,<br/>The Team</p>
        {% endblock %}
        """;
    }

    private String generateNotificationTemplate() {
        return """
        {% extends "base.html" %}

        {% block title %}{{ title }}{% endblock %}

        {% block content %}
        <h2>{{ title }}</h2>
        <div style="background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; padding: 20px; margin: 20px 0;">
            <p>{{ message }}</p>
        </div>
        <p>Best regards,<br/>The Team</p>
        {% endblock %}
        """;
    }
}
