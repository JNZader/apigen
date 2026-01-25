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
@SuppressWarnings({
    "java:S2479",
    "java:S1192"
}) // Literal tabs intentional for Go code; duplicate strings for templates
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
                generateServiceTest(entityName));
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
                \t"bytes"
                \t"encoding/json"
                \t"net/http"
                \t"net/http/httptest"
                \t"testing"

                \t"github.com/go-chi/chi/v5"
                \t"github.com/stretchr/testify/assert"
                \t"github.com/stretchr/testify/suite"
                \t"%s/internal/handler"
                \t"%s/internal/model"
                )

                type %sIntegrationSuite struct {
                \tsuite.Suite
                \trouter    *chi.Mux
                \tcreatedID uint
                }

                func (s *%sIntegrationSuite) SetupSuite() {
                \ts.router = chi.NewRouter()
                \t// Setup routes here
                }

                func (s *%sIntegrationSuite) TearDownSuite() {
                \t// Cleanup
                }

                func (s *%sIntegrationSuite) TestCreate%s() {
                \tcreate := model.Create%sDTO{
                \t\t// Add required fields
                \t}
                \tbody, _ := json.Marshal(create)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodPost, "/api/v1/%s", bytes.NewBuffer(body))
                \treq.Header.Set("Content-Type", "application/json")
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusCreated, w.Code)

                \tvar response model.%s
                \tjson.Unmarshal(w.Body.Bytes(), &response)
                \ts.createdID = response.ID
                \tassert.NotZero(s.T(), response.ID)
                }

                func (s *%sIntegrationSuite) TestGetAll%s() {
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s", nil)
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusOK, w.Code)

                \tvar response struct {
                \t\tItems []model.%s `json:"items"`
                \t\tTotal int64     `json:"total"`
                \t}
                \tjson.Unmarshal(w.Body.Bytes(), &response)
                \tassert.NotNil(s.T(), response.Items)
                }

                func (s *%sIntegrationSuite) TestGetByID() {
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s/1", nil)
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusOK, w.Code)
                }

                func (s *%sIntegrationSuite) TestGetByIDNotFound() {
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s/99999", nil)
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusNotFound, w.Code)
                }

                func (s *%sIntegrationSuite) TestUpdate%s() {
                \tupdate := model.Update%sDTO{
                \t\t// Add fields to update
                \t}
                \tbody, _ := json.Marshal(update)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodPut, "/api/v1/%s/1", bytes.NewBuffer(body))
                \treq.Header.Set("Content-Type", "application/json")
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusOK, w.Code)
                }

                func (s *%sIntegrationSuite) TestDelete%s() {
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodDelete, "/api/v1/%s/1", nil)
                \ts.router.ServeHTTP(w, req)

                \tassert.Equal(s.T(), http.StatusNoContent, w.Code)
                }

                func Test%sIntegrationSuite(t *testing.T) {
                \tsuite.Run(t, new(%sIntegrationSuite))
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

    private String generateServiceTest(String entityName) {
        return String.format(
                """
                package service

                import (
                \t"context"
                \t"testing"

                \t"github.com/stretchr/testify/assert"
                \t"github.com/stretchr/testify/mock"
                )

                type Mock%sRepository struct {
                \tmock.Mock
                }

                func (m *Mock%sRepository) FindAll(ctx context.Context, page, limit int) ([]%s, int64, error) {
                \targs := m.Called(ctx, page, limit)
                \treturn args.Get(0).([]%s), args.Get(1).(int64), args.Error(2)
                }

                func (m *Mock%sRepository) FindByID(ctx context.Context, id uint) (*%s, error) {
                \targs := m.Called(ctx, id)
                \tif args.Get(0) == nil {
                \t\treturn nil, args.Error(1)
                \t}
                \treturn args.Get(0).(*%s), args.Error(1)
                }

                func (m *Mock%sRepository) Create(ctx context.Context, entity *%s) error {
                \targs := m.Called(ctx, entity)
                \treturn args.Error(0)
                }

                func (m *Mock%sRepository) Update(ctx context.Context, entity *%s) error {
                \targs := m.Called(ctx, entity)
                \treturn args.Error(0)
                }

                func (m *Mock%sRepository) Delete(ctx context.Context, id uint) error {
                \targs := m.Called(ctx, id)
                \treturn args.Error(0)
                }

                func Test%sService_FindAll(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \texpected := []%s{{ID: 1}}
                \tmockRepo.On("FindAll", ctx, 1, 10).Return(expected, int64(1), nil)

                \tresult, total, err := service.FindAll(ctx, 1, 10)

                \tassert.NoError(t, err)
                \tassert.Equal(t, expected, result)
                \tassert.Equal(t, int64(1), total)
                \tmockRepo.AssertExpectations(t)
                }

                func Test%sService_FindByID(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \texpected := &%s{ID: 1}
                \tmockRepo.On("FindByID", ctx, uint(1)).Return(expected, nil)

                \tresult, err := service.FindByID(ctx, 1)

                \tassert.NoError(t, err)
                \tassert.Equal(t, expected, result)
                \tmockRepo.AssertExpectations(t)
                }

                func Test%sService_FindByID_NotFound(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \tmockRepo.On("FindByID", ctx, uint(99999)).Return(nil, ErrNotFound)

                \tresult, err := service.FindByID(ctx, 99999)

                \tassert.Error(t, err)
                \tassert.Nil(t, result)
                \tmockRepo.AssertExpectations(t)
                }

                func Test%sService_Create(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \tentity := &%s{}
                \tmockRepo.On("Create", ctx, entity).Return(nil)

                \terr := service.Create(ctx, entity)

                \tassert.NoError(t, err)
                \tmockRepo.AssertExpectations(t)
                }

                func Test%sService_Update(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \texisting := &%s{ID: 1}
                \tmockRepo.On("FindByID", ctx, uint(1)).Return(existing, nil)
                \tmockRepo.On("Update", ctx, existing).Return(nil)

                \terr := service.Update(ctx, 1, existing)

                \tassert.NoError(t, err)
                \tmockRepo.AssertExpectations(t)
                }

                func Test%sService_Delete(t *testing.T) {
                \tmockRepo := new(Mock%sRepository)
                \tservice := New%sService(mockRepo)
                \tctx := context.Background()

                \tmockRepo.On("Delete", ctx, uint(1)).Return(nil)

                \terr := service.Delete(ctx, 1)

                \tassert.NoError(t, err)
                \tmockRepo.AssertExpectations(t)
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
                \t"bytes"
                \t"context"
                \t"encoding/json"
                \t"net/http"
                \t"net/http/httptest"
                \t"testing"

                \t"github.com/go-chi/chi/v5"
                \t"github.com/stretchr/testify/assert"
                \t"github.com/stretchr/testify/mock"
                )

                type Mock%sService struct {
                \tmock.Mock
                }

                func setup%sHandler() (*chi.Mux, *Mock%sService) {
                \tmockService := new(Mock%sService)
                \thandler := New%sHandler(mockService)

                \trouter := chi.NewRouter()
                \trouter.Get("/api/v1/%s", handler.GetAll)
                \trouter.Get("/api/v1/%s/{id}", handler.GetByID)
                \trouter.Post("/api/v1/%s", handler.Create)
                \trouter.Put("/api/v1/%s/{id}", handler.Update)
                \trouter.Delete("/api/v1/%s/{id}", handler.Delete)

                \treturn router, mockService
                }

                func Test%sHandler_GetAll(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \texpected := []%s{{ID: 1}}
                \tmockService.On("FindAll", mock.Anything, 1, 10).Return(expected, int64(1), nil)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s?page=1&limit=10", nil)
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusOK, w.Code)
                \tmockService.AssertExpectations(t)
                }

                func Test%sHandler_GetByID(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \texpected := &%s{ID: 1}
                \tmockService.On("FindByID", mock.Anything, uint(1)).Return(expected, nil)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s/1", nil)
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusOK, w.Code)
                \tmockService.AssertExpectations(t)
                }

                func Test%sHandler_GetByID_NotFound(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \tmockService.On("FindByID", mock.Anything, uint(99999)).Return(nil, ErrNotFound)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodGet, "/api/v1/%s/99999", nil)
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusNotFound, w.Code)
                \tmockService.AssertExpectations(t)
                }

                func Test%sHandler_Create(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \tcreateDTO := Create%sDTO{}
                \tmockService.On("Create", mock.Anything, mock.AnythingOfType("*%s")).Return(nil)

                \tbody, _ := json.Marshal(createDTO)
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodPost, "/api/v1/%s", bytes.NewBuffer(body))
                \treq.Header.Set("Content-Type", "application/json")
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusCreated, w.Code)
                \tmockService.AssertExpectations(t)
                }

                func Test%sHandler_Update(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \tupdateDTO := Update%sDTO{}
                \tmockService.On("Update", mock.Anything, uint(1), mock.AnythingOfType("*%s")).Return(nil)

                \tbody, _ := json.Marshal(updateDTO)
                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodPut, "/api/v1/%s/1", bytes.NewBuffer(body))
                \treq.Header.Set("Content-Type", "application/json")
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusOK, w.Code)
                \tmockService.AssertExpectations(t)
                }

                func Test%sHandler_Delete(t *testing.T) {
                \trouter, mockService := setup%sHandler()

                \tmockService.On("Delete", mock.Anything, uint(1)).Return(nil)

                \tw := httptest.NewRecorder()
                \treq := httptest.NewRequest(http.MethodDelete, "/api/v1/%s/1", nil)
                \trouter.ServeHTTP(w, req)

                \tassert.Equal(t, http.StatusNoContent, w.Code)
                \tmockService.AssertExpectations(t)
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
