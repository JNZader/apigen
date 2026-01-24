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
}
