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
package com.jnzader.apigen.codegen.generator.rust.storage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates file storage code for Rust/Axum applications.
 *
 * @author APiGen
 * @since 2.13.0
 */
public class RustFileStorageGenerator {

    /**
     * Generates file storage files.
     *
     * @param useS3 whether to generate S3 storage support
     * @param useAzure whether to generate Azure Blob storage support
     * @return map of file path to content
     */
    public Map<String, String> generate(boolean useS3, boolean useAzure) {
        Map<String, String> files = new LinkedHashMap<>();

        files.put("src/storage/mod.rs", generateModRs(useS3, useAzure));
        files.put("src/storage/config.rs", generateConfig(useS3, useAzure));
        files.put("src/storage/service.rs", generateService());
        files.put("src/storage/handler.rs", generateHandler());
        files.put("src/storage/dto.rs", generateDto());
        files.put("src/storage/model.rs", generateModel());
        files.put("src/storage/local.rs", generateLocalStorage());

        if (useS3) {
            files.put("src/storage/s3.rs", generateS3Storage());
        }

        if (useAzure) {
            files.put("src/storage/azure.rs", generateAzureStorage());
        }

        return files;
    }

    private String generateModRs(boolean useS3, boolean useAzure) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                //! File storage module.

                mod config;
                mod dto;
                mod handler;
                mod local;
                mod model;
                mod service;
                """);

        if (useS3) {
            sb.append("mod s3;\n");
        }
        if (useAzure) {
            sb.append("mod azure;\n");
        }

        sb.append(
                """

                pub use config::*;
                pub use dto::*;
                pub use handler::*;
                pub use model::FileMetadata;
                pub use service::StorageService;
                """);

        return sb.toString();
    }

    private String generateConfig(boolean useS3, boolean useAzure) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                """
                //! Storage configuration.

                use serde::Deserialize;

                /// Storage type.
                #[derive(Debug, Clone, Deserialize, Default)]
                #[serde(rename_all = "lowercase")]
                pub enum StorageType {
                    #[default]
                    Local,
                """);

        if (useS3) {
            sb.append("    S3,\n");
        }
        if (useAzure) {
            sb.append("    Azure,\n");
        }

        sb.append(
                """
                }

                /// Storage configuration.
                #[derive(Debug, Clone, Deserialize)]
                pub struct StorageConfig {
                    #[serde(default)]
                    pub storage_type: StorageType,
                    pub local: Option<LocalStorageConfig>,
                """);

        if (useS3) {
            sb.append("    pub s3: Option<S3Config>,\n");
        }
        if (useAzure) {
            sb.append("    pub azure: Option<AzureConfig>,\n");
        }

        sb.append(
                """
                    #[serde(default = "default_max_file_size")]
                    pub max_file_size: usize,
                    #[serde(default = "default_allowed_extensions")]
                    pub allowed_extensions: Vec<String>,
                }

                fn default_max_file_size() -> usize {
                    10 * 1024 * 1024 // 10MB
                }

                fn default_allowed_extensions() -> Vec<String> {
                    vec![
                        "jpg".into(), "jpeg".into(), "png".into(), "gif".into(), "webp".into(),
                        "pdf".into(), "doc".into(), "docx".into(), "xls".into(), "xlsx".into(),
                        "txt".into(), "csv".into(), "zip".into(),
                    ]
                }

                /// Local storage configuration.
                #[derive(Debug, Clone, Deserialize)]
                pub struct LocalStorageConfig {
                    pub path: String,
                    #[serde(default = "default_base_url")]
                    pub base_url: String,
                }

                fn default_base_url() -> String {
                    "http://localhost:3000/files".to_string()
                }
                """);

        if (useS3) {
            sb.append(
                    """

                    /// S3 storage configuration.
                    #[derive(Debug, Clone, Deserialize)]
                    pub struct S3Config {
                        pub bucket: String,
                        pub region: String,
                        pub access_key_id: String,
                        pub secret_access_key: String,
                        #[serde(default)]
                        pub endpoint: Option<String>,
                    }
                    """);
        }

        if (useAzure) {
            sb.append(
                    """

                    /// Azure Blob storage configuration.
                    #[derive(Debug, Clone, Deserialize)]
                    pub struct AzureConfig {
                        pub container: String,
                        pub account_name: String,
                        pub account_key: String,
                    }
                    """);
        }

        sb.append(
                """

