package com.jnzader.apigen.codegen.generator.php.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates PHP/Laravel JWT authentication functionality using Laravel Sanctum or JWT-Auth.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Auth controller with login, register, logout endpoints
 *   <li>User model with authentication traits
 *   <li>Auth request classes for validation
 *   <li>Auth middleware for protected routes
 *   <li>Token configuration
 * </ul>
 */
@SuppressWarnings({
    "java:S1192",
    "java:S3400"
}) // S1192: template strings; S3400: template methods return constants
public class PhpJwtAuthGenerator {

    private static final int DEFAULT_ACCESS_TOKEN_EXPIRATION = 60; // minutes
    private static final int DEFAULT_REFRESH_TOKEN_EXPIRATION = 10080; // 7 days in minutes

    /**
     * Generates all JWT authentication files.
     *
     * @param accessTokenExpiration access token expiration in minutes
     * @param refreshTokenExpiration refresh token expiration in minutes
     * @return map of file path to content
     */
    public Map<String, String> generate(int accessTokenExpiration, int refreshTokenExpiration) {
        Map<String, String> files = new LinkedHashMap<>();

        // Auth Controller
        files.put("app/Http/Controllers/Auth/AuthController.php", generateAuthController());

        // Request classes
        files.put("app/Http/Requests/Auth/LoginRequest.php", generateLoginRequest());
        files.put("app/Http/Requests/Auth/RegisterRequest.php", generateRegisterRequest());
        files.put("app/Http/Requests/Auth/RefreshTokenRequest.php", generateRefreshTokenRequest());
        files.put(
                "app/Http/Requests/Auth/ChangePasswordRequest.php",
                generateChangePasswordRequest());

        // Auth Service
        files.put("app/Services/AuthService.php", generateAuthService(accessTokenExpiration));

        // Auth Routes
        files.put("routes/auth.php", generateAuthRoutes());

        // Config
        files.put(
                "config/auth_tokens.php",
                generateTokenConfig(accessTokenExpiration, refreshTokenExpiration));

        return files;
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_ACCESS_TOKEN_EXPIRATION, DEFAULT_REFRESH_TOKEN_EXPIRATION);
    }

    private String generateAuthController() {
        return """
        <?php

        namespace App\\Http\\Controllers\\Auth;

        use App\\Http\\Controllers\\Controller;
        use App\\Http\\Requests\\Auth\\LoginRequest;
        use App\\Http\\Requests\\Auth\\RegisterRequest;
        use App\\Http\\Requests\\Auth\\RefreshTokenRequest;
        use App\\Http\\Requests\\Auth\\ChangePasswordRequest;
        use App\\Services\\AuthService;
        use Illuminate\\Http\\JsonResponse;
        use Illuminate\\Http\\Request;

        /**
         * Authentication controller.
         *
         * Handles user registration, login, logout, token refresh, and password changes.
         */
        class AuthController extends Controller
        {
            public function __construct(
                private readonly AuthService $authService
            ) {}

            /**
             * Register a new user.
             */
            public function register(RegisterRequest $request): JsonResponse
            {
                $result = $this->authService->register($request->validated());

                return response()->json([
                    'message' => 'User registered successfully',
                    'user' => $result['user'],
                    'access_token' => $result['access_token'],
                    'refresh_token' => $result['refresh_token'],
                    'token_type' => 'Bearer',
                ], 201);
            }

            /**
             * Login user and return tokens.
             */
            public function login(LoginRequest $request): JsonResponse
            {
                $result = $this->authService->login($request->validated());

                if (!$result) {
                    return response()->json([
                        'message' => 'Invalid credentials',
                    ], 401);
                }

                return response()->json([
                    'message' => 'Login successful',
                    'user' => $result['user'],
                    'access_token' => $result['access_token'],
                    'refresh_token' => $result['refresh_token'],
                    'token_type' => 'Bearer',
                ]);
            }

            /**
             * Logout user and revoke tokens.
             */
            public function logout(Request $request): JsonResponse
            {
                $this->authService->logout($request->user());

                return response()->json([
                    'message' => 'Successfully logged out',
                ]);
            }

            /**
             * Refresh access token.
             */
            public function refresh(RefreshTokenRequest $request): JsonResponse
            {
                $result = $this->authService->refreshToken($request->validated()['refresh_token']);

                if (!$result) {
                    return response()->json([
                        'message' => 'Invalid or expired refresh token',
                    ], 401);
                }

                return response()->json([
                    'access_token' => $result['access_token'],
                    'refresh_token' => $result['refresh_token'],
                    'token_type' => 'Bearer',
                ]);
            }

            /**
             * Get current authenticated user.
             */
            public function profile(Request $request): JsonResponse
            {
                return response()->json([
                    'user' => $request->user(),
                ]);
            }

            /**
             * Change user password.
             */
            public function changePassword(ChangePasswordRequest $request): JsonResponse
            {
                $result = $this->authService->changePassword(
                    $request->user(),
                    $request->validated()['current_password'],
                    $request->validated()['new_password']
                );

                if (!$result) {
                    return response()->json([
                        'message' => 'Current password is incorrect',
                    ], 400);
                }

                return response()->json([
                    'message' => 'Password changed successfully',
                ]);
            }
        }
        """;
    }

    private String generateLoginRequest() {
        return """
        <?php

        namespace App\\Http\\Requests\\Auth;

        use Illuminate\\Foundation\\Http\\FormRequest;

        /**
         * Login request validation.
         */
        class LoginRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'email' => ['required', 'string', 'email'],
                    'password' => ['required', 'string', 'min:8'],
                ];
            }

            public function messages(): array
            {
                return [
                    'email.required' => 'Email is required',
                    'email.email' => 'Please provide a valid email address',
                    'password.required' => 'Password is required',
                    'password.min' => 'Password must be at least 8 characters',
                ];
            }
        }
        """;
    }

    private String generateRegisterRequest() {
        return """
        <?php

        namespace App\\Http\\Requests\\Auth;

        use Illuminate\\Foundation\\Http\\FormRequest;
        use Illuminate\\Validation\\Rules\\Password;

        /**
         * Registration request validation.
         */
        class RegisterRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'name' => ['required', 'string', 'min:2', 'max:255'],
                    'email' => ['required', 'string', 'email', 'max:255', 'unique:users,email'],
                    'password' => [
                        'required',
                        'string',
                        'confirmed',
                        Password::min(8)
                            ->mixedCase()
                            ->numbers()
                            ->symbols(),
                    ],
                ];
            }

            public function messages(): array
            {
                return [
                    'name.required' => 'Name is required',
                    'name.min' => 'Name must be at least 2 characters',
                    'email.required' => 'Email is required',
                    'email.email' => 'Please provide a valid email address',
                    'email.unique' => 'This email is already registered',
                    'password.required' => 'Password is required',
                    'password.confirmed' => 'Password confirmation does not match',
                ];
            }
        }
        """;
    }

    private String generateRefreshTokenRequest() {
        return """
        <?php

        namespace App\\Http\\Requests\\Auth;

        use Illuminate\\Foundation\\Http\\FormRequest;

        /**
         * Refresh token request validation.
         */
        class RefreshTokenRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'refresh_token' => ['required', 'string'],
                ];
            }

            public function messages(): array
            {
                return [
                    'refresh_token.required' => 'Refresh token is required',
                ];
            }
        }
        """;
    }

    private String generateChangePasswordRequest() {
        return """
        <?php

        namespace App\\Http\\Requests\\Auth;

        use Illuminate\\Foundation\\Http\\FormRequest;
        use Illuminate\\Validation\\Rules\\Password;

        /**
         * Change password request validation.
         */
        class ChangePasswordRequest extends FormRequest
        {
            public function authorize(): bool
            {
                return true;
            }

            public function rules(): array
            {
                return [
                    'current_password' => ['required', 'string'],
                    'new_password' => [
                        'required',
                        'string',
                        'confirmed',
                        'different:current_password',
                        Password::min(8)
                            ->mixedCase()
                            ->numbers()
                            ->symbols(),
                    ],
                ];
            }

            public function messages(): array
            {
                return [
                    'current_password.required' => 'Current password is required',
                    'new_password.required' => 'New password is required',
                    'new_password.confirmed' => 'Password confirmation does not match',
                    'new_password.different' => 'New password must be different from current password',
                ];
            }
        }
        """;
    }

    private String generateAuthService(int accessTokenExpiration) {
        return """
        <?php

        namespace App\\Services;

        use App\\Models\\User;
        use Illuminate\\Support\\Facades\\Hash;
        use Illuminate\\Support\\Str;

        /**
         * Authentication service.
         *
         * Handles user authentication, token generation, and password management.
         */
        class AuthService
        {
            /**
             * Register a new user.
             *
             * @param array $data User registration data
             * @return array User and tokens
             */
            public function register(array $data): array
            {
                $user = User::create([
                    'name' => $data['name'],
                    'email' => $data['email'],
                    'password' => Hash::make($data['password']),
                ]);

                return $this->createTokensForUser($user);
            }

            /**
             * Login user.
             *
             * @param array $credentials Login credentials
             * @return array|null User and tokens or null if invalid
             */
            public function login(array $credentials): ?array
            {
                $user = User::where('email', $credentials['email'])->first();

                if (!$user || !Hash::check($credentials['password'], $user->password)) {
                    return null;
                }

                return $this->createTokensForUser($user);
            }

            /**
             * Logout user and revoke all tokens.
             *
             * @param User $user The user to logout
             */
            public function logout(User $user): void
            {
                $user->tokens()->delete();
            }

            /**
             * Refresh access token using refresh token.
             *
             * @param string $refreshToken The refresh token
             * @return array|null New tokens or null if invalid
             */
            public function refreshToken(string $refreshToken): ?array
            {
                $user = User::whereHas('tokens', function ($query) use ($refreshToken) {
                    $query->where('name', 'refresh_token')
                          ->where('token', hash('sha256', $refreshToken));
                })->first();

                if (!$user) {
                    return null;
                }

                // Revoke old tokens
                $user->tokens()->delete();

                return $this->createTokensForUser($user);
            }

            /**
             * Change user password.
             *
             * @param User $user The user
             * @param string $currentPassword Current password
             * @param string $newPassword New password
             * @return bool Success status
             */
            public function changePassword(User $user, string $currentPassword, string $newPassword): bool
            {
                if (!Hash::check($currentPassword, $user->password)) {
                    return false;
                }

                $user->update([
                    'password' => Hash::make($newPassword),
                ]);

                // Revoke all tokens after password change
                $user->tokens()->delete();

                return true;
            }

            /**
             * Create access and refresh tokens for user.
             *
             * @param User $user The user
             * @return array User and tokens
             */
            private function createTokensForUser(User $user): array
            {
                $accessToken = $user->createToken('access_token', ['*'], now()->addMinutes(%d));
                $refreshToken = $user->createToken('refresh_token', ['refresh'], now()->addMinutes(config('auth_tokens.refresh_expiration')));

                return [
                    'user' => $user,
                    'access_token' => $accessToken->plainTextToken,
                    'refresh_token' => $refreshToken->plainTextToken,
                ];
            }
        }
        """
                .formatted(accessTokenExpiration);
    }

    private String generateAuthRoutes() {
        return """
        <?php

        use App\\Http\\Controllers\\Auth\\AuthController;
        use Illuminate\\Support\\Facades\\Route;

        /*
        |--------------------------------------------------------------------------
        | Authentication Routes
        |--------------------------------------------------------------------------
        |
        | Routes for user authentication including registration, login, logout,
        | token refresh, and password management.
        |
        */

        Route::prefix('auth')->group(function () {
            // Public routes
            Route::post('/register', [AuthController::class, 'register']);
            Route::post('/login', [AuthController::class, 'login']);
            Route::post('/refresh', [AuthController::class, 'refresh']);

            // Protected routes (require authentication)
            Route::middleware('auth:sanctum')->group(function () {
                Route::post('/logout', [AuthController::class, 'logout']);
                Route::get('/profile', [AuthController::class, 'profile']);
                Route::put('/password', [AuthController::class, 'changePassword']);
            });
        });
        """;
    }

    private String generateTokenConfig(int accessTokenExpiration, int refreshTokenExpiration) {
        return """
        <?php

        return [
            /*
            |--------------------------------------------------------------------------
            | Token Expiration Settings
            |--------------------------------------------------------------------------
            |
            | Here you can configure the expiration times for access and refresh tokens.
            | Values are in minutes.
            |
            */

            'access_expiration' => env('JWT_ACCESS_EXPIRATION', %d),

            'refresh_expiration' => env('JWT_REFRESH_EXPIRATION', %d),

            /*
            |--------------------------------------------------------------------------
            | Token Abilities
            |--------------------------------------------------------------------------
            |
            | Define the default abilities for tokens.
            |
            */

            'abilities' => [
                'access' => ['*'],
                'refresh' => ['refresh'],
            ],
        ];
        """
                .formatted(accessTokenExpiration, refreshTokenExpiration);
    }
}
