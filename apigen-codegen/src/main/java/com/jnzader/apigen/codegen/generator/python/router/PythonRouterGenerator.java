package com.jnzader.apigen.codegen.generator.python.router;

import com.jnzader.apigen.codegen.generator.python.PythonTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates FastAPI router classes for Python projects.
 *
 * <p>Routers define API endpoints and handle HTTP requests.
 */
public class PythonRouterGenerator {

    private final PythonTypeMapper typeMapper;

    public PythonRouterGenerator() {
        this.typeMapper = new PythonTypeMapper();
    }

    /**
     * Generates a FastAPI router for a specific entity.
     *
     * @param table the SQL table
     * @return the router.py content
     */
    public String generate(SqlTable table) {
        String className = table.getEntityName();
        String varName = typeMapper.toSnakeCase(className);
        String routePath = varName + "s"; // Pluralize for route

        StringBuilder sb = new StringBuilder();

        // Imports
        sb.append("from fastapi import APIRouter, Depends, HTTPException, status, Query\n");
        sb.append("from sqlalchemy.ext.asyncio import AsyncSession\n");
        sb.append("\n");
        sb.append("from app.core.database import get_db\n");
        sb.append("from app.schemas.").append(varName).append(" import (\n");
        sb.append("    ").append(className).append("Create,\n");
        sb.append("    ").append(className).append("Update,\n");
        sb.append("    ").append(className).append("Response,\n");
        sb.append("    ").append(className).append("ListResponse,\n");
        sb.append(")\n");
        sb.append("from app.services.").append(varName).append("_service import (\n");
        sb.append("    ").append(className).append("Service,\n");
        sb.append("    get_").append(varName).append("_service,\n");
        sb.append(")\n");
        sb.append("\n");

        // Router definition
        sb.append("router = APIRouter(\n");
        sb.append("    prefix=\"/api/v1/").append(routePath).append("\",\n");
        sb.append("    tags=[\"").append(className).append("s\"],\n");
        sb.append("    responses={404: {\"description\": \"Not found\"}},\n");
        sb.append(")\n\n\n");

        // Helper to get service
        sb.append("async def _get_service(db: AsyncSession = Depends(get_db)) -> ")
                .append(className)
                .append("Service:\n");
        sb.append("    return get_").append(varName).append("_service(db)\n\n\n");

        // GET all (paginated)
        sb.append("@router.get(\n");
        sb.append("    \"/\",\n");
        sb.append("    response_model=").append(className).append("ListResponse,\n");
        sb.append("    summary=\"Get all ").append(varName).append("s\",\n");
        sb.append("    description=\"Retrieve a paginated list of ")
                .append(varName)
                .append("s.\",\n");
        sb.append(")\n");
        sb.append("async def get_all(\n");
        sb.append("    page: int = Query(0, ge=0, description=\"Page number (0-indexed)\"),\n");
        sb.append("    size: int = Query(10, ge=1, le=100, description=\"Page size\"),\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("ListResponse:\n");
        sb.append("    \"\"\"Get all ").append(varName).append("s with pagination.\"\"\"\n");
        sb.append("    result = await service.get_all(page=page, size=size)\n");
        sb.append("    return ").append(className).append("ListResponse(**result)\n\n\n");

        // GET by ID
        sb.append("@router.get(\n");
        sb.append("    \"/{id}\",\n");
        sb.append("    response_model=").append(className).append("Response,\n");
        sb.append("    summary=\"Get ").append(varName).append(" by ID\",\n");
        sb.append("    description=\"Retrieve a single ")
                .append(varName)
                .append(" by its ID.\",\n");
        sb.append(")\n");
        sb.append("async def get_by_id(\n");
        sb.append("    id: int,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("Response:\n");
        sb.append("    \"\"\"Get ").append(varName).append(" by ID.\"\"\"\n");
        sb.append("    return await service.get_by_id(id)\n\n\n");

        // POST create
        sb.append("@router.post(\n");
        sb.append("    \"/\",\n");
        sb.append("    response_model=").append(className).append("Response,\n");
        sb.append("    status_code=status.HTTP_201_CREATED,\n");
        sb.append("    summary=\"Create ").append(varName).append("\",\n");
        sb.append("    description=\"Create a new ").append(varName).append(".\",\n");
        sb.append(")\n");
        sb.append("async def create(\n");
        sb.append("    ").append(varName).append("_in: ").append(className).append("Create,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("Response:\n");
        sb.append("    \"\"\"Create a new ").append(varName).append(".\"\"\"\n");
        sb.append("    return await service.create(").append(varName).append("_in)\n\n\n");

        // PUT update
        sb.append("@router.put(\n");
        sb.append("    \"/{id}\",\n");
        sb.append("    response_model=").append(className).append("Response,\n");
        sb.append("    summary=\"Update ").append(varName).append("\",\n");
        sb.append("    description=\"Update an existing ").append(varName).append(".\",\n");
        sb.append(")\n");
        sb.append("async def update(\n");
        sb.append("    id: int,\n");
        sb.append("    ").append(varName).append("_in: ").append(className).append("Update,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("Response:\n");
        sb.append("    \"\"\"Update ").append(varName).append(" by ID.\"\"\"\n");
        sb.append("    return await service.update(id, ").append(varName).append("_in)\n\n\n");

        // PATCH partial update (same as PUT for simplicity)
        sb.append("@router.patch(\n");
        sb.append("    \"/{id}\",\n");
        sb.append("    response_model=").append(className).append("Response,\n");
        sb.append("    summary=\"Partial update ").append(varName).append("\",\n");
        sb.append("    description=\"Partially update an existing ")
                .append(varName)
                .append(".\",\n");
        sb.append(")\n");
        sb.append("async def partial_update(\n");
        sb.append("    id: int,\n");
        sb.append("    ").append(varName).append("_in: ").append(className).append("Update,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("Response:\n");
        sb.append("    \"\"\"Partially update ").append(varName).append(" by ID.\"\"\"\n");
        sb.append("    return await service.update(id, ").append(varName).append("_in)\n\n\n");

        // DELETE soft delete
        sb.append("@router.delete(\n");
        sb.append("    \"/{id}\",\n");
        sb.append("    status_code=status.HTTP_204_NO_CONTENT,\n");
        sb.append("    summary=\"Delete ").append(varName).append("\",\n");
        sb.append("    description=\"Soft delete a ").append(varName).append(".\",\n");
        sb.append(")\n");
        sb.append("async def delete(\n");
        sb.append("    id: int,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> None:\n");
        sb.append("    \"\"\"Soft delete ").append(varName).append(" by ID.\"\"\"\n");
        sb.append("    await service.delete(id)\n\n\n");

        // POST restore
        sb.append("@router.post(\n");
        sb.append("    \"/{id}/restore\",\n");
        sb.append("    response_model=").append(className).append("Response,\n");
        sb.append("    summary=\"Restore ").append(varName).append("\",\n");
        sb.append("    description=\"Restore a soft-deleted ").append(varName).append(".\",\n");
        sb.append(")\n");
        sb.append("async def restore(\n");
        sb.append("    id: int,\n");
        sb.append("    service: ").append(className).append("Service = Depends(_get_service),\n");
        sb.append(") -> ").append(className).append("Response:\n");
        sb.append("    \"\"\"Restore soft-deleted ").append(varName).append(".\"\"\"\n");
        sb.append("    return await service.restore(id)\n");

        return sb.toString();
    }
}
