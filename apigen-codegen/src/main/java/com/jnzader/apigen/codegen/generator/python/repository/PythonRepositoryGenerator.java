package com.jnzader.apigen.codegen.generator.python.repository;

import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates repository classes for Python/FastAPI projects.
 *
 * <p>Uses SQLAlchemy async sessions for database operations.
 */
public class PythonRepositoryGenerator {

    private final PythonTypeMapper typeMapper;

    public PythonRepositoryGenerator() {
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates the base repository class.
     *
     * @return the base_repository.py content
     */
    public String generateBase() {
        return """
        from typing import Generic, TypeVar, Type
        from sqlalchemy import select, func
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.models.base import BaseModel

        ModelType = TypeVar("ModelType", bound=BaseModel)


        class BaseRepository(Generic[ModelType]):
            \"""Base repository with common CRUD operations.\"""

            def __init__(self, model: Type[ModelType], db: AsyncSession):
                self.model = model
                self.db = db

            async def get_by_id(self, id: int) -> ModelType | None:
                \"""Get entity by ID.\"""
                result = await self.db.execute(
                    select(self.model).where(
                        self.model.id == id,
                        self.model.activo == True
                    )
                )
                return result.scalar_one_or_none()

            async def get_all(
                self,
                skip: int = 0,
                limit: int = 10,
                include_inactive: bool = False
            ) -> list[ModelType]:
                \"""Get all entities with pagination.\"""
                query = select(self.model)
                if not include_inactive:
                    query = query.where(self.model.activo == True)
                query = query.offset(skip).limit(limit)
                result = await self.db.execute(query)
                return list(result.scalars().all())

            async def count(self, include_inactive: bool = False) -> int:
                \"""Count all entities.\"""
                query = select(func.count(self.model.id))
                if not include_inactive:
                    query = query.where(self.model.activo == True)
                result = await self.db.execute(query)
                return result.scalar_one()

            async def create(self, obj_in: dict) -> ModelType:
                \"""Create a new entity.\"""
                db_obj = self.model(**obj_in)
                self.db.add(db_obj)
                await self.db.commit()
                await self.db.refresh(db_obj)
                return db_obj

            async def update(self, db_obj: ModelType, obj_in: dict) -> ModelType:
                \"""Update an existing entity.\"""
                for field, value in obj_in.items():
                    if value is not None:
                        setattr(db_obj, field, value)
                await self.db.commit()
                await self.db.refresh(db_obj)
                return db_obj

            async def delete(self, db_obj: ModelType) -> None:
                \"""Hard delete an entity.\"""
                await self.db.delete(db_obj)
                await self.db.commit()

            async def soft_delete(self, db_obj: ModelType) -> ModelType:
                \"""Soft delete an entity.\"""
                from datetime import datetime
                db_obj.activo = False
                db_obj.deleted_at = datetime.utcnow()
                await self.db.commit()
                await self.db.refresh(db_obj)
                return db_obj

            async def restore(self, db_obj: ModelType) -> ModelType:
                \"""Restore a soft-deleted entity.\"""
                db_obj.activo = True
                db_obj.deleted_at = None
                db_obj.deleted_by = None
                await self.db.commit()
                await self.db.refresh(db_obj)
                return db_obj
        """;
    }

    /**
     * Generates a repository for a specific entity.
     *
     * @param table the SQL table
     * @return the repository.py content
     */
    public String generate(SqlTable table) {
        String className = table.getEntityName();
        String varName = typeMapper.toSnakeCase(className);

        return """
        from sqlalchemy import select
        from sqlalchemy.ext.asyncio import AsyncSession

        from app.models.%s import %s
        from app.repositories.base_repository import BaseRepository


        class %sRepository(BaseRepository[%s]):
            \"""%s repository with custom queries.\"""

            def __init__(self, db: AsyncSession):
                super().__init__(%s, db)

            # Add custom query methods here
            # Example:
            # async def find_by_name(self, name: str) -> %s | None:
            #     result = await self.db.execute(
            #         select(%s).where(%s.name == name)
            #     )
            #     return result.scalar_one_or_none()
        """
                .formatted(
                        varName, className, className, className, className, className, className,
                        className, className);
    }
}
