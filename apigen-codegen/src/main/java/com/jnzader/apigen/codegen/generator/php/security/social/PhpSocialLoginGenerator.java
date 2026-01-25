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
package com.jnzader.apigen.codegen.generator.php.security.social;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates social login (OAuth2) code for PHP/Laravel applications using Socialite.
 *
 * @author APiGen
 * @since 2.13.0
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400",
    "java:S3457"
}) // S1192: template strings; S3400: template methods; S3457: Unix line endings
public class PhpSocialLoginGenerator {

    /**
     * Generates social login files.
     *
     * @param providers list of OAuth2 providers (google, github, linkedin, etc.)
     * @return map of file path to content
     */
    public Map<String, String> generate(List<String> providers) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put(
                "app/Http/Controllers/Api/V1/SocialAuthController.php",
                generateController(providers));
        files.put("app/Services/SocialAuthService.php", generateService());
        files.put("config/services_social.php", generateServicesConfig(providers));
        files.put("routes/socialite.php", generateRoutes(providers));

        return files;
    }

    private String generateController(List<String> providers) {
        StringBuilder providerChecks = new StringBuilder();
        for (String provider : providers) {
            providerChecks.append(String.format("        '%s',\n", provider));
        }

        return String.format(
                """
                <?php

                namespace App\\Http\\Controllers\\Api\\V1;

                use App\\Http\\Controllers\\Controller;
                use App\\Services\\SocialAuthService;
                use Illuminate\\Http\\JsonResponse;
                use Illuminate\\Http\\RedirectResponse;
                use Laravel\\Socialite\\Facades\\Socialite;
                use Symfony\\Component\\HttpKernel\\Exception\\BadRequestHttpException;

                /**
                 * @OA\\Tag(
                 *     name="Social Authentication",
                 *     description="OAuth2 social login endpoints"
                 * )
                 */
                class SocialAuthController extends Controller
                {
                    private const SUPPORTED_PROVIDERS = [
                %s    ];

                    public function __construct(
                        private SocialAuthService $socialAuthService
                    ) {}

                    /**
                     * @OA\\Get(
                     *     path="/api/auth/social/providers",
                     *     tags={"Social Authentication"},
                     *     summary="List supported OAuth providers",
                     *     @OA\\Response(
                     *         response=200,
                     *         description="List of supported providers",
                     *         @OA\\JsonContent(
                     *             @OA\\Property(
                     *                 property="providers",
                     *                 type="array",
                     *                 @OA\\Items(type="string")
                     *             )
                     *         )
                     *     )
                     * )
                     */
                    public function providers(): JsonResponse
                    {
                        return response()->json(['providers' => self::SUPPORTED_PROVIDERS]);
                    }

                    /**
                     * Redirect to OAuth provider.
                     */
                    public function redirect(string $provider): RedirectResponse
                    {
                        $this->validateProvider($provider);

                        return Socialite::driver($provider)->redirect();
                    }

                    /**
                     * Handle OAuth callback.
                     */
                    public function callback(string $provider): JsonResponse
                    {
                        $this->validateProvider($provider);

                        try {
                            $socialUser = Socialite::driver($provider)->user();
                            $result = $this->socialAuthService->handleSocialUser($provider, $socialUser);

                            return response()->json($result);
                        } catch (\\Exception $e) {
                            throw new BadRequestHttpException("Failed to authenticate with {$provider}: " . $e->getMessage());
                        }
                    }

                    /**
                     * Handle OAuth callback for stateless (API) flow.
                     */
                    public function callbackStateless(string $provider): JsonResponse
                    {
                        $this->validateProvider($provider);

                        try {
                            $socialUser = Socialite::driver($provider)->stateless()->user();
                            $result = $this->socialAuthService->handleSocialUser($provider, $socialUser);

                            return response()->json($result);
                        } catch (\\Exception $e) {
                            throw new BadRequestHttpException("Failed to authenticate with {$provider}: " . $e->getMessage());
                        }
                    }

                    private function validateProvider(string $provider): void
                    {
                        if (!in_array($provider, self::SUPPORTED_PROVIDERS)) {
                            throw new BadRequestHttpException("Unsupported OAuth provider: {$provider}");
                        }
                    }
                }
                """,
                providerChecks);
    }

    private String generateService() {
        return """
        <?php

        namespace App\\Services;

        use App\\Models\\User;
        use Illuminate\\Support\\Facades\\Hash;
        use Illuminate\\Support\\Facades\\Log;
        use Illuminate\\Support\\Str;
        use Laravel\\Socialite\\Contracts\\User as SocialiteUser;
        use Tymon\\JWTAuth\\Facades\\JWTAuth;

        class SocialAuthService
        {
            /**
             * Handle social user login/registration.
             */
            public function handleSocialUser(string $provider, SocialiteUser $socialUser): array
            {
                $providerIdField = $provider . '_id';
                $isNewUser = false;

                // Try to find existing user by provider ID or email
                $user = User::where($providerIdField, $socialUser->getId())
                    ->orWhere('email', $socialUser->getEmail())
                    ->first();

                if (!$user) {
                    // Create new user
                    $user = User::create([
                        'name' => $socialUser->getName() ?? $socialUser->getNickname() ?? 'User',
                        'email' => $socialUser->getEmail(),
                        'password' => Hash::make(Str::random(32)),
                        'email_verified_at' => now(), // Social login users are auto-verified
                        $providerIdField => $socialUser->getId(),
                        'avatar' => $socialUser->getAvatar(),
                    ]);
                    $isNewUser = true;
                    Log::info("New user created via {$provider}: {$socialUser->getEmail()}");
                } else {
                    // Update provider ID if not set
                    if (!$user->{$providerIdField}) {
                        $user->{$providerIdField} = $socialUser->getId();
                        $user->save();
                    }
                }

                // Generate JWT tokens
                $accessToken = JWTAuth::fromUser($user);
                $refreshToken = JWTAuth::claims(['refresh' => true])->fromUser($user);

                return [
                    'access_token' => $accessToken,
                    'refresh_token' => $refreshToken,
                    'token_type' => 'bearer',
                    'user_id' => $user->id,
                    'email' => $user->email,
                    'is_new_user' => $isNewUser,
                ];
            }
        }
        """;
    }

    private String generateServicesConfig(List<String> providers) {
        StringBuilder config = new StringBuilder();
        config.append("<?php\n\n");
        config.append("/**\n");
        config.append(" * Social login configuration.\n");
        config.append(" * Add these to your config/services.php file.\n");
        config.append(" */\n\n");
        config.append("return [\n");

        for (String provider : providers) {
            String upperProvider = provider.toUpperCase();
            config.append(
                    String.format(
                            """
                                '%s' => [
                                    'client_id' => env('%s_CLIENT_ID'),
                                    'client_secret' => env('%s_CLIENT_SECRET'),
                                    'redirect' => env('%s_REDIRECT_URI', '/api/auth/social/%s/callback'),
                                ],

                            """,
                            provider, upperProvider, upperProvider, upperProvider, provider));
        }

        config.append("];\n");
        return config.toString();
    }

    private String generateRoutes(List<String> providers) {
        StringBuilder routes = new StringBuilder();
        routes.append(
                """
                <?php

                use App\\Http\\Controllers\\Api\\V1\\SocialAuthController;
                use Illuminate\\Support\\Facades\\Route;

                /*
                |--------------------------------------------------------------------------
                | Social Authentication Routes
                |--------------------------------------------------------------------------
                |
                | Include this file in your routes/api.php:
                | require __DIR__ . '/socialite.php';
                |
                */

                Route::prefix('auth/social')->group(function () {
                    Route::get('providers', [SocialAuthController::class, 'providers']);

                """);

        for (String provider : providers) {
            routes.append(
                    String.format(
                            """
                                // %s OAuth routes
                                Route::get('%s', [SocialAuthController::class, 'redirect'])
                                    ->defaults('provider', '%s')
                                    ->name('social.%s');
                                Route::get('%s/callback', [SocialAuthController::class, 'callback'])
                                    ->defaults('provider', '%s')
                                    ->name('social.%s.callback');

                            """,
                            capitalize(provider),
                            provider,
                            provider,
                            provider,
                            provider,
                            provider,
                            provider));
        }

        routes.append("});\n");
        return routes.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
