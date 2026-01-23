package com.jnzader.apigen.codegen.generator.python.service;

import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates service classes for Python/FastAPI projects.
 *
 * <p>Services contain business logic and coordinate between repositories and routers.
 */
public class PythonServiceGenerator {

    private final PythonTypeMapper typeMapper;

    public PythonServiceGenerator() {
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates the base service class.
     *
     * @return the base_service.py content
     */
    public String generateBase() {
        return """
        from typing import Generic, TypeVar, Type
        from fastapi import HTTPException, status

        from app.repositories.base_repository import BaseRepository
        from app.models.base import BaseModel

        ModelType = TypeVar("ModelType", bound=BaseModel)
        CreateSchemaType = TypeVar("CreateSchemaType")
        UpdateSchemaType = TypeVar("UpdateSchemaType")
        ResponseSchemaType = TypeVar("ResponseSchemaType")


        class BaseService(Generic[ModelType, CreateSchemaType, UpdateSchemaType, ResponseSchemaType]):
            \"\"\"Base service with common CRUD operations.\"\"\"

            def __init__(
                self,
                repository: BaseRepository[ModelType],
                response_schema: Type[ResponseSchemaType]
            ):
                self.repository = repository
                self.response_schema = response_schema

            async def get_by_id(self, id: int) -> ResponseSchemaType:
                \"\"\"Get entity by ID.\"\"\"
                entity = await self.repository.get_by_id(id)
                if not entity:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail=f"Entity with id {id} not found"
                    )
                return self.response_schema.model_validate(entity)

            async def get_all(
                self,
                page: int = 0,
                size: int = 10
            ) -> dict:
                \"\"\"Get all entities with pagination.\"\"\"
                skip = page * size
                entities = await self.repository.get_all(skip=skip, limit=size)
                total = await self.repository.count()
                pages = (total + size - 1) // size

                return {
                    "items": [self.response_schema.model_validate(e) for e in entities],
                    "total": total,
                    "page": page,
                    "size": size,
                    "pages": pages
                }

            async def create(self, obj_in: CreateSchemaType) -> ResponseSchemaType:
                \"\"\"Create a new entity.\"\"\"
                entity = await self.repository.create(obj_in.model_dump())
                return self.response_schema.model_validate(entity)

            async def update(self, id: int, obj_in: UpdateSchemaType) -> ResponseSchemaType:
                \"\"\"Update an existing entity.\"\"\"
                entity = await self.repository.get_by_id(id)
                if not entity:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail=f"Entity with id {id} not found"
                    )
                updated = await self.repository.update(
                    entity,
                    obj_in.model_dump(exclude_unset=True)
                )
                return self.response_schema.model_validate(updated)

            async def delete(self, id: int) -> None:
                \"\"\"Soft delete an entity.\"\"\"
                entity = await self.repository.get_by_id(id)
                if not entity:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail=f"Entity with id {id} not found"
                    )
                await self.repository.soft_delete(entity)

            async def hard_delete(self, id: int) -> None:
                \"\"\"Permanently delete an entity.\"\"\"
                entity = await self.repository.get_by_id(id)
                if not entity:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail=f"Entity with id {id} not found"
                    )
                await self.repository.delete(entity)

            async def restore(self, id: int) -> ResponseSchemaType:
                \"\"\"Restore a soft-deleted entity.\"\"\"
                # Include inactive to find soft-deleted
                entities = await self.repository.get_all(include_inactive=True)
                entity = next((e for e in entities if e.id == id), None)
                if not entity:
                    raise HTTPException(
                        status_code=status.HTTP_404_NOT_FOUND,
                        detail=f"Entity with id {id} not found"
                    )
                restored = await self.repository.restore(entity)
                return self.response_schema.model_validate(restored)
        """;
    }

    /**
     * Generates a service for a specific entity.
     *
     * @param table the SQL table
     * @return the service.py content
     */
    public String generate(SqlTable table) {
        String className = table.getEntityName();
        String varName = typeMapper.toSnakeCase(className);

        return """
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.models.%s import %s
        from app.schemas.%s import %sCreate, %sUpdate, %sResponse
        from app.repositories.%s_repository import %sRepository
        from app.services.base_service import BaseService


        class %sService(BaseService[%s, %sCreate, %sUpdate, %sResponse]):
            \"\"\"%s service with business logic.\"\"\"

            def __init__(self, db: AsyncSession):
                repository = %sRepository(db)
                super().__init__(repository, %sResponse)

            # Add custom business logic methods here
            # Example:
            # async def do_something_special(self, id: int) -> %sResponse:
            #     entity = await self.repository.get_by_id(id)
            #     # Business logic...
            #     return %sResponse.model_validate(entity)


        def get_%s_service(db: AsyncSession) -> %sService:
            \"\"\"Dependency injection for %sService.\"\"\"
            return %sService(db)
        """
                .formatted(
                        varName, className, varName, className, className, className, varName,
                        className, className, className, className, className, className, className,
                        className, className, className, className, varName, className, className,
                        className);
    }
}
