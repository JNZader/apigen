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
package com.jnzader.apigen.codegen.generator.gochi.test;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Go test files for Go/Chi projects.
 *
 * @author APiGen
 * @since 2.16.0
 */
public class GoChiTestGenerator {

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
                "internal/service/" + snakeName + "_service_test.go",
                generateServiceTest(entityName, snakeName));
        files.put(
                "internal/handler/" + snakeName + "_handler_test.go",
                generateHandlerTest(entityName, snakeName));

        return files;
    }

    /**
     * Generates integration test for the API.
     *
     * @param table the table
     * @param moduleName Go module name
     * @return integration test content
     */
    public String generateIntegrationTest(SqlTable table, String moduleName) {
        String entityName = table.getEntityName();
        String snakeName = toSnakeCase(table.getName());
        String pluralName = snakeName + "s";

        return String.format(
                """
                package integration_test

                import (
                	"bytes"
                	"encoding/json"
                	"net/http"
                	"net/http/httptest"
                	"testing"

                	"github.com/go-chi/chi/v5"
                	"github.com/stretchr/testify/assert"
                	"github.com/stretchr/testify/suite"
                	"%s/internal/handler"
                	"%s/internal/model"
                )

                type %sIntegrationSuite struct {
                	suite.Suite
                	router    *chi.Mux
                	createdID uint
                }

                func (s *%sIntegrationSuite) SetupSuite() {
                	s.router = chi.NewRouter()
                	// Setup routes here
                }

                func (s *%sIntegrationSuite) TearDownSuite() {
                	// Cleanup
                }

                func (s *%sIntegrationSuite) TestCreate%s() {
                	create := model.Create%sDTO{
                		// Add required fields
                	}
                	body, _ := json.Marshal(create)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodPost, "/api/v1/%s", bytes.NewBuffer(body))
                	req.Header.Set("Content-Type", "application/json")
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusCreated, w.Code)

                	var response model.%s
                	json.Unmarshal(w.Body.Bytes(), &response)
                	s.createdID = response.ID
                	assert.NotZero(s.T(), response.ID)
                }

                func (s *%sIntegrationSuite) TestGetAll%s() {
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s", nil)
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusOK, w.Code)

                	var response struct {
                		Items []model.%s `json:"items"`
                		Total int64     `json:"total"`
                	}
                	json.Unmarshal(w.Body.Bytes(), &response)
                	assert.NotNil(s.T(), response.Items)
                }

                func (s *%sIntegrationSuite) TestGetByID() {
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s/1", nil)
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusOK, w.Code)
                }

                func (s *%sIntegrationSuite) TestGetByIDNotFound() {
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s/99999", nil)
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusNotFound, w.Code)
                }

                func (s *%sIntegrationSuite) TestUpdate%s() {
                	update := model.Update%sDTO{
                		// Add fields to update
                	}
                	body, _ := json.Marshal(update)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodPut, "/api/v1/%s/1", bytes.NewBuffer(body))
                	req.Header.Set("Content-Type", "application/json")
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusOK, w.Code)
                }

                func (s *%sIntegrationSuite) TestDelete%s() {
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodDelete, "/api/v1/%s/1", nil)
                	s.router.ServeHTTP(w, req)

                	assert.Equal(s.T(), http.StatusNoContent, w.Code)
                }

                func Test%sIntegrationSuite(t *testing.T) {
                	suite.Run(t, new(%sIntegrationSuite))
                }
                """,
                moduleName,
                moduleName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                pluralName,
                entityName,
                entityName,
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
                pluralName,
                entityName,
                entityName);
    }

    private String generateServiceTest(String entityName, String snakeName) {
        return String.format(
                """
                package service

                import (
                	"context"
                	"testing"

                	"github.com/stretchr/testify/assert"
                	"github.com/stretchr/testify/mock"
                )

                type Mock%sRepository struct {
                	mock.Mock
                }

                func (m *Mock%sRepository) FindAll(ctx context.Context, page, limit int) ([]%s, int64, error) {
                	args := m.Called(ctx, page, limit)
                	return args.Get(0).([]%s), args.Get(1).(int64), args.Error(2)
                }

                func (m *Mock%sRepository) FindByID(ctx context.Context, id uint) (*%s, error) {
                	args := m.Called(ctx, id)
                	if args.Get(0) == nil {
                		return nil, args.Error(1)
                	}
                	return args.Get(0).(*%s), args.Error(1)
                }

                func (m *Mock%sRepository) Create(ctx context.Context, entity *%s) error {
                	args := m.Called(ctx, entity)
                	return args.Error(0)
                }

                func (m *Mock%sRepository) Update(ctx context.Context, entity *%s) error {
                	args := m.Called(ctx, entity)
                	return args.Error(0)
                }

                func (m *Mock%sRepository) Delete(ctx context.Context, id uint) error {
                	args := m.Called(ctx, id)
                	return args.Error(0)
                }

                func Test%sService_FindAll(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	expected := []%s{{ID: 1}}
                	mockRepo.On("FindAll", ctx, 1, 10).Return(expected, int64(1), nil)

                	result, total, err := service.FindAll(ctx, 1, 10)

                	assert.NoError(t, err)
                	assert.Equal(t, expected, result)
                	assert.Equal(t, int64(1), total)
                	mockRepo.AssertExpectations(t)
                }

                func Test%sService_FindByID(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	expected := &%s{ID: 1}
                	mockRepo.On("FindByID", ctx, uint(1)).Return(expected, nil)

                	result, err := service.FindByID(ctx, 1)

                	assert.NoError(t, err)
                	assert.Equal(t, expected, result)
                	mockRepo.AssertExpectations(t)
                }

                func Test%sService_FindByID_NotFound(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	mockRepo.On("FindByID", ctx, uint(99999)).Return(nil, ErrNotFound)

                	result, err := service.FindByID(ctx, 99999)

                	assert.Error(t, err)
                	assert.Nil(t, result)
                	mockRepo.AssertExpectations(t)
                }

                func Test%sService_Create(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	entity := &%s{}
                	mockRepo.On("Create", ctx, entity).Return(nil)

                	err := service.Create(ctx, entity)

                	assert.NoError(t, err)
                	mockRepo.AssertExpectations(t)
                }

                func Test%sService_Update(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	existing := &%s{ID: 1}
                	mockRepo.On("FindByID", ctx, uint(1)).Return(existing, nil)
                	mockRepo.On("Update", ctx, existing).Return(nil)

                	err := service.Update(ctx, 1, existing)

                	assert.NoError(t, err)
                	mockRepo.AssertExpectations(t)
                }

                func Test%sService_Delete(t *testing.T) {
                	mockRepo := new(Mock%sRepository)
                	service := New%sService(mockRepo)
                	ctx := context.Background()

                	mockRepo.On("Delete", ctx, uint(1)).Return(nil)

                	err := service.Delete(ctx, 1)

                	assert.NoError(t, err)
                	mockRepo.AssertExpectations(t)
                }
                """,
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
                package handler

                import (
                	"bytes"
                	"context"
                	"encoding/json"
                	"net/http"
                	"net/http/httptest"
                	"testing"

                	"github.com/go-chi/chi/v5"
                	"github.com/stretchr/testify/assert"
                	"github.com/stretchr/testify/mock"
                )

                type Mock%sService struct {
                	mock.Mock
                }

                func setup%sHandler() (*chi.Mux, *Mock%sService) {
                	mockService := new(Mock%sService)
                	handler := New%sHandler(mockService)

                	router := chi.NewRouter()
                	router.Get("/api/v1/%s", handler.GetAll)
                	router.Get("/api/v1/%s/{id}", handler.GetByID)
                	router.Post("/api/v1/%s", handler.Create)
                	router.Put("/api/v1/%s/{id}", handler.Update)
                	router.Delete("/api/v1/%s/{id}", handler.Delete)

                	return router, mockService
                }

                func Test%sHandler_GetAll(t *testing.T) {
                	router, mockService := setup%sHandler()

                	expected := []%s{{ID: 1}}
                	mockService.On("FindAll", mock.Anything, 1, 10).Return(expected, int64(1), nil)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s?page=1&limit=10", nil)
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusOK, w.Code)
                	mockService.AssertExpectations(t)
                }

                func Test%sHandler_GetByID(t *testing.T) {
                	router, mockService := setup%sHandler()

                	expected := &%s{ID: 1}
                	mockService.On("FindByID", mock.Anything, uint(1)).Return(expected, nil)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s/1", nil)
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusOK, w.Code)
                	mockService.AssertExpectations(t)
                }

                func Test%sHandler_GetByID_NotFound(t *testing.T) {
                	router, mockService := setup%sHandler()

                	mockService.On("FindByID", mock.Anything, uint(99999)).Return(nil, ErrNotFound)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodGet, "/api/v1/%s/99999", nil)
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusNotFound, w.Code)
                	mockService.AssertExpectations(t)
                }

                func Test%sHandler_Create(t *testing.T) {
                	router, mockService := setup%sHandler()

                	createDTO := Create%sDTO{}
                	mockService.On("Create", mock.Anything, mock.AnythingOfType("*%s")).Return(nil)

                	body, _ := json.Marshal(createDTO)
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodPost, "/api/v1/%s", bytes.NewBuffer(body))
                	req.Header.Set("Content-Type", "application/json")
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusCreated, w.Code)
                	mockService.AssertExpectations(t)
                }

                func Test%sHandler_Update(t *testing.T) {
                	router, mockService := setup%sHandler()

                	updateDTO := Update%sDTO{}
                	mockService.On("Update", mock.Anything, uint(1), mock.AnythingOfType("*%s")).Return(nil)

                	body, _ := json.Marshal(updateDTO)
                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodPut, "/api/v1/%s/1", bytes.NewBuffer(body))
                	req.Header.Set("Content-Type", "application/json")
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusOK, w.Code)
                	mockService.AssertExpectations(t)
                }

                func Test%sHandler_Delete(t *testing.T) {
                	router, mockService := setup%sHandler()

                	mockService.On("Delete", mock.Anything, uint(1)).Return(nil)

                	w := httptest.NewRecorder()
                	req := httptest.NewRequest(http.MethodDelete, "/api/v1/%s/1", nil)
                	router.ServeHTTP(w, req)

                	assert.Equal(t, http.StatusNoContent, w.Code)
                	mockService.AssertExpectations(t)
                }
                """,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                pluralName,
                pluralName,
                pluralName,
                pluralName,
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
                entityName,
                pluralName,
                entityName,
                entityName,
                entityName,
                entityName,
                pluralName,
                entityName,
                entityName,
                entityName,
                entityName,
                pluralName,
                entityName,
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