                impl Default for StorageConfig {
                    fn default() -> Self {
                        Self {
                            storage_type: StorageType::Local,
                            local: Some(LocalStorageConfig {
                                path: "./uploads".to_string(),
                                base_url: default_base_url(),
                            }),
                """);

        if (useS3) {
            sb.append("            s3: None,\n");
        }
        if (useAzure) {
            sb.append("            azure: None,\n");
        }

        sb.append(
                """
                            max_file_size: default_max_file_size(),
                            allowed_extensions: default_allowed_extensions(),
                        }
                    }
                }
                """);

        return sb.toString();
    }

    private String generateService() {
        return """
        //! Storage service abstraction.

        use async_trait::async_trait;
        use bytes::Bytes;
        use sqlx::PgPool;
        use tracing::{info, error};
        use uuid::Uuid;
        use super::{
            config::{StorageConfig, StorageType},
            dto::{FileUploadResponse, FileInfoResponse},
            local::LocalStorage,
            model::FileMetadata,
        };
        use crate::error::AppError;

        /// Storage backend trait.
        #[async_trait]
        pub trait StorageBackend: Send + Sync {
            /// Upload a file.
            async fn upload(&self, key: &str, data: Bytes, content_type: &str) -> Result<String, AppError>;
            /// Download a file.
            async fn download(&self, key: &str) -> Result<Bytes, AppError>;
            /// Delete a file.
            async fn delete(&self, key: &str) -> Result<(), AppError>;
            /// Get file URL.
            fn get_url(&self, key: &str) -> String;
        }

        /// Storage service.
        #[derive(Clone)]
        pub struct StorageService {
            backend: std::sync::Arc<dyn StorageBackend>,
            pool: PgPool,
            config: StorageConfig,
        }

        impl StorageService {
            /// Create a new storage service.
            pub fn new(config: StorageConfig, pool: PgPool) -> Result<Self, AppError> {
                let backend: std::sync::Arc<dyn StorageBackend> = match config.storage_type {
                    StorageType::Local => {
                        let local_config = config.local.clone()
                            .ok_or_else(|| AppError::Internal("Local storage not configured".into()))?;
                        std::sync::Arc::new(LocalStorage::new(local_config)?)
                    }
                    #[allow(unreachable_patterns)]
                    _ => return Err(AppError::Internal("Storage type not supported".into())),
                };

                Ok(Self { backend, pool, config })
            }

            /// Upload a file.
            pub async fn upload(
                &self,
                filename: &str,
                data: Bytes,
                content_type: &str,
                user_id: Option<Uuid>,
            ) -> Result<FileUploadResponse, AppError> {
                // Validate file size
                if data.len() > self.config.max_file_size {
                    return Err(AppError::BadRequest(format!(
                        "File size exceeds maximum of {} bytes",
                        self.config.max_file_size
                    )));
                }

                // Validate extension
                let extension = std::path::Path::new(filename)
                    .extension()
                    .and_then(|e| e.to_str())
                    .unwrap_or("")
                    .to_lowercase();

                if !self.config.allowed_extensions.contains(&extension) {
                    return Err(AppError::BadRequest(format!(
                        "File extension '{}' is not allowed",
                        extension
                    )));
                }

                // Generate unique key
                let file_id = Uuid::new_v4();
                let key = format!("{}/{}.{}", chrono::Utc::now().format("%Y/%m/%d"), file_id, extension);

                // Upload to backend
                let url = self.backend.upload(&key, data.clone(), content_type).await?;

                // Save metadata
                let metadata = FileMetadata {
                    id: file_id,
                    filename: filename.to_string(),
                    storage_key: key,
                    content_type: content_type.to_string(),
                    size: data.len() as i64,
                    url: url.clone(),
                    user_id,
                    created_at: chrono::Utc::now(),
                };

                sqlx::query(
                    r#"INSERT INTO file_metadata (id, filename, storage_key, content_type, size, url, user_id, created_at)
                       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)"#
                )
                .bind(metadata.id)
                .bind(&metadata.filename)
                .bind(&metadata.storage_key)
                .bind(&metadata.content_type)
                .bind(metadata.size)
                .bind(&metadata.url)
                .bind(metadata.user_id)
                .bind(metadata.created_at)
                .execute(&self.pool)
                .await?;

                info!("File uploaded: {} -> {}", filename, key);

                Ok(FileUploadResponse {
                    id: file_id,
                    filename: filename.to_string(),
                    url,
                    size: data.len() as i64,
                    content_type: content_type.to_string(),
                })
            }

