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
package com.jnzader.apigen.codegen.generator.rust.config;

import com.jnzader.apigen.codegen.generator.api.ProjectConfig;
import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates Rust project configuration files including Cargo.toml, main.rs, error.rs, config.rs,
 * and environment files.
 *
 * @author APiGen
 * @since 2.12.0
 */
public class RustConfigGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;
    private final ProjectConfig config;
    private final String crateName;

    public RustConfigGenerator(
            RustTypeMapper typeMapper, RustAxumOptions options, ProjectConfig config) {
        this.typeMapper = typeMapper;
        this.options = options;
        this.config = config;
        this.crateName = typeMapper.toSnakeCase(config.getProjectName());
    }

    /** Generates Cargo.toml with conditional dependencies. */
    public String generateCargoToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("[package]\n");
        sb.append("name = \"").append(crateName).append("\"\n");
        sb.append("version = \"").append(config.getOption("version", "0.1.0")).append("\"\n");
        sb.append("edition = \"2024\"\n");
        sb.append("rust-version = \"1.85\"\n");
        sb.append("authors = [\"APiGen\"]\n");
        sb.append("description = \"Generated REST API with Axum\"\n");
        sb.append("\n");

        sb.append("[dependencies]\n");
        // Core dependencies (always included)
        sb.append("tokio = { version = \"1\", features = [\"full\"] }\n");
        sb.append("axum = { version = \"0.8\", features = [\"macros\"] }\n");
        sb.append("tower = \"0.5\"\n");
        sb.append("tower-http = { version = \"0.6\", features = [\"cors\", \"trace\"] }\n");
        sb.append("serde = { version = \"1\", features = [\"derive\"] }\n");
        sb.append("serde_json = \"1\"\n");
        sb.append("anyhow = \"1\"\n");
        sb.append("thiserror = \"2\"\n");

        // Tracing
        if (options.useTracing()) {
            sb.append("tracing = \"0.1\"\n");
            sb.append(
                    "tracing-subscriber = { version = \"0.3\", features = [\"env-filter\","
                            + " \"json\"] }\n");
        }

        // Database
        if (options.usePostgres() || options.useSqlite()) {
            sb.append("sqlx = { version = \"0.8\", features = [\"runtime-tokio\"");
            if (options.usePostgres()) {
                sb.append(", \"postgres\"");
            }
            if (options.useSqlite()) {
                sb.append(", \"sqlite\"");
            }
            sb.append(", \"chrono\", \"uuid\", \"rust_decimal\"] }\n");
        }

        // Common types
        sb.append("chrono = { version = \"0.4\", features = [\"serde\"] }\n");
        sb.append("uuid = { version = \"1\", features = [\"serde\", \"v4\"] }\n");
        sb.append("rust_decimal = { version = \"1\", features = [\"serde\"] }\n");

        // Validation
        sb.append("validator = { version = \"0.19\", features = [\"derive\"] }\n");

        // Config
        sb.append("toml = \"0.8\"\n");
        sb.append("dotenvy = \"0.15\"\n");

        // Redis
        if (options.useRedis()) {
            sb.append(
                    "redis = { version = \"0.27\", features = [\"tokio-comp\","
                            + " \"connection-manager\"] }\n");
        }

        // MQTT
        if (options.useMqtt()) {
            sb.append("rumqttc = \"0.24\"\n");
        }

        // NATS
        if (options.useNats()) {
            sb.append("async-nats = \"0.37\"\n");
        }

        // Modbus
        if (options.useModbus()) {
            sb.append("tokio-modbus = \"0.14\"\n");
        }

        // Serial
        if (options.useSerial()) {
            sb.append("serialport = \"4\"\n");
        }

        // ONNX
        if (options.useOnnx()) {
            sb.append("ort = \"2\"\n");
        }

        // Tokenizers
        if (options.useTokenizers()) {
            sb.append("tokenizers = \"0.20\"\n");
        }

        // ndarray
        if (options.useNdarray()) {
            sb.append("ndarray = \"0.16\"\n");
            sb.append("ndarray-stats = \"0.6\"\n");
        }

        // JWT
        if (options.useJwt()) {
            sb.append("jsonwebtoken = \"9\"\n");
        }

        // Argon2
        if (options.useArgon2()) {
            sb.append("argon2 = \"0.5\"\n");
        }

        // OpenTelemetry
        if (options.useOpenTelemetry()) {
            sb.append("opentelemetry = \"0.27\"\n");
            sb.append("opentelemetry-otlp = { version = \"0.27\", features = [\"tonic\"] }\n");
            sb.append("opentelemetry_sdk = { version = \"0.27\", features = [\"rt-tokio\"] }\n");
            sb.append("tracing-opentelemetry = \"0.28\"\n");
        }

        // Features
        sb.append("\n[features]\n");
        sb.append("default = [");
        List<String> defaultFeatures = new ArrayList<>();
        if (options.usePostgres()) defaultFeatures.add("\"postgres\"");
        if (!defaultFeatures.isEmpty()) {
            sb.append(String.join(", ", defaultFeatures));
        }
        sb.append("]\n");

        if (options.usePostgres()) {
            sb.append("postgres = []\n");
        }
        if (options.useSqlite()) {
            sb.append("sqlite = []\n");
        }

        // Profile settings
        sb.append("\n[profile.release]\n");
        sb.append("lto = true\n");
        sb.append("codegen-units = 1\n");
        sb.append("strip = true\n");

        return sb.toString();
    }

    /** Generates .env.example file. */
    public String generateEnvExample() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Server\n");
        sb.append("HOST=0.0.0.0\n");
        sb.append("PORT=8080\n");
        sb.append("RUST_LOG=info\n");
        sb.append("\n");

        if (options.usePostgres()) {
            sb.append("# PostgreSQL\n");
            sb.append("DATABASE_URL=postgres://user:password@localhost:5432/")
                    .append(crateName)
                    .append("\n");
            sb.append("\n");
        }

        if (options.useSqlite()) {
            sb.append("# SQLite\n");
            sb.append("SQLITE_PATH=./data/").append(crateName).append(".db\n");
            sb.append("\n");
        }

        if (options.useRedis()) {
            sb.append("# Redis\n");
            sb.append("REDIS_URL=redis://localhost:6379\n");
            sb.append("\n");
        }

        if (options.useMqtt()) {
            sb.append("# MQTT\n");
            sb.append("MQTT_BROKER=localhost\n");
            sb.append("MQTT_PORT=1883\n");
            sb.append("MQTT_CLIENT_ID=").append(crateName).append("\n");
            sb.append("\n");
        }

        if (options.useNats()) {
            sb.append("# NATS\n");
            sb.append("NATS_URL=nats://localhost:4222\n");
            sb.append("\n");
        }

        if (options.useModbus()) {
            sb.append("# Modbus\n");
            sb.append("MODBUS_HOST=localhost\n");
            sb.append("MODBUS_PORT=502\n");
            sb.append("\n");
        }

        if (options.useSerial()) {
            sb.append("# Serial\n");
            sb.append("SERIAL_PORT=/dev/ttyUSB0\n");
            sb.append("SERIAL_BAUD_RATE=9600\n");
            sb.append("\n");
        }

        if (options.useJwt()) {
            sb.append("# JWT\n");
            sb.append("JWT_SECRET=your-secret-key-min-32-chars-long!!\n");
            sb.append("JWT_EXPIRATION_MINUTES=15\n");
            sb.append("JWT_REFRESH_EXPIRATION_MINUTES=10080\n");
            sb.append("\n");
        }

        if (options.useOpenTelemetry()) {
            sb.append("# OpenTelemetry\n");
            sb.append("OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317\n");
            sb.append("OTEL_SERVICE_NAME=").append(crateName).append("\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /** Generates config.toml file. */
    public String generateConfigToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(crateName).append(" configuration\n\n");

        sb.append("[server]\n");
        sb.append("host = \"0.0.0.0\"\n");
        sb.append("port = 8080\n");
        sb.append("\n");

        if (options.usePostgres()) {
            sb.append("[database.postgres]\n");
            sb.append("max_connections = 10\n");
            sb.append("min_connections = 1\n");
            sb.append("acquire_timeout_secs = 30\n");
            sb.append("\n");
        }

        if (options.useSqlite()) {
            sb.append("[database.sqlite]\n");
            sb.append("max_connections = 5\n");
            sb.append("\n");
        }

        if (options.useRedis()) {
            sb.append("[redis]\n");
            sb.append("pool_size = 10\n");
            sb.append("\n");
        }

        if (options.useMqtt()) {
            sb.append("[mqtt]\n");
            sb.append("keep_alive_secs = 30\n");
            sb.append("clean_session = true\n");
            sb.append("\n");
        }

        if (options.useJwt()) {
            sb.append("[auth]\n");
            sb.append("token_expiration_minutes = 15\n");
            sb.append("refresh_expiration_minutes = 10080\n");
            sb.append("\n");
        }

        sb.append("[logging]\n");
        sb.append("level = \"info\"\n");
        sb.append("format = \"json\"\n");

        return sb.toString();
    }

    /** Generates src/config.rs module. */
    public String generateConfigRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Application configuration module.\n\n");

        sb.append("use serde::Deserialize;\n");
        sb.append("use std::env;\n\n");

        // Config struct
        sb.append("#[derive(Debug, Deserialize)]\n");
        sb.append("pub struct Config {\n");
        sb.append("    pub server: ServerConfig,\n");
        if (options.hasDatabase()) {
            sb.append("    pub database: DatabaseConfig,\n");
        }
        if (options.useRedis()) {
            sb.append("    pub redis: Option<RedisConfig>,\n");
        }
        if (options.useMqtt()) {
            sb.append("    pub mqtt: Option<MqttConfig>,\n");
        }
        if (options.useNats()) {
            sb.append("    pub nats: Option<NatsConfig>,\n");
        }
        if (options.useJwt()) {
            sb.append("    pub auth: AuthConfig,\n");
        }
        sb.append("    pub logging: LoggingConfig,\n");
        sb.append("}\n\n");

        // Server config
        sb.append("#[derive(Debug, Deserialize)]\n");
        sb.append("pub struct ServerConfig {\n");
        sb.append("    pub host: String,\n");
        sb.append("    pub port: u16,\n");
        sb.append("}\n\n");

        // Database config
        if (options.hasDatabase()) {
            sb.append("#[derive(Debug, Deserialize)]\n");
            sb.append("pub struct DatabaseConfig {\n");
            if (options.usePostgres()) {
                sb.append("    pub postgres: Option<PostgresConfig>,\n");
            }
            if (options.useSqlite()) {
                sb.append("    pub sqlite: Option<SqliteConfig>,\n");
            }
            sb.append("}\n\n");

            if (options.usePostgres()) {
                sb.append("#[derive(Debug, Deserialize)]\n");
                sb.append("pub struct PostgresConfig {\n");
                sb.append("    pub max_connections: u32,\n");
                sb.append("    pub min_connections: u32,\n");
                sb.append("    pub acquire_timeout_secs: u64,\n");
                sb.append("}\n\n");
            }

            if (options.useSqlite()) {
                sb.append("#[derive(Debug, Deserialize)]\n");
                sb.append("pub struct SqliteConfig {\n");
                sb.append("    pub max_connections: u32,\n");
                sb.append("}\n\n");
            }
        }

        // Redis config
        if (options.useRedis()) {
            sb.append("#[derive(Debug, Deserialize)]\n");
            sb.append("pub struct RedisConfig {\n");
            sb.append("    pub pool_size: u32,\n");
            sb.append("}\n\n");
        }

        // MQTT config
        if (options.useMqtt()) {
            sb.append("#[derive(Debug, Deserialize)]\n");
            sb.append("pub struct MqttConfig {\n");
            sb.append("    pub keep_alive_secs: u64,\n");
            sb.append("    pub clean_session: bool,\n");
            sb.append("}\n\n");
        }

        // NATS config
        if (options.useNats()) {
            sb.append("#[derive(Debug, Deserialize)]\n");
            sb.append("pub struct NatsConfig {\n");
            sb.append("    pub max_reconnects: u32,\n");
            sb.append("}\n\n");
        }

        // Auth config
        if (options.useJwt()) {
            sb.append("#[derive(Debug, Deserialize)]\n");
            sb.append("pub struct AuthConfig {\n");
            sb.append("    pub token_expiration_minutes: u64,\n");
            sb.append("    pub refresh_expiration_minutes: u64,\n");
            sb.append("}\n\n");
        }

        // Logging config
        sb.append("#[derive(Debug, Deserialize)]\n");
        sb.append("pub struct LoggingConfig {\n");
        sb.append("    pub level: String,\n");
        sb.append("    pub format: String,\n");
        sb.append("}\n\n");

        // Load function
        sb.append("impl Config {\n");
        sb.append("    /// Load configuration from config.toml and environment variables.\n");
        sb.append("    pub fn load() -> anyhow::Result<Self> {\n");
        sb.append("        dotenvy::dotenv().ok();\n");
        sb.append("        \n");
        sb.append("        let config_path = env::var(\"CONFIG_PATH\")\n");
        sb.append("            .unwrap_or_else(|_| \"config.toml\".to_string());\n");
        sb.append("        \n");
        sb.append("        let config_str = std::fs::read_to_string(&config_path)?;\n");
        sb.append("        let config: Config = toml::from_str(&config_str)?;\n");
        sb.append("        \n");
        sb.append("        Ok(config)\n");
        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /** Generates src/error.rs module. */
    public String generateErrorRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Application error types.\n\n");

        sb.append("use axum::{\n");
        sb.append("    http::StatusCode,\n");
        sb.append("    response::{IntoResponse, Response},\n");
        sb.append("    Json,\n");
        sb.append("};\n");
        sb.append("use serde::Serialize;\n");
        sb.append("use thiserror::Error;\n\n");

        // Error enum
        sb.append("#[derive(Debug, Error)]\n");
        sb.append("pub enum AppError {\n");
        sb.append("    #[error(\"Not found: {0}\")]\n");
        sb.append("    NotFound(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Bad request: {0}\")]\n");
        sb.append("    BadRequest(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Validation error: {0}\")]\n");
        sb.append("    Validation(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Unauthorized: {0}\")]\n");
        sb.append("    Unauthorized(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Forbidden: {0}\")]\n");
        sb.append("    Forbidden(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Conflict: {0}\")]\n");
        sb.append("    Conflict(String),\n");
        sb.append("\n");
        sb.append("    #[error(\"Internal error: {0}\")]\n");
        sb.append("    Internal(String),\n");
        sb.append("\n");
        if (options.hasDatabase()) {
            sb.append("    #[error(\"Database error: {0}\")]\n");
            sb.append("    Database(#[from] sqlx::Error),\n");
            sb.append("\n");
        }
        sb.append("}\n\n");

        // Error response struct
        sb.append("#[derive(Debug, Serialize)]\n");
        sb.append("pub struct ErrorResponse {\n");
        sb.append("    pub error: String,\n");
        sb.append("    pub message: String,\n");
        sb.append("    #[serde(skip_serializing_if = \"Option::is_none\")]\n");
        sb.append("    pub details: Option<Vec<String>>,\n");
        sb.append("}\n\n");

        // IntoResponse impl
        sb.append("impl IntoResponse for AppError {\n");
        sb.append("    fn into_response(self) -> Response {\n");
        sb.append("        let (status, error_type) = match &self {\n");
        sb.append("            AppError::NotFound(_) => (StatusCode::NOT_FOUND, \"not_found\"),\n");
        sb.append(
                "            AppError::BadRequest(_) => (StatusCode::BAD_REQUEST,"
                        + " \"bad_request\"),\n");
        sb.append(
                "            AppError::Validation(_) => (StatusCode::UNPROCESSABLE_ENTITY,"
                        + " \"validation_error\"),\n");
        sb.append(
                "            AppError::Unauthorized(_) => (StatusCode::UNAUTHORIZED,"
                        + " \"unauthorized\"),\n");
        sb.append(
                "            AppError::Forbidden(_) => (StatusCode::FORBIDDEN, \"forbidden\"),\n");
        sb.append("            AppError::Conflict(_) => (StatusCode::CONFLICT, \"conflict\"),\n");
        sb.append(
                "            AppError::Internal(_) => (StatusCode::INTERNAL_SERVER_ERROR,"
                        + " \"internal_error\"),\n");
        if (options.hasDatabase()) {
            sb.append(
                    "            AppError::Database(_) => (StatusCode::INTERNAL_SERVER_ERROR,"
                            + " \"database_error\"),\n");
        }
        sb.append("        };\n");
        sb.append("\n");
        sb.append("        let body = ErrorResponse {\n");
        sb.append("            error: error_type.to_string(),\n");
        sb.append("            message: self.to_string(),\n");
        sb.append("            details: None,\n");
        sb.append("        };\n");
        sb.append("\n");
        sb.append("        (status, Json(body)).into_response()\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        // Result type alias
        sb.append("/// Convenience type alias for AppError results.\n");
        sb.append("pub type AppResult<T> = Result<T, AppError>;\n");

        return sb.toString();
    }

    /** Generates src/lib.rs module. */
    public String generateLibRs(List<String> entityNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! ").append(crateName).append(" - Generated REST API\n\n");

        sb.append("pub mod config;\n");
        sb.append("pub mod error;\n");
        sb.append("pub mod models;\n");
        sb.append("pub mod dto;\n");
        sb.append("pub mod repository;\n");
        sb.append("pub mod service;\n");
        sb.append("pub mod handlers;\n");
        sb.append("pub mod router;\n");

        if (options.useJwt()) {
            sb.append("pub mod middleware;\n");
        }

        if (options.useMqtt()) {
            sb.append("pub mod mqtt;\n");
        }

        if (options.useModbus()) {
            sb.append("pub mod modbus;\n");
        }

        if (options.useSerial()) {
            sb.append("pub mod serial;\n");
        }

        if (options.useOnnx()) {
            sb.append("pub mod inference;\n");
        }

        return sb.toString();
    }

    /** Generates src/main.rs entry point. */
    public String generateMainRs(List<String> entityNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("//! ").append(crateName).append(" - Application entry point\n\n");

        sb.append("use ").append(crateName).append("::config::Config;\n");
        sb.append("use ").append(crateName).append("::router::create_router;\n");
        if (options.useTracing()) {
            sb.append("use tracing::info;\n");
            sb.append("use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};\n");
        }
        if (options.hasDatabase()) {
            sb.append("use sqlx::postgres::PgPoolOptions;\n");
        }
        sb.append("use std::net::SocketAddr;\n");
        if (options.useGracefulShutdown()) {
            sb.append("use tokio::signal;\n");
        }
        sb.append("\n");

        sb.append("#[tokio::main]\n");
        sb.append("async fn main() -> anyhow::Result<()> {\n");

        // Tracing setup
        if (options.useTracing()) {
            sb.append("    // Initialize tracing\n");
            sb.append("    tracing_subscriber::registry()\n");
            sb.append("        .with(tracing_subscriber::EnvFilter::try_from_default_env()\n");
            sb.append("            .unwrap_or_else(|_| \"info\".into()))\n");
            sb.append("        .with(tracing_subscriber::fmt::layer())\n");
            sb.append("        .init();\n\n");
        }

        // Load config
        sb.append("    // Load configuration\n");
        sb.append("    let config = Config::load()?;\n");
        if (options.useTracing()) {
            sb.append("    info!(\"Configuration loaded\");\n");
        }
        sb.append("\n");

        // Database pool
        if (options.usePostgres()) {
            sb.append("    // Create database pool\n");
            sb.append("    let database_url = std::env::var(\"DATABASE_URL\")\n");
            sb.append("        .expect(\"DATABASE_URL must be set\");\n");
            sb.append("    let pool = PgPoolOptions::new()\n");
            sb.append("        .max_connections(config.database.postgres.as_ref()\n");
            sb.append("            .map(|p| p.max_connections).unwrap_or(10))\n");
            sb.append("        .connect(&database_url)\n");
            sb.append("        .await?;\n");
            if (options.useTracing()) {
                sb.append("    info!(\"Database pool created\");\n");
            }
            sb.append("\n");
        }

        // Create router
        sb.append("    // Create router\n");
        if (options.usePostgres()) {
            sb.append("    let app = create_router(pool.clone());\n");
        } else {
            sb.append("    let app = create_router();\n");
        }
        sb.append("\n");

        // Server address
        sb.append("    // Start server\n");
        sb.append("    let addr = SocketAddr::from((\n");
        sb.append("        config.server.host.parse::<std::net::IpAddr>()?,\n");
        sb.append("        config.server.port,\n");
        sb.append("    ));\n");
        if (options.useTracing()) {
            sb.append("    info!(\"Starting server on {}\", addr);\n");
        }
        sb.append("\n");

        // Start server with graceful shutdown
        if (options.useGracefulShutdown()) {
            sb.append("    let listener = tokio::net::TcpListener::bind(addr).await?;\n");
            sb.append("    axum::serve(listener, app)\n");
            sb.append("        .with_graceful_shutdown(shutdown_signal())\n");
            sb.append("        .await?;\n");
        } else {
            sb.append("    let listener = tokio::net::TcpListener::bind(addr).await?;\n");
            sb.append("    axum::serve(listener, app).await?;\n");
        }
        sb.append("\n");

        sb.append("    Ok(())\n");
        sb.append("}\n");

        // Graceful shutdown signal
        if (options.useGracefulShutdown()) {
            sb.append("\n");
            sb.append("async fn shutdown_signal() {\n");
            sb.append("    let ctrl_c = async {\n");
            sb.append("        signal::ctrl_c()\n");
            sb.append("            .await\n");
            sb.append("            .expect(\"Failed to install Ctrl+C handler\");\n");
            sb.append("    };\n");
            sb.append("\n");
            sb.append("    #[cfg(unix)]\n");
            sb.append("    let terminate = async {\n");
            sb.append("        signal::unix::signal(signal::unix::SignalKind::terminate())\n");
            sb.append("            .expect(\"Failed to install signal handler\")\n");
            sb.append("            .recv()\n");
            sb.append("            .await;\n");
            sb.append("    };\n");
            sb.append("\n");
            sb.append("    #[cfg(not(unix))]\n");
            sb.append("    let terminate = std::future::pending::<()>();\n");
            sb.append("\n");
            sb.append("    tokio::select! {\n");
            sb.append("        _ = ctrl_c => {},\n");
            sb.append("        _ = terminate => {},\n");
            sb.append("    }\n");
            sb.append("\n");
            if (options.useTracing()) {
                sb.append("    info!(\"Shutdown signal received\");\n");
            }
            sb.append("}\n");
        }

        return sb.toString();
    }

    /** Generates Dockerfile. */
    public String generateDockerfile() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Build stage\n");
        sb.append("FROM rust:1.85-slim as builder\n");
        sb.append("\n");
        sb.append("WORKDIR /app\n");
        sb.append("\n");
        sb.append("# Install dependencies for sqlx\n");
        if (options.usePostgres()) {
            sb.append(
                    "RUN apt-get update && apt-get install -y libpq-dev pkg-config libssl-dev && rm"
                            + " -rf /var/lib/apt/lists/*\n");
        }
        sb.append("\n");
        sb.append("# Copy manifests\n");
        sb.append("COPY Cargo.toml Cargo.lock ./\n");
        sb.append("\n");
        sb.append("# Create dummy main.rs to cache dependencies\n");
        sb.append("RUN mkdir src && echo \"fn main() {}\" > src/main.rs\n");
        sb.append("RUN cargo build --release && rm -rf src\n");
        sb.append("\n");
        sb.append("# Copy source code\n");
        sb.append("COPY src ./src\n");
        sb.append("\n");
        sb.append("# Build release\n");
        sb.append("RUN touch src/main.rs && cargo build --release\n");
        sb.append("\n");
        sb.append("# Runtime stage\n");
        sb.append("FROM debian:bookworm-slim\n");
        sb.append("\n");
        sb.append("WORKDIR /app\n");
        sb.append("\n");
        if (options.usePostgres()) {
            sb.append(
                    "RUN apt-get update && apt-get install -y libpq5 ca-certificates && rm -rf"
                            + " /var/lib/apt/lists/*\n");
        } else {
            sb.append(
                    "RUN apt-get update && apt-get install -y ca-certificates && rm -rf"
                            + " /var/lib/apt/lists/*\n");
        }
        sb.append("\n");
        sb.append("COPY --from=builder /app/target/release/")
                .append(crateName)
                .append(" /app/")
                .append(crateName)
                .append("\n");
        sb.append("COPY config.toml /app/config.toml\n");
        sb.append("\n");
        sb.append("ENV HOST=0.0.0.0\n");
        sb.append("ENV PORT=8080\n");
        sb.append("ENV RUST_LOG=info\n");
        sb.append("\n");
        sb.append("EXPOSE 8080\n");
        sb.append("\n");
        sb.append("CMD [\"/app/").append(crateName).append("\"]\n");

        return sb.toString();
    }

    /** Generates docker-compose.yml. */
    public String generateDockerCompose() {
        StringBuilder sb = new StringBuilder();
        sb.append("version: '3.8'\n\n");
        sb.append("services:\n");
        sb.append("  api:\n");
        sb.append("    build: .\n");
        sb.append("    ports:\n");
        sb.append("      - \"8080:8080\"\n");
        sb.append("    environment:\n");
        sb.append("      - RUST_LOG=info\n");
        if (options.usePostgres()) {
            sb.append("      - DATABASE_URL=postgres://postgres:postgres@db:5432/")
                    .append(crateName)
                    .append("\n");
        }
        if (options.useRedis()) {
            sb.append("      - REDIS_URL=redis://redis:6379\n");
        }
        sb.append("    depends_on:\n");
        if (options.usePostgres()) {
            sb.append("      db:\n");
            sb.append("        condition: service_healthy\n");
        }
        if (options.useRedis()) {
            sb.append("      - redis\n");
        }
        sb.append("\n");

        if (options.usePostgres()) {
            sb.append("  db:\n");
            sb.append("    image: postgres:17-alpine\n");
            sb.append("    environment:\n");
            sb.append("      POSTGRES_USER: postgres\n");
            sb.append("      POSTGRES_PASSWORD: postgres\n");
            sb.append("      POSTGRES_DB: ").append(crateName).append("\n");
            sb.append("    ports:\n");
            sb.append("      - \"5432:5432\"\n");
            sb.append("    volumes:\n");
            sb.append("      - postgres_data:/var/lib/postgresql/data\n");
            sb.append("    healthcheck:\n");
            sb.append("      test: [\"CMD-SHELL\", \"pg_isready -U postgres\"]\n");
            sb.append("      interval: 5s\n");
            sb.append("      timeout: 5s\n");
            sb.append("      retries: 5\n");
            sb.append("\n");
        }

        if (options.useRedis()) {
            sb.append("  redis:\n");
            sb.append("    image: redis:7-alpine\n");
            sb.append("    ports:\n");
            sb.append("      - \"6379:6379\"\n");
            sb.append("\n");
        }

        if (options.useMqtt()) {
            sb.append("  mqtt:\n");
            sb.append("    image: eclipse-mosquitto:2\n");
            sb.append("    ports:\n");
            sb.append("      - \"1883:1883\"\n");
            sb.append("    volumes:\n");
            sb.append("      - ./mosquitto.conf:/mosquitto/config/mosquitto.conf\n");
            sb.append("\n");
        }

        sb.append("volumes:\n");
        if (options.usePostgres()) {
            sb.append("  postgres_data:\n");
        }

        return sb.toString();
    }

    /** Generates Makefile. */
    public String generateMakefile() {
        StringBuilder sb = new StringBuilder();
        sb.append(".PHONY: build run test clean docker\n\n");

        sb.append("build:\n");
        sb.append("\tcargo build --release\n\n");

        sb.append("run:\n");
        sb.append("\tcargo run\n\n");

        sb.append("test:\n");
        sb.append("\tcargo test\n\n");

        sb.append("clean:\n");
        sb.append("\tcargo clean\n\n");

        sb.append("fmt:\n");
        sb.append("\tcargo fmt\n\n");

        sb.append("lint:\n");
        sb.append("\tcargo clippy -- -D warnings\n\n");

        if (options.useDocker()) {
            sb.append("docker:\n");
            sb.append("\tdocker build -t ").append(crateName).append(" .\n\n");

            sb.append("docker-up:\n");
            sb.append("\tdocker-compose up -d\n\n");

            sb.append("docker-down:\n");
            sb.append("\tdocker-compose down\n\n");
        }

        return sb.toString();
    }

    /** Generates README.md. */
    public String generateReadme() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(config.getProjectName()).append("\n\n");
        sb.append("Generated REST API with Rust/Axum.\n\n");

        sb.append("## Quick Start\n\n");
        sb.append("```bash\n");
        sb.append("# Copy environment file\n");
        sb.append("cp .env.example .env\n\n");
        sb.append("# Edit .env with your configuration\n");
        sb.append("# ...\n\n");
        sb.append("# Run the application\n");
        sb.append("cargo run\n");
        sb.append("```\n\n");

        if (options.useDocker()) {
            sb.append("## Docker\n\n");
            sb.append("```bash\n");
            sb.append("# Start all services\n");
            sb.append("docker-compose up -d\n\n");
            sb.append("# View logs\n");
            sb.append("docker-compose logs -f api\n");
            sb.append("```\n\n");
        }

        sb.append("## API Endpoints\n\n");
        sb.append("- `GET /health` - Health check\n");
        sb.append("- `GET /api/v1/{resource}` - List resources\n");
        sb.append("- `GET /api/v1/{resource}/{id}` - Get resource by ID\n");
        sb.append("- `POST /api/v1/{resource}` - Create resource\n");
        sb.append("- `PUT /api/v1/{resource}/{id}` - Update resource\n");
        sb.append("- `DELETE /api/v1/{resource}/{id}` - Delete resource\n\n");

        sb.append("## Development\n\n");
        sb.append("```bash\n");
        sb.append("# Format code\n");
        sb.append("cargo fmt\n\n");
        sb.append("# Run linter\n");
        sb.append("cargo clippy\n\n");
        sb.append("# Run tests\n");
        sb.append("cargo test\n");
        sb.append("```\n");

        return sb.toString();
    }

    /** Generates .gitignore. */
    public String generateGitignore() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by Cargo\n");
        sb.append("/target/\n");
        sb.append("Cargo.lock\n\n");

        sb.append("# Environment\n");
        sb.append(".env\n\n");

        sb.append("# IDE\n");
        sb.append(".idea/\n");
        sb.append(".vscode/\n");
        sb.append("*.swp\n");
        sb.append("*.swo\n\n");

        if (options.useSqlite()) {
            sb.append("# SQLite\n");
            sb.append("*.db\n");
            sb.append("*.sqlite\n\n");
        }

        return sb.toString();
    }

    /** Generates rust-toolchain.toml. */
    public String generateRustToolchain() {
        StringBuilder sb = new StringBuilder();
        sb.append("[toolchain]\n");
        sb.append("channel = \"stable\"\n");
        sb.append("components = [\"rustfmt\", \"clippy\"]\n");
        return sb.toString();
    }
}
