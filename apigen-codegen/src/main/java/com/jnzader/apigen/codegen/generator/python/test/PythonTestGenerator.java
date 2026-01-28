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
package com.jnzader.apigen.codegen.generator.python.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates pytest test files for Python/FastAPI projects.
 *
 * @author APiGen
 * @since 2.16.0
 */
public class PythonTestGenerator {

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
                "tests/test_" + snakeName + "_model.py", generateModelTest(entityName, snakeName));
        files.put(
                "tests/test_" + snakeName + "_schema.py",
                generateSchemaTest(entityName, snakeName));
        files.put(
                "tests/test_" + snakeName + "_service.py",
                generateServiceTest(entityName, snakeName));
        files.put(
                "tests/test_" + snakeName + "_router.py",
                generateRouterTest(entityName, snakeName));

        return files;
    }

    private String generateModelTest(String entityName, String snakeName) {
        return String.format(
                """
                import pytest
                from datetime import datetime

                from app.models.%s import %s

                pytestmark = pytest.mark.asyncio


                class Test%sModel:
                    \"""Unit tests for %s SQLAlchemy model.\"""

                    def test_create_instance(self):
                        \"""Test creating a model instance.\"""
                        instance = %s()
                        assert instance is not None

                    def test_default_estado(self):
                        \"""Test default value of estado field.\"""
                        instance = %s()
                        assert instance.estado is True

                    def test_set_and_get_id(self):
                        \"""Test setting and getting id.\"""
                        instance = %s()
                        instance.id = 1
                        assert instance.id == 1

                    def test_set_and_get_estado(self):
                        \"""Test setting and getting estado.\"""
                        instance = %s()
                        instance.estado = False
                        assert instance.estado is False

                    def test_audit_fields(self):
                        \"""Test setting audit fields.\"""
                        instance = %s()
                        now = datetime.utcnow()
                        instance.created_at = now
                        instance.updated_at = now
                        instance.created_by = "test_user"
                        instance.updated_by = "test_user"

                        assert instance.created_at == now
                        assert instance.updated_at == now
                        assert instance.created_by == "test_user"
                        assert instance.updated_by == "test_user"

                    def test_soft_delete_fields(self):
                        \"""Test soft delete fields.\"""
                        instance = %s()
                        now = datetime.utcnow()
                        instance.deleted_at = now
                        instance.deleted_by = "admin"

                        assert instance.deleted_at == now
                        assert instance.deleted_by == "admin"

                    def test_repr(self):
                        \"""Test string representation.\"""
                        instance = %s()
                        instance.id = 1
                        repr_str = repr(instance)
                        assert "%s" in repr_str
                """,
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
                entityName);
    }

    private String generateSchemaTest(String entityName, String snakeName) {
        return String.format(
                """
                import pytest
                from pydantic import ValidationError

                from app.schemas.%s import %sCreate, %sUpdate, %sResponse

                pytestmark = pytest.mark.asyncio


                class Test%sSchemas:
                    \"""Unit tests for %s Pydantic schemas.\"""

                    def test_create_schema_valid(self):
                        \"""Test create schema with valid data.\"""
                        # Add required fields
                        data = {}
                        schema = %sCreate(**data)
                        assert schema is not None

                    def test_update_schema_partial(self):
                        \"""Test update schema allows partial data.\"""
                        schema = %sUpdate()
                        assert schema is not None

                    def test_response_schema(self):
                        \"""Test response schema with all fields.\"""
                        data = {
                            "id": 1,
                            "activo": True,
                        }
                        schema = %sResponse(**data)
                        assert schema.id == 1
                        assert schema.activo is True

                    def test_activo_maps_to_estado(self):
                        \"""Test activo field maps to estado in entity.\"""
                        data = {
                            "id": 1,
                            "activo": False,
                        }
                        schema = %sResponse(**data)
                        assert schema.activo is False
                """,
                snakeName,
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

    /**
     * Generates conftest.py with test fixtures.
     *
     * @return conftest.py content
     */
    public String generateConftest() {
        return """
        import asyncio
        from typing import AsyncGenerator, Generator

        import pytest
        import pytest_asyncio
        from httpx import AsyncClient, ASGITransport
        from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker

        from app.main import app
        from app.core.database import Base, get_db

        # Test database URL (in-memory SQLite)
        TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

        engine = create_async_engine(TEST_DATABASE_URL, echo=False)
        async_session = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


        @pytest.fixture(scope="session")
        def event_loop() -> Generator:
            \"""Create an instance of the default event loop for the test session.\"""
            loop = asyncio.get_event_loop_policy().new_event_loop()
            yield loop
            loop.close()


        @pytest_asyncio.fixture
        async def db_session() -> AsyncGenerator[AsyncSession, None]:
            \"""Create a new database session for a test.\"""
            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.create_all)

            async with async_session() as session:
                yield session
                await session.rollback()

            async with engine.begin() as conn:
                await conn.run_sync(Base.metadata.drop_all)


        @pytest_asyncio.fixture
        async def client(db_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
            \"""Create a test client with database session override.\"""
            async def override_get_db():
                yield db_session

            app.dependency_overrides[get_db] = override_get_db

            async with AsyncClient(
                transport=ASGITransport(app=app),
                base_url="http://test"
            ) as ac:
                yield ac

            app.dependency_overrides.clear()
        """;
    }

    /**
     * Generates integration test for the API endpoints.
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
                import pytest
                from httpx import AsyncClient

                pytestmark = pytest.mark.asyncio


                class Test%sEndpoints:
                    \"""Integration tests for %s API endpoints.\"""

                    async def test_create_%s(self, client: AsyncClient):
                        \"""Test creating a new %s.\"""
                        payload = {
                            # Add required fields here
                        }
                        response = await client.post("/api/v1/%s", json=payload)
                        assert response.status_code == 201
                        data = response.json()
                        assert "id" in data

                    async def test_get_%s_list(self, client: AsyncClient):
                        \"""Test getting list of %s.\"""
                        response = await client.get("/api/v1/%s")
                        assert response.status_code == 200
                        data = response.json()
                        assert "items" in data
                        assert "total" in data

                    async def test_get_%s_by_id(self, client: AsyncClient):
                        \"""Test getting a %s by ID.\"""
                        # First create a %s
                        create_response = await client.post("/api/v1/%s", json={})
                        created_id = create_response.json()["id"]

                        # Then get it
                        response = await client.get(f"/api/v1/%s/{created_id}")
                        assert response.status_code == 200
                        data = response.json()
                        assert data["id"] == created_id

                    async def test_get_%s_not_found(self, client: AsyncClient):
                        \"""Test 404 when %s not found.\"""
                        response = await client.get("/api/v1/%s/99999")
                        assert response.status_code == 404

                    async def test_update_%s(self, client: AsyncClient):
                        \"""Test updating a %s.\"""
                        # First create a %s
                        create_response = await client.post("/api/v1/%s", json={})
                        created_id = create_response.json()["id"]

                        # Then update it
                        update_payload = {
                            # Add fields to update
                        }
                        response = await client.put(f"/api/v1/%s/{created_id}", json=update_payload)
                        assert response.status_code == 200

                    async def test_delete_%s(self, client: AsyncClient):
                        \"""Test deleting a %s.\"""
                        # First create a %s
                        create_response = await client.post("/api/v1/%s", json={})
                        created_id = create_response.json()["id"]

                        # Then delete it
                        response = await client.delete(f"/api/v1/%s/{created_id}")
                        assert response.status_code == 204

                        # Verify it's deleted
                        get_response = await client.get(f"/api/v1/%s/{created_id}")
                        assert get_response.status_code == 404
                """,
                entityName,
                entityName,
                snakeName,
                snakeName,
                pluralName,
                snakeName,
                pluralName,
                pluralName,
                snakeName,
                entityName,
                entityName,
                pluralName,
                pluralName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                entityName,
                entityName,
                pluralName,
                pluralName,
                snakeName,
                entityName,
                entityName,
                pluralName,
                pluralName,
                pluralName);
    }

    private String generateServiceTest(String entityName, String snakeName) {
        return String.format(
                """
                import pytest
                from unittest.mock import AsyncMock, MagicMock

                from app.services.%s_service import %sService
                from app.models.%s import %s
                from app.schemas.%s import %sCreate, %sUpdate

                pytestmark = pytest.mark.asyncio


                class Test%sService:
                    \"""Unit tests for %sService.\"""

                    @pytest.fixture
                    def mock_repository(self):
                        \"""Create a mock repository.\"""
                        return AsyncMock()

                    @pytest.fixture
                    def service(self, mock_repository):
                        \"""Create service with mocked repository.\"""
                        service = %sService()
                        service.repository = mock_repository
                        return service

                    async def test_get_all(self, service, mock_repository):
                        \"""Test get_all returns list of entities.\"""
                        mock_repository.get_all.return_value = []
                        result = await service.get_all()
                        assert result == []
                        mock_repository.get_all.assert_called_once()

                    async def test_get_by_id(self, service, mock_repository):
                        \"""Test get_by_id returns entity.\"""
                        expected = MagicMock(spec=%s)
                        mock_repository.get_by_id.return_value = expected
                        result = await service.get_by_id(1)
                        assert result == expected
                        mock_repository.get_by_id.assert_called_once_with(1)

                    async def test_get_by_id_not_found(self, service, mock_repository):
                        \"""Test get_by_id returns None when not found.\"""
                        mock_repository.get_by_id.return_value = None
                        result = await service.get_by_id(99999)
                        assert result is None

                    async def test_create(self, service, mock_repository):
                        \"""Test create returns created entity.\"""
                        create_data = %sCreate()
                        expected = MagicMock(spec=%s)
                        mock_repository.create.return_value = expected
                        result = await service.create(create_data)
                        assert result == expected
                        mock_repository.create.assert_called_once()

                    async def test_update(self, service, mock_repository):
                        \"""Test update returns updated entity.\"""
                        update_data = %sUpdate()
                        expected = MagicMock(spec=%s)
                        mock_repository.update.return_value = expected
                        result = await service.update(1, update_data)
                        assert result == expected

                    async def test_delete(self, service, mock_repository):
                        \"""Test delete calls repository delete.\"""
                        mock_repository.delete.return_value = True
                        result = await service.delete(1)
                        assert result is True
                        mock_repository.delete.assert_called_once_with(1)
                """,
                snakeName,
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
                entityName);
    }

    private String generateRouterTest(String entityName, String snakeName) {
        String pluralName = snakeName + "s";

        return String.format(
                """
                import pytest
                from httpx import AsyncClient
                from unittest.mock import AsyncMock, patch

                pytestmark = pytest.mark.asyncio


                class Test%sRouter:
                    \"""Unit tests for %s router endpoints.\"""

                    @pytest.fixture
                    def mock_service(self):
                        \"""Create a mock service.\"""
                        return AsyncMock()

                    async def test_list_%s(self, client: AsyncClient, mock_service):
                        \"""Test list endpoint returns paginated results.\"""
                        with patch("app.routers.%s.%sService", return_value=mock_service):
                            mock_service.get_all.return_value = []
                            mock_service.count.return_value = 0
                            response = await client.get("/api/v1/%s")
                            assert response.status_code == 200

                    async def test_get_%s(self, client: AsyncClient, mock_service):
                        \"""Test get by ID endpoint.\"""
                        with patch("app.routers.%s.%sService", return_value=mock_service):
                            mock_service.get_by_id.return_value = {"id": 1}
                            response = await client.get("/api/v1/%s/1")
                            assert response.status_code == 200

                    async def test_create_%s(self, client: AsyncClient, mock_service):
                        \"""Test create endpoint.\"""
                        with patch("app.routers.%s.%sService", return_value=mock_service):
                            mock_service.create.return_value = {"id": 1}
                            response = await client.post("/api/v1/%s", json={})
                            assert response.status_code == 201

                    async def test_update_%s(self, client: AsyncClient, mock_service):
                        \"""Test update endpoint.\"""
                        with patch("app.routers.%s.%sService", return_value=mock_service):
                            mock_service.update.return_value = {"id": 1}
                            response = await client.put("/api/v1/%s/1", json={})
                            assert response.status_code == 200

                    async def test_delete_%s(self, client: AsyncClient, mock_service):
                        \"""Test delete endpoint.\"""
                        with patch("app.routers.%s.%sService", return_value=mock_service):
                            mock_service.delete.return_value = True
                            response = await client.delete("/api/v1/%s/1")
                            assert response.status_code == 204
                """,
                entityName,
                entityName,
                pluralName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                snakeName,
                entityName,
                pluralName,
                snakeName,
                snakeName,
                entityName,
                pluralName);
    }
}