            /// Get file info.
            pub async fn get_info(&self, id: Uuid) -> Result<FileInfoResponse, AppError> {
                let metadata = sqlx::query_as::<_, FileMetadata>(
                    "SELECT id, filename, storage_key, content_type, size, url, user_id, created_at FROM file_metadata WHERE id = $1"
                )
                .bind(id)
                .fetch_optional(&self.pool)
                .await?
                .ok_or_else(|| AppError::NotFound("File not found".into()))?;

                Ok(FileInfoResponse {
                    id: metadata.id,
                    filename: metadata.filename,
                    url: metadata.url,
                    size: metadata.size,
                    content_type: metadata.content_type,
                    created_at: metadata.created_at,
                })
            }

            /// Download a file.
            pub async fn download(&self, id: Uuid) -> Result<(Bytes, String, String), AppError> {
                let metadata = sqlx::query_as::<_, FileMetadata>(
                    "SELECT id, filename, storage_key, content_type, size, url, user_id, created_at FROM file_metadata WHERE id = $1"
                )
                .bind(id)
                .fetch_optional(&self.pool)
                .await?
                .ok_or_else(|| AppError::NotFound("File not found".into()))?;

                let data = self.backend.download(&metadata.storage_key).await?;

                Ok((data, metadata.filename, metadata.content_type))
            }

