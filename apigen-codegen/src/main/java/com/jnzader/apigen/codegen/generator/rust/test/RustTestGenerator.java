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
package com.jnzader.apigen.codegen.generator.rust.test;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Rust test files for Rust/Axum projects.
 *
 * @author APiGen
 * @since 2.16.0
 */
public class RustTestGenerator {

    /**
     * Generates all test files for a table.
     *
     * @param table the table to generate tests for
     * @return map of file path to content
     */
    public Map<String, String> generateTests(SqlTable table) {
        Map<String, String> files = new LinkedHashMap<>();
        String entityName = table.getEntityName();
        String snakeName = toSnakeCase(table.getName());

        files.put(
                "tests/" + snakeName + "_service_test.rs",
                generateServiceTest(entityName, snakeName));
        files.put(
                "tests/" + snakeName + "_handler_test.rs",
                generateHandlerTest(entityName, snakeName));

        return files;
    }

    /**
     * Generates integration test for the API.
     *
     * @param table the table
     * @return integration test content
     */
    public String generateIntegrationTest(SqlTable table) {
        String entityName = table.getEntityName();
        String snakeName = toSnakeCase(table.getName());
        String pluralName = snakeName + "s";

        return String.format(
                """
                use axum::{
                    body::Body,
                    http::{Request, StatusCode},
                };
                use serde_json::{json, Value};
                use tower::ServiceExt;

                use crate::app::create_app;
                use crate::models::%s::{Create%sDto, Update%sDto};

                async fn setup_test_app() -> impl tower::Service<
                    Request<Body>,
                    Response = axum::response::Response,
                    Error = std::convert::Infallible,
                > {
                    create_app().await
                }

                #[tokio::test]
                async fn test_create_%s() {
                    let app = setup_test_app().await;

                    let create_dto = Create%sDto {
                        // Add required fields
                    };

                    let request = Request::builder()
                        .method("POST")
                        .uri("/api/v1/%s")
                        .header("Content-Type", "application/json")
                        .body(Body::from(serde_json::to_string(&create_dto).unwrap()))
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::CREATED);
                }

                #[tokio::test]
                async fn test_get_%s_list() {
                    let app = setup_test_app().await;

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::OK);

                    let body = hyper::body::to_bytes(response.into_body()).await.unwrap();
                    let json: Value = serde_json::from_slice(&body).unwrap();

                    assert!(json.get("items").is_some());
                    assert!(json.get("total").is_some());
                }

                #[tokio::test]
                async fn test_get_%s_by_id() {
                    let app = setup_test_app().await;

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s/1")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::OK);
                }

                #[tokio::test]
                async fn test_get_%s_not_found() {
                    let app = setup_test_app().await;

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s/99999")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::NOT_FOUND);
                }

                #[tokio::test]
                async fn test_update_%s() {
                    let app = setup_test_app().await;

                    let update_dto = Update%sDto {
                        // Add fields to update
                    };

                    let request = Request::builder()
                        .method("PUT")
                        .uri("/api/v1/%s/1")
                        .header("Content-Type", "application/json")
                        .body(Body::from(serde_json::to_string(&update_dto).unwrap()))
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::OK);
                }

                #[tokio::test]
                async fn test_delete_%s() {
                    let app = setup_test_app().await;

                    let request = Request::builder()
                        .method("DELETE")
                        .uri("/api/v1/%s/1")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();

                    assert_eq!(response.status(), StatusCode::NO_CONTENT);
                }
                """,
                snakeName,
                entityName,
                entityName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                pluralName,
                snakeName,
                pluralName,
                snakeName,
                pluralName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                pluralName);
    }

