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
package com.jnzader.apigen.codegen.generator.typescript.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for TypeScript/NestJS applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class TypeScriptMailServiceGenerator {

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

        files.put("src/mail/mail.module.ts", generateModule());
        files.put("src/mail/mail.service.ts", generateService());
        files.put("src/mail/dto/email.dto.ts", generateDto());

        if (generateWelcomeTemplate) {
            files.put("src/mail/templates/welcome.hbs", generateWelcomeTemplate());
        }
        if (generatePasswordResetTemplate) {
            files.put("src/mail/templates/password-reset.hbs", generatePasswordResetTemplate());
        }
        if (generateNotificationTemplate) {
            files.put("src/mail/templates/notification.hbs", generateNotificationTemplate());
        }

        return files;
    }

    private String generateModule() {
        return """
        import { Module } from '@nestjs/common';
        import { MailerModule } from '@nestjs-modules/mailer';
        import { HandlebarsAdapter } from '@nestjs-modules/mailer/dist/adapters/handlebars.adapter';
        import { ConfigModule, ConfigService } from '@nestjs/config';
        import { join } from 'path';
        import { MailService } from './mail.service';

        @Module({
          imports: [
            MailerModule.forRootAsync({
              imports: [ConfigModule],
              useFactory: async (configService: ConfigService) => ({
                transport: {
                  host: configService.get('MAIL_HOST', 'smtp.example.com'),
                  port: configService.get('MAIL_PORT', 587),
                  secure: configService.get('MAIL_SECURE', 'false') === 'true',
                  auth: {
                    user: configService.get('MAIL_USER'),
                    pass: configService.get('MAIL_PASSWORD'),
                  },
                },
                defaults: {
                  from: `"${configService.get('MAIL_FROM_NAME', 'Application')}" <${configService.get('MAIL_FROM', 'noreply@example.com')}>`,
                },
                template: {
                  dir: join(__dirname, 'templates'),
                  adapter: new HandlebarsAdapter(),
                  options: {
                    strict: true,
                  },
                },
              }),
              inject: [ConfigService],
            }),
          ],
          providers: [MailService],
          exports: [MailService],
        })
        export class MailModule {}
        """;
    }

    private String generateService() {
        return """
        import { Injectable, Logger } from '@nestjs/common';
        import { MailerService } from '@nestjs-modules/mailer';
        import { ConfigService } from '@nestjs/config';
        import { EmailDto, SendEmailDto } from './dto/email.dto';

        @Injectable()
        export class MailService {
          private readonly logger = new Logger(MailService.name);
          private readonly appName: string;

          constructor(
            private readonly mailerService: MailerService,
            private readonly configService: ConfigService,
          ) {
            this.appName = this.configService.get('APP_NAME', 'Application');
          }

          async sendSimpleEmail(to: string, subject: string, text: string): Promise<void> {
            try {
              await this.mailerService.sendMail({
                to,
                subject,
                text,
              });
              this.logger.log(`Simple email sent to: ${to}`);
            } catch (error) {
              this.logger.error(`Failed to send email to: ${to}`, error);
              throw error;
            }
          }

          async sendHtmlEmail(
            to: string,
            subject: string,
            template: string,
            context: Record<string, any>,
          ): Promise<void> {
            try {
              await this.mailerService.sendMail({
                to,
                subject,
                template,
                context: {
                  ...context,
                  appName: this.appName,
                },
              });
              this.logger.log(`HTML email sent to: ${to} using template: ${template}`);
            } catch (error) {
              this.logger.error(`Failed to send HTML email to: ${to}`, error);
              throw error;
            }
          }

          async send(emailDto: SendEmailDto): Promise<void> {
            if (emailDto.template) {
              await this.sendHtmlEmail(
                emailDto.to,
                emailDto.subject,
                emailDto.template,
                emailDto.context || {},
              );
            } else {
              await this.sendSimpleEmail(
                emailDto.to,
                emailDto.subject,
                emailDto.body || '',
              );
            }
          }

          async sendWelcomeEmail(to: string, userName: string): Promise<void> {
            await this.sendHtmlEmail(to, `Welcome to ${this.appName}!`, 'welcome', {
              userName,
              loginUrl: '/login',
            });
          }

          async sendPasswordResetEmail(
            to: string,
            userName: string,
            resetLink: string,
            expirationMinutes: number = 30,
          ): Promise<void> {
            await this.sendHtmlEmail(to, 'Password Reset Request', 'password-reset', {
              userName,
              resetLink,
              expirationMinutes,
            });
          }

          async sendNotificationEmail(
            to: string,
            title: string,
            message: string,
          ): Promise<void> {
            await this.sendHtmlEmail(to, title, 'notification', {
              title,
              message,
            });
          }
        }
        """;
    }

    private String generateDto() {
        return """
        import { IsEmail, IsNotEmpty, IsOptional, IsString } from 'class-validator';
        import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

        export class EmailDto {
          @ApiProperty({ description: 'Recipient email address' })
          @IsEmail()
          @IsNotEmpty()
          to: string;

          @ApiProperty({ description: 'Email subject' })
          @IsString()
          @IsNotEmpty()
          subject: string;

          @ApiPropertyOptional({ description: 'Plain text body' })
          @IsString()
          @IsOptional()
          body?: string;

          @ApiPropertyOptional({ description: 'Template name' })
          @IsString()
          @IsOptional()
          template?: string;

          @ApiPropertyOptional({ description: 'Template context variables' })
          @IsOptional()
          context?: Record<string, any>;
        }

        export class SendEmailDto extends EmailDto {}

        export class SendEmailResponseDto {
          @ApiProperty({ description: 'Whether the email was sent successfully' })
          success: boolean;

          @ApiProperty({ description: 'Response message' })
          message: string;
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
                <div class="header"><h1>{{appName}}</h1></div>
                <h2>Welcome, {{userName}}!</h2>
                <p>Thank you for joining {{appName}}. We're excited to have you on board!</p>
                <p>Your account has been successfully created and is ready to use.</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="{{loginUrl}}" class="button">Get Started</a>
                </p>
                <p>Best regards,<br/>The {{appName}} Team</p>
                <div class="footer"><p>&copy; {{appName}}. All rights reserved.</p></div>
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
                <div class="header"><h1>{{appName}}</h1></div>
                <h2>Password Reset Request</h2>
                <p>Hello {{userName}},</p>
                <p>We received a request to reset your password. Click the button below to create a new password:</p>
                <p style="text-align: center; margin: 30px 0;">
                    <a href="{{resetLink}}" class="button">Reset Password</a>
                </p>
                <div class="warning">
                    <strong>Important:</strong> This link will expire in {{expirationMinutes}} minutes.
                    If you didn't request this, you can safely ignore this email.
                </div>
                <p>Best regards,<br/>The {{appName}} Team</p>
                <div class="footer"><p>&copy; {{appName}}. All rights reserved.</p></div>
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
                <div class="header"><h1>{{appName}}</h1></div>
                <h2>{{title}}</h2>
                <div class="notification"><p>{{{message}}}</p></div>
                <p>Best regards,<br/>The {{appName}} Team</p>
                <div class="footer"><p>&copy; {{appName}}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }
}