            /// Delete a file.
            pub async fn delete(&self, id: Uuid) -> Result<(), AppError> {
                let metadata = sqlx::query_as::<_, FileMetadata>(
                    "SELECT id, filename, storage_key, content_type, size, url, user_id, created_at FROM file_metadata WHERE id = $1"
                )
                .bind(id)
                .fetch_optional(&self.pool)
                .await?
                .ok_or_else(|| AppError::NotFound("File not found".into()))?;

                // Delete from backend
                self.backend.delete(&metadata.storage_key).await?;

                // Delete metadata
                sqlx::query("DELETE FROM file_metadata WHERE id = $1")
                    .bind(id)
                    .execute(&self.pool)
                    .await?;

                info!("File deleted: {}", id);

                Ok(())
            }
        }
        """;
    }

    private String generateHandler() {
        return """
        //! File storage HTTP handlers.

        use axum::{
            body::Body,
            extract::{Multipart, Path, State},
            http::{header, StatusCode},
            response::{IntoResponse, Response},
            Json,
        };
        use bytes::Bytes;
        use std::sync::Arc;
        use uuid::Uuid;
        use super::{
            dto::{FileUploadResponse, FileInfoResponse},
            service::StorageService,
        };
        use crate::error::AppError;

        /// Storage application state.
        pub struct StorageState {
            pub service: StorageService,
        }

        /// Upload a file.
        #[axum::debug_handler]
        pub async fn upload_file(
            State(state): State<Arc<StorageState>>,
            mut multipart: Multipart,
        ) -> Result<Json<FileUploadResponse>, AppError> {
            while let Some(field) = multipart.next_field().await
                .map_err(|e| AppError::BadRequest(format!("Multipart error: {}", e)))?
            {
                if field.name() == Some("file") {
                    let filename = field.file_name()
                        .ok_or_else(|| AppError::BadRequest("Missing filename".into()))?
                        .to_string();

                    let content_type = field.content_type()
                        .unwrap_or("application/octet-stream")
                        .to_string();

                    let data = field.bytes().await
                        .map_err(|e| AppError::BadRequest(format!("Failed to read file: {}", e)))?;

                    let response = state.service.upload(&filename, data, &content_type, None).await?;
                    return Ok(Json(response));
                }
            }

            Err(AppError::BadRequest("No file field found".into()))
        }

        /// Get file info.
        #[axum::debug_handler]
        pub async fn get_file_info(
            State(state): State<Arc<StorageState>>,
            Path(id): Path<Uuid>,
        ) -> Result<Json<FileInfoResponse>, AppError> {
            let info = state.service.get_info(id).await?;
            Ok(Json(info))
        }

        /// Download a file.
        #[axum::debug_handler]
        pub async fn download_file(
            State(state): State<Arc<StorageState>>,
            Path(id): Path<Uuid>,
        ) -> Result<impl IntoResponse, AppError> {
            let (data, filename, content_type) = state.service.download(id).await?;

            let body = Body::from(data);

            Ok(Response::builder()
                .status(StatusCode::OK)
                .header(header::CONTENT_TYPE, content_type)
                .header(
                    header::CONTENT_DISPOSITION,
                    format!("attachment; filename=\"{}\"", filename),
                )
                .body(body)
                .unwrap())
        }

        /// Delete a file.
        #[axum::debug_handler]
        pub async fn delete_file(
            State(state): State<Arc<StorageState>>,
            Path(id): Path<Uuid>,
        ) -> Result<StatusCode, AppError> {
            state.service.delete(id).await?;
            Ok(StatusCode::NO_CONTENT)
        }
        """;
    }

    private String generateDto() {
        return """
        //! File storage DTOs.

        use chrono::{DateTime, Utc};
        use serde::{Deserialize, Serialize};
        use uuid::Uuid;

        /// File upload response.
        #[derive(Debug, Serialize)]
        pub struct FileUploadResponse {
            pub id: Uuid,
            pub filename: String,
            pub url: String,
            pub size: i64,
            pub content_type: String,
        }

        /// File info response.
        #[derive(Debug, Serialize)]
        pub struct FileInfoResponse {
            pub id: Uuid,
            pub filename: String,
            pub url: String,
            pub size: i64,
            pub content_type: String,
            pub created_at: DateTime<Utc>,
        }
        """;
    }

    private String generateModel() {
        return """
        //! File metadata model.

        use chrono::{DateTime, Utc};
        use sqlx::FromRow;
        use uuid::Uuid;

        /// File metadata entity.
        #[derive(Debug, Clone, FromRow)]
        pub struct FileMetadata {
            pub id: Uuid,
            pub filename: String,
            pub storage_key: String,
            pub content_type: String,
            pub size: i64,
            pub url: String,
            pub user_id: Option<Uuid>,
            pub created_at: DateTime<Utc>,
        }
        """;
    }

    private String generateLocalStorage() {
        return """
        //! Local filesystem storage backend.

        use async_trait::async_trait;
        use bytes::Bytes;
        use std::path::PathBuf;
        use tokio::fs;
        use tokio::io::AsyncWriteExt;
        use super::{config::LocalStorageConfig, service::StorageBackend};
        use crate::error::AppError;

        /// Local filesystem storage.
        pub struct LocalStorage {
            config: LocalStorageConfig,
        }

        impl LocalStorage {
            /// Create a new local storage backend.
            pub fn new(config: LocalStorageConfig) -> Result<Self, AppError> {
                // Ensure directory exists
                std::fs::create_dir_all(&config.path)
                    .map_err(|e| AppError::Internal(format!("Failed to create storage directory: {}", e)))?;

                Ok(Self { config })
            }

            fn get_path(&self, key: &str) -> PathBuf {
                PathBuf::from(&self.config.path).join(key)
            }
        }

        #[async_trait]
        impl StorageBackend for LocalStorage {
            async fn upload(&self, key: &str, data: Bytes, _content_type: &str) -> Result<String, AppError> {
                let path = self.get_path(key);

                // Create parent directories
                if let Some(parent) = path.parent() {
                    fs::create_dir_all(parent).await
                        .map_err(|e| AppError::Internal(format!("Failed to create directory: {}", e)))?;
                }

                // Write file
                let mut file = fs::File::create(&path).await
                    .map_err(|e| AppError::Internal(format!("Failed to create file: {}", e)))?;

                file.write_all(&data).await
                    .map_err(|e| AppError::Internal(format!("Failed to write file: {}", e)))?;

                Ok(self.get_url(key))
            }

            async fn download(&self, key: &str) -> Result<Bytes, AppError> {
                let path = self.get_path(key);

                let data = fs::read(&path).await
                    .map_err(|e| AppError::NotFound(format!("File not found: {}", e)))?;

                Ok(Bytes::from(data))
            }

            async fn delete(&self, key: &str) -> Result<(), AppError> {
                let path = self.get_path(key);

                fs::remove_file(&path).await
                    .map_err(|e| AppError::Internal(format!("Failed to delete file: {}", e)))?;

                Ok(())
            }

            fn get_url(&self, key: &str) -> String {
                format!("{}/{}", self.config.base_url, key)
            }
        }
        """;
    }

    private String generateS3Storage() {
        return """
        //! AWS S3 storage backend.

        use async_trait::async_trait;
        use aws_sdk_s3::{
            config::{Credentials, Region},
            primitives::ByteStream,
            Client,
        };
        use bytes::Bytes;
        use super::{config::S3Config, service::StorageBackend};
        use crate::error::AppError;

        /// S3 storage backend.
        pub struct S3Storage {
            client: Client,
            bucket: String,
        }

        impl S3Storage {
            /// Create a new S3 storage backend.
            pub async fn new(config: S3Config) -> Result<Self, AppError> {
                let credentials = Credentials::new(
                    &config.access_key_id,
                    &config.secret_access_key,
                    None,
                    None,
                    "apigen",
                );

                let mut s3_config = aws_sdk_s3::Config::builder()
                    .credentials_provider(credentials)
                    .region(Region::new(config.region.clone()));

                if let Some(endpoint) = &config.endpoint {
                    s3_config = s3_config.endpoint_url(endpoint);
                }

                let client = Client::from_conf(s3_config.build());

                Ok(Self {
                    client,
                    bucket: config.bucket,
                })
            }
        }

        #[async_trait]
        impl StorageBackend for S3Storage {
            async fn upload(&self, key: &str, data: Bytes, content_type: &str) -> Result<String, AppError> {
                self.client
                    .put_object()
                    .bucket(&self.bucket)
                    .key(key)
                    .body(ByteStream::from(data))
                    .content_type(content_type)
                    .send()
                    .await
                    .map_err(|e| AppError::Internal(format!("S3 upload failed: {}", e)))?;

                Ok(self.get_url(key))
            }

            async fn download(&self, key: &str) -> Result<Bytes, AppError> {
                let response = self.client
                    .get_object()
                    .bucket(&self.bucket)
                    .key(key)
                    .send()
                    .await
                    .map_err(|e| AppError::NotFound(format!("S3 download failed: {}", e)))?;

                let data = response.body.collect().await
                    .map_err(|e| AppError::Internal(format!("Failed to read S3 response: {}", e)))?
                    .into_bytes();

                Ok(data)
            }

            async fn delete(&self, key: &str) -> Result<(), AppError> {
                self.client
                    .delete_object()
                    .bucket(&self.bucket)
                    .key(key)
                    .send()
                    .await
                    .map_err(|e| AppError::Internal(format!("S3 delete failed: {}", e)))?;

                Ok(())
            }

            fn get_url(&self, key: &str) -> String {
                format!("https://{}.s3.amazonaws.com/{}", self.bucket, key)
            }
        }
        """;
    }

    private String generateAzureStorage() {
        return """
        //! Azure Blob storage backend.

        use async_trait::async_trait;
        use azure_storage::StorageCredentials;
        use azure_storage_blobs::prelude::*;
        use bytes::Bytes;
        use futures::StreamExt;
        use super::{config::AzureConfig, service::StorageBackend};
        use crate::error::AppError;

        /// Azure Blob storage backend.
        pub struct AzureStorage {
            client: ContainerClient,
        }

        impl AzureStorage {
            /// Create a new Azure storage backend.
            pub fn new(config: AzureConfig) -> Result<Self, AppError> {
                let credentials = StorageCredentials::access_key(
                    &config.account_name,
                    config.account_key.clone(),
                );

                let client = ClientBuilder::new(&config.account_name, credentials)
                    .container_client(&config.container);

                Ok(Self { client })
            }
        }

        #[async_trait]
        impl StorageBackend for AzureStorage {
            async fn upload(&self, key: &str, data: Bytes, content_type: &str) -> Result<String, AppError> {
                self.client
                    .blob_client(key)
                    .put_block_blob(data)
                    .content_type(content_type)
                    .await
                    .map_err(|e| AppError::Internal(format!("Azure upload failed: {}", e)))?;

                Ok(self.get_url(key))
            }

            async fn download(&self, key: &str) -> Result<Bytes, AppError> {
                let mut stream = self.client
                    .blob_client(key)
                    .get()
                    .into_stream();

                let mut data = Vec::new();
                while let Some(chunk) = stream.next().await {
                    let chunk = chunk
                        .map_err(|e| AppError::NotFound(format!("Azure download failed: {}", e)))?;
                    data.extend_from_slice(&chunk.data.collect().await
                        .map_err(|e| AppError::Internal(format!("Failed to collect data: {}", e)))?);
                }

                Ok(Bytes::from(data))
            }

            async fn delete(&self, key: &str) -> Result<(), AppError> {
                self.client
                    .blob_client(key)
                    .delete()
                    .await
                    .map_err(|e| AppError::Internal(format!("Azure delete failed: {}", e)))?;

                Ok(())
            }

            fn get_url(&self, key: &str) -> String {
                self.client.blob_client(key).url().map(|u| u.to_string()).unwrap_or_default()
            }
        }
        """;
    }
}