    private String generateServiceTest(String entityName, String snakeName) {
        return String.format(
                """
                use std::sync::Arc;
                use mockall::predicate::*;
                use mockall::mock;

                use crate::models::%s::{%s, Create%sDto, Update%sDto};
                use crate::services::%s_service::%sService;
                use crate::repositories::%s_repository::%sRepository;
                use crate::errors::AppError;

                mock! {
                    %sRepository {}

                    #[async_trait::async_trait]
                    impl %sRepositoryTrait for %sRepository {
                        async fn find_all(&self, page: i32, limit: i32) -> Result<(Vec<%s>, i64), AppError>;
                        async fn find_by_id(&self, id: i64) -> Result<Option<%s>, AppError>;
                        async fn create(&self, entity: &%s) -> Result<%s, AppError>;
                        async fn update(&self, entity: &%s) -> Result<%s, AppError>;
                        async fn delete(&self, id: i64) -> Result<(), AppError>;
                    }
                }

                #[tokio::test]
                async fn test_find_all() {
                    let mut mock_repo = Mock%sRepository::new();

                    mock_repo
                        .expect_find_all()
                        .with(eq(1), eq(10))
                        .times(1)
                        .returning(|_, _| Ok((vec![], 0)));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.find_all(1, 10).await;

                    assert!(result.is_ok());
                    let (items, total) = result.unwrap();
                    assert_eq!(items.len(), 0);
                    assert_eq!(total, 0);
                }

                #[tokio::test]
                async fn test_find_by_id() {
                    let mut mock_repo = Mock%sRepository::new();

                    let expected = %s {
                        id: 1,
                        // Add other fields
                    };

                    mock_repo
                        .expect_find_by_id()
                        .with(eq(1))
                        .times(1)
                        .returning(move |_| Ok(Some(expected.clone())));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.find_by_id(1).await;

                    assert!(result.is_ok());
                    assert!(result.unwrap().is_some());
                }

                #[tokio::test]
                async fn test_find_by_id_not_found() {
                    let mut mock_repo = Mock%sRepository::new();

                    mock_repo
                        .expect_find_by_id()
                        .with(eq(99999))
                        .times(1)
                        .returning(|_| Ok(None));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.find_by_id(99999).await;

                    assert!(result.is_ok());
                    assert!(result.unwrap().is_none());
                }

                #[tokio::test]
                async fn test_create() {
                    let mut mock_repo = Mock%sRepository::new();

                    let create_dto = Create%sDto {
                        // Add required fields
                    };

                    mock_repo
                        .expect_create()
                        .times(1)
                        .returning(|_| Ok(%s { id: 1 }));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.create(create_dto).await;

                    assert!(result.is_ok());
                }

                #[tokio::test]
                async fn test_update() {
                    let mut mock_repo = Mock%sRepository::new();

                    let existing = %s { id: 1 };
                    let update_dto = Update%sDto {
                        // Add fields to update
                    };

                    mock_repo
                        .expect_find_by_id()
                        .with(eq(1))
                        .times(1)
                        .returning(move |_| Ok(Some(existing.clone())));

                    mock_repo
                        .expect_update()
                        .times(1)
                        .returning(|_| Ok(%s { id: 1 }));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.update(1, update_dto).await;

                    assert!(result.is_ok());
                }

                #[tokio::test]
                async fn test_delete() {
                    let mut mock_repo = Mock%sRepository::new();

                    mock_repo
                        .expect_delete()
                        .with(eq(1))
                        .times(1)
                        .returning(|_| Ok(()));

                    let service = %sService::new(Arc::new(mock_repo));
                    let result = service.delete(1).await;

                    assert!(result.is_ok());
                }
                """,
                snakeName,
                entityName,
                entityName,
                entityName,
                snakeName,
                entityName,
                snakeName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName);
    }

