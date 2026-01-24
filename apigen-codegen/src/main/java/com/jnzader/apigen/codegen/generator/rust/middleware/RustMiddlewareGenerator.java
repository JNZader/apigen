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
package com.jnzader.apigen.codegen.generator.rust.middleware;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;

/**
 * Generates Rust middleware implementations including JWT authentication.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings("UnusedVariable") // typeMapper reserved for future middleware types
public class RustMiddlewareGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustMiddlewareGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the middleware/mod.rs file.
     *
     * @return the mod.rs content
     */
    public String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Middleware components.\n\n");

        if (options.useJwt()) {
            sb.append("mod jwt;\n");
            sb.append("mod auth;\n");
            sb.append("\n");
            sb.append("pub use jwt::*;\n");
            sb.append("pub use auth::*;\n");
        }

        if (options.useArgon2()) {
            sb.append("mod password;\n");
            sb.append("pub use password::*;\n");
        }

        if (options.useRateLimiting()) {
            sb.append("mod rate_limit;\n");
            sb.append("pub use rate_limit::*;\n");
        }

        return sb.toString();
    }

    /**
     * Generates the JWT middleware module.
     *
     * @return the JWT middleware content
     */
    public String generateJwtMiddleware() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! JWT authentication middleware.\n\n");

        sb.append("use axum::{\n");
        sb.append("    extract::Request,\n");
        sb.append("    http::StatusCode,\n");
        sb.append("    middleware::Next,\n");
        sb.append("    response::Response,\n");
        sb.append("};\n");
        sb.append("use jsonwebtoken::{decode, DecodingKey, Validation};\n");
        sb.append("use serde::{Deserialize, Serialize};\n");
        sb.append("use std::env;\n");
        sb.append("\n");

        // Claims struct
        sb.append("#[derive(Debug, Serialize, Deserialize, Clone)]\n");
        sb.append("pub struct Claims {\n");
        sb.append("    pub sub: String,\n");
        sb.append("    pub exp: usize,\n");
        sb.append("    pub iat: usize,\n");
        sb.append("    #[serde(default)]\n");
        sb.append("    pub roles: Vec<String>,\n");
        sb.append("}\n\n");

        // JWT auth middleware
        sb.append("/// JWT authentication middleware.\n");
        sb.append(
                "pub async fn jwt_auth(request: Request, next: Next) -> Result<Response,"
                        + " StatusCode> {\n");
        sb.append("    // Get authorization header\n");
        sb.append("    let auth_header = request\n");
        sb.append("        .headers()\n");
        sb.append("        .get(\"Authorization\")\n");
        sb.append("        .and_then(|h| h.to_str().ok());\n\n");

        sb.append("    let token = match auth_header {\n");
        sb.append("        Some(h) if h.starts_with(\"Bearer \") => &h[7..],\n");
        sb.append("        _ => return Err(StatusCode::UNAUTHORIZED),\n");
        sb.append("    };\n\n");

        sb.append("    // Validate token\n");
        sb.append("    let secret = env::var(\"JWT_SECRET\")\n");
        sb.append("        .expect(\"JWT_SECRET must be set\");\n\n");

        sb.append("    let validation = Validation::default();\n");
        sb.append("    let key = DecodingKey::from_secret(secret.as_bytes());\n\n");

        sb.append("    match decode::<Claims>(token, &key, &validation) {\n");
        sb.append("        Ok(_) => Ok(next.run(request).await),\n");
        sb.append("        Err(_) => Err(StatusCode::UNAUTHORIZED),\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the auth module with JWT token generation.
     *
     * @return the auth module content
     */
    public String generateAuthModule() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Authentication utilities.\n\n");

        sb.append("use chrono::{Duration, Utc};\n");
        sb.append("use jsonwebtoken::{encode, EncodingKey, Header};\n");
        sb.append("use std::env;\n\n");

        sb.append("use super::jwt::Claims;\n\n");

        // Token generation
        sb.append("/// Generates a new JWT access token.\n");
        sb.append(
                "pub fn generate_token(user_id: &str, roles: Vec<String>) -> Result<String,"
                        + " jsonwebtoken::errors::Error> {\n");
        sb.append("    let secret = env::var(\"JWT_SECRET\")\n");
        sb.append("        .expect(\"JWT_SECRET must be set\");\n");
        sb.append("    let expiration_minutes: i64 = env::var(\"JWT_EXPIRATION_MINUTES\")\n");
        sb.append("        .unwrap_or_else(|_| \"15\".to_string())\n");
        sb.append("        .parse()\n");
        sb.append("        .unwrap_or(15);\n\n");

        sb.append("    let now = Utc::now();\n");
        sb.append(
                "    let exp = (now + Duration::minutes(expiration_minutes)).timestamp() as"
                        + " usize;\n");
        sb.append("    let iat = now.timestamp() as usize;\n\n");

        sb.append("    let claims = Claims {\n");
        sb.append("        sub: user_id.to_string(),\n");
        sb.append("        exp,\n");
        sb.append("        iat,\n");
        sb.append("        roles,\n");
        sb.append("    };\n\n");

        sb.append("    let key = EncodingKey::from_secret(secret.as_bytes());\n");
        sb.append("    encode(&Header::default(), &claims, &key)\n");
        sb.append("}\n\n");

        // Refresh token generation
        sb.append("/// Generates a new refresh token.\n");
        sb.append(
                "pub fn generate_refresh_token(user_id: &str) -> Result<String,"
                        + " jsonwebtoken::errors::Error> {\n");
        sb.append("    let secret = env::var(\"JWT_SECRET\")\n");
        sb.append("        .expect(\"JWT_SECRET must be set\");\n");
        sb.append(
                "    let expiration_minutes: i64 = env::var(\"JWT_REFRESH_EXPIRATION_MINUTES\")\n");
        sb.append("        .unwrap_or_else(|_| \"10080\".to_string())\n");
        sb.append("        .parse()\n");
        sb.append("        .unwrap_or(10080);\n\n");

        sb.append("    let now = Utc::now();\n");
        sb.append(
                "    let exp = (now + Duration::minutes(expiration_minutes)).timestamp() as"
                        + " usize;\n");
        sb.append("    let iat = now.timestamp() as usize;\n\n");

        sb.append("    let claims = Claims {\n");
        sb.append("        sub: user_id.to_string(),\n");
        sb.append("        exp,\n");
        sb.append("        iat,\n");
        sb.append("        roles: vec![\"refresh\".to_string()],\n");
        sb.append("    };\n\n");

        sb.append("    let key = EncodingKey::from_secret(secret.as_bytes());\n");
        sb.append("    encode(&Header::default(), &claims, &key)\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the password hashing module using Argon2.
     *
     * @return the password module content
     */
    public String generatePasswordModule() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Password hashing utilities using Argon2.\n\n");

        sb.append("use argon2::{\n");
        sb.append(
                "    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher,"
                        + " PasswordVerifier, SaltString},\n");
        sb.append("    Argon2,\n");
        sb.append("};\n\n");

        // Hash password
        sb.append("/// Hashes a password using Argon2.\n");
        sb.append(
                "pub fn hash_password(password: &str) -> Result<String,"
                        + " argon2::password_hash::Error> {\n");
        sb.append("    let salt = SaltString::generate(&mut OsRng);\n");
        sb.append("    let argon2 = Argon2::default();\n");
        sb.append("    let hash = argon2.hash_password(password.as_bytes(), &salt)?;\n");
        sb.append("    Ok(hash.to_string())\n");
        sb.append("}\n\n");

        // Verify password
        sb.append("/// Verifies a password against a hash.\n");
        sb.append(
                "pub fn verify_password(password: &str, hash: &str) -> Result<bool,"
                        + " argon2::password_hash::Error> {\n");
        sb.append("    let parsed_hash = PasswordHash::new(hash)?;\n");
        sb.append("    let argon2 = Argon2::default();\n");
        sb.append("    Ok(argon2.verify_password(password.as_bytes(), &parsed_hash).is_ok())\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the rate limiting middleware using governor crate.
     *
     * @return the rate limit module content
     */
    public String generateRateLimitMiddleware() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Rate limiting middleware using governor.\n\n");

        sb.append("use axum::{\n");
        sb.append("    body::Body,\n");
        sb.append("    extract::{ConnectInfo, Request},\n");
        sb.append("    http::StatusCode,\n");
        sb.append("    middleware::Next,\n");
        sb.append("    response::{IntoResponse, Response},\n");
        sb.append("};\n");
        sb.append("use governor::{\n");
        sb.append("    clock::{Clock, DefaultClock},\n");
        sb.append("    state::{InMemoryState, NotKeyed},\n");
        sb.append("    Quota, RateLimiter,\n");
        sb.append("};\n");
        sb.append("use std::{\n");
        sb.append("    collections::HashMap,\n");
        sb.append("    net::SocketAddr,\n");
        sb.append("    num::NonZeroU32,\n");
        sb.append("    sync::{Arc, RwLock},\n");
        sb.append("    time::Duration,\n");
        sb.append("};\n\n");

        // Type aliases
        sb.append("/// Per-IP rate limiter type.\n");
        sb.append(
                "pub type IpRateLimiter = RateLimiter<NotKeyed, InMemoryState, DefaultClock>;\n\n");

        // Rate limiter store
        sb.append("/// Store for per-IP rate limiters.\n");
        sb.append("#[derive(Clone)]\n");
        sb.append("pub struct RateLimiterStore {\n");
        sb.append("    limiters: Arc<RwLock<HashMap<String, Arc<IpRateLimiter>>>>,\n");
        sb.append("    quota: Quota,\n");
        sb.append("}\n\n");

        sb.append("impl RateLimiterStore {\n");
        sb.append("    /// Creates a new rate limiter store.\n");
        sb.append("    pub fn new(requests_per_second: u32, burst_size: u32) -> Self {\n");
        sb.append("        let quota = Quota::per_second(NonZeroU32::new(requests_per_second)");
        sb.append(".expect(\"rate must be > 0\"))\n");
        sb.append(
                "            .allow_burst(NonZeroU32::new(burst_size).expect(\"burst must >"
                        + " 0\"));\n");
        sb.append("        Self {\n");
        sb.append("            limiters: Arc::new(RwLock::new(HashMap::new())),\n");
        sb.append("            quota,\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        sb.append("    /// Gets or creates a rate limiter for the given IP.\n");
        sb.append("    pub fn get_limiter(&self, ip: &str) -> Arc<IpRateLimiter> {\n");
        sb.append("        // Try read lock first\n");
        sb.append("        {\n");
        sb.append("            let read = self.limiters.read().unwrap();\n");
        sb.append("            if let Some(limiter) = read.get(ip) {\n");
        sb.append("                return Arc::clone(limiter);\n");
        sb.append("            }\n");
        sb.append("        }\n\n");
        sb.append("        // Need to create new limiter\n");
        sb.append("        let mut write = self.limiters.write().unwrap();\n");
        sb.append("        // Double-check after acquiring write lock\n");
        sb.append("        if let Some(limiter) = write.get(ip) {\n");
        sb.append("            return Arc::clone(limiter);\n");
        sb.append("        }\n\n");
        sb.append("        let limiter = Arc::new(RateLimiter::direct(self.quota.clone()));\n");
        sb.append("        write.insert(ip.to_string(), Arc::clone(&limiter));\n");
        sb.append("        limiter\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // Rate limit error response
        sb.append("/// Rate limit exceeded error response.\n");
        sb.append("#[derive(serde::Serialize)]\n");
        sb.append("pub struct RateLimitError {\n");
        sb.append("    pub error: String,\n");
        sb.append("    pub retry_after_seconds: u64,\n");
        sb.append("}\n\n");

        sb.append("impl IntoResponse for RateLimitError {\n");
        sb.append("    fn into_response(self) -> Response {\n");
        sb.append("        let body = serde_json::to_string(&self).unwrap_or_default();\n");
        sb.append("        Response::builder()\n");
        sb.append("            .status(StatusCode::TOO_MANY_REQUESTS)\n");
        sb.append("            .header(\"Content-Type\", \"application/json\")\n");
        sb.append("            .header(\"Retry-After\", self.retry_after_seconds.to_string())\n");
        sb.append("            .body(Body::from(body))\n");
        sb.append("            .unwrap()\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // Middleware function
        sb.append("/// Rate limiting middleware.\n");
        sb.append("pub async fn rate_limit(\n");
        sb.append("    ConnectInfo(addr): ConnectInfo<SocketAddr>,\n");
        sb.append("    axum::Extension(store): axum::Extension<RateLimiterStore>,\n");
        sb.append("    request: Request,\n");
        sb.append("    next: Next,\n");
        sb.append(") -> Result<Response, RateLimitError> {\n");
        sb.append("    let ip = addr.ip().to_string();\n");
        sb.append("    let limiter = store.get_limiter(&ip);\n\n");

        sb.append("    match limiter.check() {\n");
        sb.append("        Ok(_) => Ok(next.run(request).await),\n");
        sb.append("        Err(not_until) => {\n");
        sb.append(
                "            let wait_time ="
                        + " not_until.wait_time_from(DefaultClock::default().now());\n");
        sb.append("            Err(RateLimitError {\n");
        sb.append("                error: \"Rate limit exceeded\".to_string(),\n");
        sb.append("                retry_after_seconds: wait_time.as_secs(),\n");
        sb.append("            })\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // Builder function
        sb.append("/// Creates a rate limiter layer with the given configuration.\n");
        sb.append("pub fn rate_limiter_layer(requests_per_second: u32, burst_size: u32) -> ");
        sb.append("axum::Extension<RateLimiterStore> {\n");
        sb.append("    axum::Extension(RateLimiterStore::new(requests_per_second, burst_size))\n");
        sb.append("}\n");

        return sb.toString();
    }
}
