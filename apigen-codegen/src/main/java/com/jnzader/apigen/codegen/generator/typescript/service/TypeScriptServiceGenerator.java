package com.jnzader.apigen.codegen.generator.typescript.service;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates NestJS service classes for business logic.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Service classes with dependency injection
 *   <li>CRUD operations with DTO mapping
 *   <li>Error handling with NestJS exceptions
 * </ul>
 */
public class TypeScriptServiceGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptServiceGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates a service class for a table.
     *
     * @param table the SQL table
     * @return the service.ts content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String entityKebab = typeMapper.toKebabCase(className);
        String varName = typeMapper.toCamelCase(className);

        // Imports
        sb.append("import { Injectable, NotFoundException } from '@nestjs/common';\n");
        sb.append("import { ")
                .append(className)
                .append("Repository } from '../repositories/")
                .append(entityKebab)
                .append(".repository';\n");
        sb.append("import { ")
                .append(className)
                .append(" } from '../entities/")
                .append(entityKebab)
                .append(".entity';\n");
        sb.append("import { Create")
                .append(className)
                .append("Dto } from '../dto/create-")
                .append(entityKebab)
                .append(".dto';\n");
        sb.append("import { Update")
                .append(className)
                .append("Dto } from '../dto/update-")
                .append(entityKebab)
                .append(".dto';\n");
        sb.append("import { ")
                .append(className)
                .append("ResponseDto } from '../dto/")
                .append(entityKebab)
                .append("-response.dto';\n");
        sb.append("import { PaginatedResponseDto } from '../dto/paginated-response.dto';\n");
        sb.append("\n");

        // Class definition
        sb.append("@Injectable()\n");
        sb.append("export class ").append(className).append("Service {\n");

        // Constructor
        sb.append("  constructor(\n");
        sb.append("    private readonly repository: ").append(className).append("Repository,\n");
        sb.append("  ) {}\n\n");

        // findById
        sb.append("  /**\n");
        sb.append("   * Find a ").append(varName).append(" by ID.\n");
        sb.append("   * @throws NotFoundException if not found\n");
        sb.append("   */\n");
        sb.append("  async findById(id: number): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    const entity = await this.repository.findById(id);\n");
        sb.append("    if (!entity) {\n");
        sb.append("      throw new NotFoundException(`")
                .append(className)
                .append(" with id ${id} not found`);\n");
        sb.append("    }\n");
        sb.append("    return this.toResponseDto(entity);\n");
        sb.append("  }\n\n");

        // findAll
        sb.append("  /**\n");
        sb.append("   * Find all ")
                .append(typeMapper.pluralize(varName))
                .append(" with pagination.\n");
        sb.append("   */\n");
        sb.append("  async findAll(\n");
        sb.append("    page = 0,\n");
        sb.append("    size = 10,\n");
        sb.append("    includeInactive = false,\n");
        sb.append("  ): Promise<PaginatedResponseDto<")
                .append(className)
                .append("ResponseDto>> {\n");
        sb.append(
                "    const [entities, total] = await this.repository.findAll(page, size,"
                        + " includeInactive);\n");
        sb.append("    const items = entities.map((e) => this.toResponseDto(e));\n");
        sb.append("    return new PaginatedResponseDto(items, total, page, size);\n");
        sb.append("  }\n\n");

        // create
        sb.append("  /**\n");
        sb.append("   * Create a new ").append(varName).append(".\n");
        sb.append("   */\n");
        sb.append("  async create(dto: Create")
                .append(className)
                .append("Dto): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    const entity = await this.repository.create(dto as Partial<")
                .append(className)
                .append(">);\n");
        sb.append("    return this.toResponseDto(entity);\n");
        sb.append("  }\n\n");

        // update
        sb.append("  /**\n");
        sb.append("   * Update an existing ").append(varName).append(".\n");
        sb.append("   * @throws NotFoundException if not found\n");
        sb.append("   */\n");
        sb.append("  async update(\n");
        sb.append("    id: number,\n");
        sb.append("    dto: Update").append(className).append("Dto,\n");
        sb.append("  ): Promise<").append(className).append("ResponseDto> {\n");
        sb.append("    // Verify entity exists\n");
        sb.append("    await this.findById(id);\n");
        sb.append("\n");
        sb.append("    const entity = await this.repository.update(id, dto as Partial<")
                .append(className)
                .append(">);\n");
        sb.append("    if (!entity) {\n");
        sb.append("      throw new NotFoundException(`")
                .append(className)
                .append(" with id ${id} not found`);\n");
        sb.append("    }\n");
        sb.append("    return this.toResponseDto(entity);\n");
        sb.append("  }\n\n");

        // delete (soft)
        sb.append("  /**\n");
        sb.append("   * Soft delete a ").append(varName).append(".\n");
        sb.append("   * @throws NotFoundException if not found\n");
        sb.append("   */\n");
        sb.append("  async delete(id: number, deletedBy?: string): Promise<void> {\n");
        sb.append("    // Verify entity exists\n");
        sb.append("    await this.findById(id);\n");
        sb.append("\n");
        sb.append("    const success = await this.repository.softDelete(id, deletedBy);\n");
        sb.append("    if (!success) {\n");
        sb.append("      throw new NotFoundException(`")
                .append(className)
                .append(" with id ${id} not found`);\n");
        sb.append("    }\n");
        sb.append("  }\n\n");

        // restore
        sb.append("  /**\n");
        sb.append("   * Restore a soft-deleted ").append(varName).append(".\n");
        sb.append("   * @throws NotFoundException if not found\n");
        sb.append("   */\n");
        sb.append("  async restore(id: number): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    const entity = await this.repository.restore(id);\n");
        sb.append("    if (!entity) {\n");
        sb.append("      throw new NotFoundException(`")
                .append(className)
                .append(" with id ${id} not found`);\n");
        sb.append("    }\n");
        sb.append("    return this.toResponseDto(entity);\n");
        sb.append("  }\n\n");

        // forceDelete
        sb.append("  /**\n");
        sb.append("   * Permanently delete a ").append(varName).append(".\n");
        sb.append("   * @throws NotFoundException if not found\n");
        sb.append("   */\n");
        sb.append("  async forceDelete(id: number): Promise<void> {\n");
        sb.append("    const success = await this.repository.forceDelete(id);\n");
        sb.append("    if (!success) {\n");
        sb.append("      throw new NotFoundException(`")
                .append(className)
                .append(" with id ${id} not found`);\n");
        sb.append("    }\n");
        sb.append("  }\n\n");

        // toResponseDto helper
        sb.append("  /**\n");
        sb.append("   * Map entity to response DTO.\n");
        sb.append("   */\n");
        sb.append("  private toResponseDto(entity: ")
                .append(className)
                .append("): ")
                .append(className)
                .append("ResponseDto {\n");
        sb.append("    return {\n");
        sb.append("      id: entity.id,\n");
        sb.append("      activo: entity.activo,\n");
        sb.append("      createdAt: entity.createdAt,\n");
        sb.append("      updatedAt: entity.updatedAt,\n");
        sb.append("      createdBy: entity.createdBy,\n");
        sb.append("      updatedBy: entity.updatedBy,\n");
        sb.append("      ...entity,\n");
        sb.append("    } as ").append(className).append("ResponseDto;\n");
        sb.append("  }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