    private String generateHandlerTest(String entityName, String snakeName) {
        String pluralName = snakeName + "s";

        return String.format(
                """
                use axum::{
                    body::Body,
                    http::{Request, StatusCode},
                    Router,
                };
                use mockall::predicate::*;
                use mockall::mock;
                use serde_json::json;
                use std::sync::Arc;
                use tower::ServiceExt;

                use crate::handlers::%s_handler;
                use crate::models::%s::{%s, Create%sDto, Update%sDto};
                use crate::services::%s_service::%sService;

                mock! {
                    %sService {}

                    #[async_trait::async_trait]
                    impl %sServiceTrait for %sService {
                        async fn find_all(&self, page: i32, limit: i32) -> Result<(Vec<%s>, i64), AppError>;
                        async fn find_by_id(&self, id: i64) -> Result<Option<%s>, AppError>;
                        async fn create(&self, dto: Create%sDto) -> Result<%s, AppError>;
                        async fn update(&self, id: i64, dto: Update%sDto) -> Result<%s, AppError>;
                        async fn delete(&self, id: i64) -> Result<(), AppError>;
                    }
                }

                fn create_test_router(service: Mock%sService) -> Router {
                    %s_handler::router(Arc::new(service))
                }

                #[tokio::test]
                async fn test_handler_get_all() {
                    let mut mock_service = Mock%sService::new();

                    mock_service
                        .expect_find_all()
                        .times(1)
                        .returning(|_, _| Ok((vec![], 0)));

                    let app = create_test_router(mock_service);

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::OK);
                }

                #[tokio::test]
                async fn test_handler_get_by_id() {
                    let mut mock_service = Mock%sService::new();

                    let entity = %s { id: 1 };
                    mock_service
                        .expect_find_by_id()
                        .with(eq(1))
                        .times(1)
                        .returning(move |_| Ok(Some(entity.clone())));

                    let app = create_test_router(mock_service);

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s/1")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::OK);
                }

                #[tokio::test]
                async fn test_handler_get_by_id_not_found() {
                    let mut mock_service = Mock%sService::new();

                    mock_service
                        .expect_find_by_id()
                        .with(eq(99999))
                        .times(1)
                        .returning(|_| Ok(None));

                    let app = create_test_router(mock_service);

                    let request = Request::builder()
                        .method("GET")
                        .uri("/api/v1/%s/99999")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::NOT_FOUND);
                }

                #[tokio::test]
                async fn test_handler_create() {
                    let mut mock_service = Mock%sService::new();

                    mock_service
                        .expect_create()
                        .times(1)
                        .returning(|_| Ok(%s { id: 1 }));

                    let app = create_test_router(mock_service);

                    let create_dto = Create%sDto {};
                    let request = Request::builder()
                        .method("POST")
                        .uri("/api/v1/%s")
                        .header("Content-Type", "application/json")
                        .body(Body::from(serde_json::to_string(&create_dto).unwrap()))
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::CREATED);
                }

                #[tokio::test]
                async fn test_handler_update() {
                    let mut mock_service = Mock%sService::new();

                    mock_service
                        .expect_update()
                        .times(1)
                        .returning(|_, _| Ok(%s { id: 1 }));

                    let app = create_test_router(mock_service);

                    let update_dto = Update%sDto {};
                    let request = Request::builder()
                        .method("PUT")
                        .uri("/api/v1/%s/1")
                        .header("Content-Type", "application/json")
                        .body(Body::from(serde_json::to_string(&update_dto).unwrap()))
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::OK);
                }

                #[tokio::test]
                async fn test_handler_delete() {
                    let mut mock_service = Mock%sService::new();

                    mock_service
                        .expect_delete()
                        .with(eq(1))
                        .times(1)
                        .returning(|_| Ok(()));

                    let app = create_test_router(mock_service);

                    let request = Request::builder()
                        .method("DELETE")
                        .uri("/api/v1/%s/1")
                        .body(Body::empty())
                        .unwrap();

                    let response = app.oneshot(request).await.unwrap();
                    assert_eq!(response.status(), StatusCode::NO_CONTENT);
                }
                """,
                snakeName,
                snakeName,
                entityName,
                entityName,
                entityName,
                snakeName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                snakeName,
                entityName,
                pluralName,
                entityName,
                entityName,
                pluralName,
                entityName,
                pluralName,
                entityName,
                entityName,
                entityName,
                pluralName,
                entityName,
                entityName,
                entityName,
                pluralName,
                entityName,
                pluralName);
    }

    /** Converts PascalCase or camelCase to snake_case. */
    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
