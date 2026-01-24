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
package com.jnzader.apigen.codegen.generator.php.mail;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates mail service code for PHP/Laravel applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class PhpMailServiceGenerator {

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

        files.put("app/Services/MailService.php", generateMailService());
        files.put("app/Mail/WelcomeMail.php", generateWelcomeMailable());
        files.put("app/Mail/PasswordResetMail.php", generatePasswordResetMailable());
        files.put("app/Mail/NotificationMail.php", generateNotificationMailable());

        if (generateWelcomeTemplate) {
            files.put("resources/views/emails/welcome.blade.php", generateWelcomeTemplate());
        }
        if (generatePasswordResetTemplate) {
            files.put(
                    "resources/views/emails/password-reset.blade.php",
                    generatePasswordResetTemplate());
        }
        if (generateNotificationTemplate) {
            files.put(
                    "resources/views/emails/notification.blade.php",
                    generateNotificationTemplate());
        }

        // Base layout
        files.put("resources/views/emails/layouts/base.blade.php", generateBaseLayout());

        return files;
    }

    private String generateMailService() {
        return """
        <?php

        namespace App\\Services;

        use App\\Mail\\WelcomeMail;
        use App\\Mail\\PasswordResetMail;
        use App\\Mail\\NotificationMail;
        use Illuminate\\Support\\Facades\\Mail;
        use Illuminate\\Support\\Facades\\Log;

        class MailService
        {
            /**
             * Send a simple text email.
             */
            public function sendSimpleEmail(string $to, string $subject, string $text): void
            {
                try {
                    Mail::raw($text, function ($message) use ($to, $subject) {
                        $message->to($to)->subject($subject);
                    });
                    Log::info("Simple email sent to: {$to}");
                } catch (\\Exception $e) {
                    Log::error("Failed to send email to: {$to}", ['error' => $e->getMessage()]);
                    throw $e;
                }
            }

            /**
             * Send a welcome email to a new user.
             */
            public function sendWelcomeEmail(string $to, string $userName): void
            {
                try {
                    Mail::to($to)->send(new WelcomeMail($userName));
                    Log::info("Welcome email sent to: {$to}");
                } catch (\\Exception $e) {
                    Log::error("Failed to send welcome email to: {$to}", ['error' => $e->getMessage()]);
                    throw $e;
                }
            }

            /**
             * Send a password reset email.
             */
            public function sendPasswordResetEmail(
                string $to,
                string $userName,
                string $resetLink,
                int $expirationMinutes = 30
            ): void {
                try {
                    Mail::to($to)->send(new PasswordResetMail($userName, $resetLink, $expirationMinutes));
                    Log::info("Password reset email sent to: {$to}");
                } catch (\\Exception $e) {
                    Log::error("Failed to send password reset email to: {$to}", ['error' => $e->getMessage()]);
                    throw $e;
                }
            }

            /**
             * Send a notification email.
             */
            public function sendNotificationEmail(string $to, string $title, string $message): void
            {
                try {
                    Mail::to($to)->send(new NotificationMail($title, $message));
                    Log::info("Notification email sent to: {$to}");
                } catch (\\Exception $e) {
                    Log::error("Failed to send notification email to: {$to}", ['error' => $e->getMessage()]);
                    throw $e;
                }
            }
        }
        """;
    }

    private String generateWelcomeMailable() {
        return """
        <?php

        namespace App\\Mail;

        use Illuminate\\Bus\\Queueable;
        use Illuminate\\Contracts\\Queue\\ShouldQueue;
        use Illuminate\\Mail\\Mailable;
        use Illuminate\\Mail\\Mailables\\Content;
        use Illuminate\\Mail\\Mailables\\Envelope;
        use Illuminate\\Queue\\SerializesModels;

        class WelcomeMail extends Mailable implements ShouldQueue
        {
            use Queueable, SerializesModels;

            public function __construct(
                public string $userName
            ) {}

            public function envelope(): Envelope
            {
                return new Envelope(
                    subject: 'Welcome to ' . config('app.name') . '!',
                );
            }

            public function content(): Content
            {
                return new Content(
                    view: 'emails.welcome',
                    with: [
                        'userName' => $this->userName,
                        'appName' => config('app.name'),
                        'loginUrl' => config('app.url') . '/login',
                    ],
                );
            }

            public function attachments(): array
            {
                return [];
            }
        }
        """;
    }

    private String generatePasswordResetMailable() {
        return """
        <?php

        namespace App\\Mail;

        use Illuminate\\Bus\\Queueable;
        use Illuminate\\Contracts\\Queue\\ShouldQueue;
        use Illuminate\\Mail\\Mailable;
        use Illuminate\\Mail\\Mailables\\Content;
        use Illuminate\\Mail\\Mailables\\Envelope;
        use Illuminate\\Queue\\SerializesModels;

        class PasswordResetMail extends Mailable implements ShouldQueue
        {
            use Queueable, SerializesModels;

            public function __construct(
                public string $userName,
                public string $resetLink,
                public int $expirationMinutes = 30
            ) {}

            public function envelope(): Envelope
            {
                return new Envelope(
                    subject: 'Password Reset Request',
                );
            }

            public function content(): Content
            {
                return new Content(
                    view: 'emails.password-reset',
                    with: [
                        'userName' => $this->userName,
                        'resetLink' => $this->resetLink,
                        'expirationMinutes' => $this->expirationMinutes,
                        'appName' => config('app.name'),
                    ],
                );
            }

            public function attachments(): array
            {
                return [];
            }
        }
        """;
    }

    private String generateNotificationMailable() {
        return """
        <?php

        namespace App\\Mail;

        use Illuminate\\Bus\\Queueable;
        use Illuminate\\Contracts\\Queue\\ShouldQueue;
        use Illuminate\\Mail\\Mailable;
        use Illuminate\\Mail\\Mailables\\Content;
        use Illuminate\\Mail\\Mailables\\Envelope;
        use Illuminate\\Queue\\SerializesModels;

        class NotificationMail extends Mailable implements ShouldQueue
        {
            use Queueable, SerializesModels;

            public function __construct(
                public string $title,
                public string $message
            ) {}

            public function envelope(): Envelope
            {
                return new Envelope(
                    subject: $this->title,
                );
            }

            public function content(): Content
            {
                return new Content(
                    view: 'emails.notification',
                    with: [
                        'title' => $this->title,
                        'message' => $this->message,
                        'appName' => config('app.name'),
                    ],
                );
            }

            public function attachments(): array
            {
                return [];
            }
        }
        """;
    }

    private String generateBaseLayout() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                .container { background: #fff; border-radius: 8px; padding: 40px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .header { text-align: center; margin-bottom: 30px; }
                .header h1 { color: #2563eb; }
                .button { display: inline-block; padding: 12px 24px; background: #2563eb; color: #fff; text-decoration: none; border-radius: 6px; }
                .button-danger { background: #dc2626; }
                .warning { background: #fef3c7; border: 1px solid #f59e0b; border-radius: 6px; padding: 15px; margin: 20px 0; }
                .notification { background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 6px; padding: 20px; margin: 20px 0; }
                .footer { text-align: center; color: #6b7280; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>{{ config('app.name') }}</h1></div>
                @yield('content')
                <div class="footer"><p>&copy; {{ date('Y') }} {{ config('app.name') }}. All rights reserved.</p></div>
            </div>
        </body>
        </html>
        """;
    }

    private String generateWelcomeTemplate() {
        return """
        @extends('emails.layouts.base')

        @section('content')
            <h2>Welcome, {{ $userName }}!</h2>
            <p>Thank you for joining {{ $appName }}. We're excited to have you on board!</p>
            <p>Your account has been successfully created and is ready to use.</p>
            <p style="text-align: center; margin: 30px 0;">
                <a href="{{ $loginUrl }}" class="button">Get Started</a>
            </p>
            <p>Best regards,<br/>The {{ $appName }} Team</p>
        @endsection
        """;
    }

    private String generatePasswordResetTemplate() {
        return """
        @extends('emails.layouts.base')

        @section('content')
            <h2>Password Reset Request</h2>
            <p>Hello {{ $userName }},</p>
            <p>We received a request to reset your password. Click the button below to create a new password:</p>
            <p style="text-align: center; margin: 30px 0;">
                <a href="{{ $resetLink }}" class="button button-danger">Reset Password</a>
            </p>
            <div class="warning">
                <strong>Important:</strong> This link will expire in {{ $expirationMinutes }} minutes.
                If you didn't request this, you can safely ignore this email.
            </div>
            <p>Best regards,<br/>The {{ $appName }} Team</p>
        @endsection
        """;
    }

    private String generateNotificationTemplate() {
        return """
        @extends('emails.layouts.base')

        @section('content')
            <h2>{{ $title }}</h2>
            <div class="notification">
                <p>{!! $message !!}</p>
            </div>
            <p>Best regards,<br/>The {{ $appName }} Team</p>
        @endsection
        """;
    }
}
