package com.jnzader.apigen.codegen.generator.python.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Python/FastAPI rate limiting functionality using slowapi.
 *
 * <p>This generator creates:
 *
 * <ul>
 *   <li>Rate limiter configuration with slowapi
 *   <li>Rate limit decorators for routes
 *   <li>Custom key functions for user-based limiting
 *   <li>Redis backend support (optional)
 * </ul>
 */
public class PythonRateLimitGenerator {

    private static final String DEFAULT_LIMIT = "100/minute";
    private static final boolean DEFAULT_USE_REDIS = false;

    /**
     * Generates all rate limiting files.
     *
     * @param defaultLimit default rate limit (e.g., "100/minute")
     * @param useRedis whether to use Redis backend
     * @return map of file path to content
     */
    public Map<String, String> generate(String defaultLimit, boolean useRedis) {
        Map<String, String> files = new LinkedHashMap<>();

        // Rate limit module
        files.put("app/core/rate_limit.py", generateRateLimitModule(defaultLimit, useRedis));
        files.put("app/middleware/__init__.py", "");
        files.put("app/middleware/rate_limit.py", generateRateLimitMiddleware());

        return files;
    }

    /** Generates with default settings. */
    public Map<String, String> generate() {
        return generate(DEFAULT_LIMIT, DEFAULT_USE_REDIS);
    }

    private String generateRateLimitModule(String defaultLimit, boolean useRedis) {
        String storageSetup =
                useRedis
                        ? """
                        # Redis backend for distributed rate limiting
                        from slowapi.middleware import SlowAPIASGIMiddleware
                        import redis.asyncio as redis

                        redis_client = redis.from_url(settings.REDIS_URL, decode_responses=True)
                        limiter = Limiter(
                            key_func=get_remote_address,
                            storage_uri=settings.REDIS_URL,
                            default_limits=["%s"],
                        )
                        """
                                .formatted(defaultLimit)
                        : """
                        # In-memory backend (single instance only)
                        limiter = Limiter(
                            key_func=get_remote_address,
                            default_limits=["%s"],
                        )
                        """
                                .formatted(defaultLimit);

        return """
        from typing import Callable, Optional

        from fastapi import Request, Response
        from slowapi import Limiter
        from slowapi.errors import RateLimitExceeded
        from slowapi.util import get_remote_address

        from app.core.config import settings


        %s


        def get_user_identifier(request: Request) -> str:
            \"\"\"
            Get rate limit key based on user identity.

            Uses user ID if authenticated, otherwise falls back to IP address.
            This provides per-user rate limiting for authenticated users.

            Args:
                request: FastAPI request object

            Returns:
                User identifier string for rate limiting
            \"\"\"
            # Check for authenticated user in request state
            if hasattr(request.state, "user") and request.state.user:
                return f"user:{request.state.user.id}"

            # Fall back to IP address
            return get_remote_address(request)


        def get_api_key_identifier(request: Request) -> str:
            \"\"\"
            Get rate limit key based on API key.

            Uses API key if present in header, otherwise falls back to IP.

            Args:
                request: FastAPI request object

            Returns:
                API key or IP identifier for rate limiting
            \"\"\"
            api_key = request.headers.get("X-API-Key")
            if api_key:
                return f"api_key:{api_key}"
            return get_remote_address(request)


        def create_dynamic_limit(
            requests_per_minute: int = 100,
            key_func: Optional[Callable] = None,
        ) -> str:
            \"\"\"
            Create a dynamic rate limit string.

            Args:
                requests_per_minute: Number of requests allowed per minute
                key_func: Optional custom key function

            Returns:
                Rate limit string for slowapi decorator
            \"\"\"
            return f"{requests_per_minute}/minute"


        # Predefined rate limits for different endpoint types
        class RateLimits:
            \"\"\"Predefined rate limit configurations.\"\"\"

            # Standard API endpoints
            STANDARD = "%s"

            # Heavy endpoints (reports, exports, etc.)
            HEAVY = "10/minute"

            # Authentication endpoints (prevent brute force)
            AUTH = "5/minute"

            # Public endpoints (more restrictive)
            PUBLIC = "30/minute"

            # Admin endpoints (more permissive)
            ADMIN = "200/minute"

            # Burst-friendly endpoints
            BURST = "1000/minute"


        def rate_limit_exceeded_handler(request: Request, exc: RateLimitExceeded) -> Response:
            \"\"\"
            Custom handler for rate limit exceeded errors.

            Args:
                request: FastAPI request object
                exc: Rate limit exceeded exception

            Returns:
                JSON response with rate limit error details
            \"\"\"
            from fastapi.responses import JSONResponse

            response = JSONResponse(
                status_code=429,
                content={
                    "error": "rate_limit_exceeded",
                    "message": f"Rate limit exceeded: {exc.detail}",
                    "retry_after": getattr(exc, "retry_after", None),
                },
            )

            # Add standard rate limit headers
            response.headers["Retry-After"] = str(getattr(exc, "retry_after", 60))
            response.headers["X-RateLimit-Limit"] = exc.detail.split()[0] if exc.detail else "unknown"

            return response
        """
                .formatted(storageSetup, defaultLimit);
    }

    private String generateRateLimitMiddleware() {
        return """
        from fastapi import FastAPI, Request
        from slowapi import _rate_limit_exceeded_handler
        from slowapi.errors import RateLimitExceeded
        from starlette.middleware.base import BaseHTTPMiddleware

        from app.core.rate_limit import limiter, rate_limit_exceeded_handler


        def setup_rate_limiting(app: FastAPI) -> None:
            \"\"\"
            Setup rate limiting for the FastAPI application.

            This function:
            1. Attaches the limiter to app state
            2. Adds the rate limit exceeded exception handler
            3. Adds middleware for request tracking

            Args:
                app: FastAPI application instance
            \"\"\"
            # Attach limiter to app state
            app.state.limiter = limiter

            # Add custom exception handler for rate limits
            app.add_exception_handler(RateLimitExceeded, rate_limit_exceeded_handler)


        class RateLimitHeaderMiddleware(BaseHTTPMiddleware):
            \"\"\"
            Middleware to add rate limit headers to responses.

            Adds the following headers:
            - X-RateLimit-Limit: The rate limit ceiling
            - X-RateLimit-Remaining: Number of requests remaining
            - X-RateLimit-Reset: Time when the rate limit resets
            \"\"\"

            async def dispatch(self, request: Request, call_next):
                response = await call_next(request)

                # Add rate limit headers if available
                if hasattr(request.state, "view_rate_limit"):
                    limit_info = request.state.view_rate_limit

                    response.headers["X-RateLimit-Limit"] = str(
                        getattr(limit_info, "limit", "unknown")
                    )
                    response.headers["X-RateLimit-Remaining"] = str(
                        getattr(limit_info, "remaining", "unknown")
                    )
                    response.headers["X-RateLimit-Reset"] = str(
                        getattr(limit_info, "reset", "unknown")
                    )

                return response


        # Example usage in routes:
        #
        # from app.core.rate_limit import limiter, RateLimits
        #
        # @router.get("/items")
        # @limiter.limit(RateLimits.STANDARD)
        # async def list_items(request: Request):
        #     ...
        #
        # @router.post("/auth/login")
        # @limiter.limit(RateLimits.AUTH)
        # async def login(request: Request):
        #     ...
        """;
    }
}
