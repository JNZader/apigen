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
package com.jnzader.apigen.codegen.generator.php.security.reset;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates password reset flow for PHP/Laravel applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class PhpPasswordResetGenerator {

    /**
     * Generates password reset files.
     *
     * @param tokenExpirationMinutes token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int tokenExpirationMinutes) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("app/Http/Controllers/Api/V1/PasswordResetController.php", generateController());
        files.put("app/Services/PasswordResetService.php", generateService(tokenExpirationMinutes));
        files.put("app/Http/Requests/ForgotPasswordRequest.php", generateForgotPasswordRequest());
        files.put("app/Http/Requests/ResetPasswordRequest.php", generateResetPasswordRequest());
        files.put("app/Http/Requests/ValidateTokenRequest.php", generateValidateTokenRequest());
        files.put(
                "database/migrations/0001_01_01_000003_create_password_reset_tokens_table.php",
                generateMigration());

        return files;
    }

    private String generateController() {
        return """
        <?php

        namespace App\\Http\\Controllers\\Api\\V1;

        use App\\Http\\Controllers\\Controller;
        use App\\Http\\Requests\\ForgotPasswordRequest;
        use App\\Http\\Requests\\ResetPasswordRequest;
        use App\\Http\\Requests\\ValidateTokenRequest;
        use App\\Services\\PasswordResetService;
        use Illuminate\\Http\\JsonResponse;
        use Illuminate\\Http\\Request;

        /**
         * @OA\\Tag(
         *     name="Password Reset",
         *     description="Password reset management endpoints"
         * )
         */
        class PasswordResetController extends Controller
        {
            public function __construct(
                private PasswordResetService $passwordResetService
            ) {}

            /**
             * @OA\\Post(
             *     path="/api/auth/password/forgot",
             *     tags={"Password Reset"},
             *     summary="Request password reset email",
             *     @OA\\RequestBody(
             *         required=true,
             *         @OA\\JsonContent(
             *             required={"email"},
             *             @OA\\Property(property="email", type="string", format="email")
             *         )
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="Password reset email sent (if email exists)",
             *         @OA\\JsonContent(
             *             @OA\\Property(property="message", type="string")
             *         )
             *     )
             * )
             */
            public function forgotPassword(ForgotPasswordRequest $request): JsonResponse
            {
                $baseUrl = $request->getSchemeAndHttpHost();
                $this->passwordResetService->requestPasswordReset($request->email, $baseUrl);

                return response()->json([
                    'message' => 'If the email exists, a password reset link has been sent.',
                ]);
            }

            /**
             * @OA\\Post(
             *     path="/api/auth/password/validate",
             *     tags={"Password Reset"},
             *     summary="Validate reset token",
             *     @OA\\RequestBody(
             *         required=true,
             *         @OA\\JsonContent(
             *             required={"token"},
             *             @OA\\Property(property="token", type="string")
             *         )
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="Token validation result",
             *         @OA\\JsonContent(
             *             @OA\\Property(property="valid", type="boolean"),
             *             @OA\\Property(property="message", type="string", nullable=true)
             *         )
             *     )
             * )
             */
            public function validateToken(ValidateTokenRequest $request): JsonResponse
            {
                $result = $this->passwordResetService->validateToken($request->token);

                return response()->json($result);
            }

            /**
             * @OA\\Post(
             *     path="/api/auth/password/reset",
             *     tags={"Password Reset"},
             *     summary="Reset password with token",
             *     @OA\\RequestBody(
             *         required=true,
             *         @OA\\JsonContent(
             *             required={"token", "password", "password_confirmation"},
             *             @OA\\Property(property="token", type="string"),
             *             @OA\\Property(property="password", type="string", minLength=8),
             *             @OA\\Property(property="password_confirmation", type="string", minLength=8)
             *         )
             *     ),
             *     @OA\\Response(
             *         response=200,
             *         description="Password reset successful",
             *         @OA\\JsonContent(
             *             @OA\\Property(property="success", type="boolean"),
             *             @OA\\Property(property="message", type="string")
             *         )
             *     ),
             *     @OA\\Response(response=400, description="Invalid or expired token")
             * )
             */
            public function resetPassword(ResetPasswordRequest $request): JsonResponse
            {
                $result = $this->passwordResetService->resetPassword(
                    $request->token,
                    $request->password
                );

                return response()->json($result);
            }
        }
        """;
    }

    private String generateService(int tokenExpirationMinutes) {
        return String.format(
                """
                <?php

                namespace App\\Services;

                use App\\Models\\User;
                use Illuminate\\Support\\Facades\\DB;
                use Illuminate\\Support\\Facades\\Hash;
                use Illuminate\\Support\\Facades\\Log;
                use Illuminate\\Support\\Str;
                use Symfony\\Component\\HttpKernel\\Exception\\BadRequestHttpException;

                class PasswordResetService
                {
                    private const TOKEN_EXPIRATION_MINUTES = %d;

                    /**
                     * Request a password reset.
                     */
                    public function requestPasswordReset(string $email, string $baseUrl): bool
                    {
                        $user = User::where('email', $email)->first();

                        if (!$user) {
                            // Return true to prevent email enumeration
                            return true;
                        }

                        // Invalidate existing tokens
                        DB::table('password_reset_tokens')
                            ->where('email', $email)
                            ->delete();

                        // Generate new token
                        $token = Str::random(64);
                        $expiresAt = now()->addMinutes(self::TOKEN_EXPIRATION_MINUTES);

                        DB::table('password_reset_tokens')->insert([
                            'email' => $email,
                            'token' => Hash::make($token),
                            'expires_at' => $expiresAt,
                            'created_at' => now(),
                        ]);

                        // Send email
                        $resetLink = "{$baseUrl}/reset-password?token={$token}&email=" . urlencode($email);
                        app(MailService::class)->sendPasswordResetEmail(
                            $email,
                            $user->name ?? $user->email,
                            $resetLink,
                            self::TOKEN_EXPIRATION_MINUTES
                        );

                        Log::info("Password reset email sent to: {$email}");
                        return true;
                    }

                    /**
                     * Validate a reset token.
                     */
                    public function validateToken(string $token): array
                    {
                        $resetRecord = DB::table('password_reset_tokens')
                            ->where('expires_at', '>', now())
                            ->first();

                        if (!$resetRecord) {
                            return ['valid' => false, 'message' => 'Invalid or expired token'];
                        }

                        // We need to check all records since token is hashed
                        $records = DB::table('password_reset_tokens')
                            ->where('expires_at', '>', now())
                            ->get();

                        foreach ($records as $record) {
                            if (Hash::check($token, $record->token)) {
                                return ['valid' => true];
                            }
                        }

                        return ['valid' => false, 'message' => 'Invalid or expired token'];
                    }

                    /**
                     * Reset password with token.
                     */
                    public function resetPassword(string $token, string $newPassword): array
                    {
                        $records = DB::table('password_reset_tokens')
                            ->where('expires_at', '>', now())
                            ->get();

                        $validRecord = null;
                        foreach ($records as $record) {
                            if (Hash::check($token, $record->token)) {
                                $validRecord = $record;
                                break;
                            }
                        }

                        if (!$validRecord) {
                            throw new BadRequestHttpException('Invalid or expired token');
                        }

                        $user = User::where('email', $validRecord->email)->first();
                        if (!$user) {
                            throw new BadRequestHttpException('User not found');
                        }

                        // Update password
                        $user->password = Hash::make($newPassword);
                        $user->save();

                        // Delete used token
                        DB::table('password_reset_tokens')
                            ->where('email', $validRecord->email)
                            ->delete();

                        Log::info("Password reset for user: {$user->email}");
                        return ['success' => true, 'message' => 'Password has been reset successfully'];
                    }

                    /**
                     * Cleanup expired tokens.
                     */
                    public function cleanupExpiredTokens(): int
                    {
                        $deleted = DB::table('password_reset_tokens')
                            ->where('expires_at', '<', now())
                            ->delete();

                        Log::info("Cleaned up {$deleted} expired password reset tokens");
                        return $deleted;
                    }
                }
                """,
                tokenExpirationMinutes);
    }

    private String generateForgotPasswordRequest() {
        return """
        <?php

        namespace App\\Http\\Requests;

        use Illuminate\\Foundation\\Http\\FormRequest;

        class ForgotPasswordRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'email' => ['required', 'email'],
                ];
            }
        }
        """;
    }

    private String generateResetPasswordRequest() {
        return """
        <?php

        namespace App\\Http\\Requests;

        use Illuminate\\Foundation\\Http\\FormRequest;

        class ResetPasswordRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'token' => ['required', 'string'],
                    'password' => ['required', 'string', 'min:8', 'confirmed'],
                ];
            }
        }
        """;
    }

    private String generateValidateTokenRequest() {
        return """
        <?php

        namespace App\\Http\\Requests;

        use Illuminate\\Foundation\\Http\\FormRequest;

        class ValidateTokenRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'token' => ['required', 'string'],
                ];
            }
        }
        """;
    }

    private String generateMigration() {
        return """
        <?php

        use Illuminate\\Database\\Migrations\\Migration;
        use Illuminate\\Database\\Schema\\Blueprint;
        use Illuminate\\Support\\Facades\\Schema;

        return new class extends Migration
        {
            public function up(): void
            {
                Schema::create('password_reset_tokens', function (Blueprint $table) {
                    $table->string('email')->primary();
                    $table->string('token');
                    $table->timestamp('expires_at');
                    $table->timestamp('created_at')->nullable();
                });
            }

            public function down(): void
            {
                Schema::dropIfExists('password_reset_tokens');
            }
        };
        """;
    }
}
