package com.jnzader.apigen.codegen.generator.typescript.repository;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates TypeORM repository classes for NestJS projects.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Custom repository with common query methods
 *   <li>Soft delete support
 *   <li>Pagination support
 * </ul>
 */
public class TypeScriptRepositoryGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptRepositoryGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates a repository class for a table.
     *
     * @param table the SQL table
     * @return the repository.ts content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String entityKebab = typeMapper.toKebabCase(className);
        String varName = typeMapper.toCamelCase(className);

        // Imports
        sb.append("import { Injectable } from '@nestjs/common';\n");
        sb.append("import { InjectRepository } from '@nestjs/typeorm';\n");
        sb.append("import { Repository, FindOptionsWhere, IsNull, Not } from 'typeorm';\n");
        sb.append("import { ")
                .append(className)
                .append(" } from '../entities/")
                .append(entityKebab)
                .append(".entity';\n");
        sb.append("\n");

        // Class definition
        sb.append("@Injectable()\n");
        sb.append("export class ").append(className).append("Repository {\n");

        // Constructor
        sb.append("  constructor(\n");
        sb.append("    @InjectRepository(").append(className).append(")\n");
        sb.append("    private readonly repository: Repository<").append(className).append(">,\n");
        sb.append("  ) {}\n\n");

        // findById
        sb.append("  /**\n");
        sb.append("   * Find an active entity by ID.\n");
        sb.append("   */\n");
        sb.append("  async findById(id: number): Promise<")
                .append(className)
                .append(" | null> {\n");
        sb.append("    return this.repository.findOne({\n");
        sb.append("      where: { id, activo: true, deletedAt: IsNull() } as FindOptionsWhere<")
                .append(className)
                .append(">,\n");
        sb.append("    });\n");
        sb.append("  }\n\n");

        // findAll with pagination
        sb.append("  /**\n");
        sb.append("   * Find all active entities with pagination.\n");
        sb.append("   */\n");
        sb.append("  async findAll(\n");
        sb.append("    page: number,\n");
        sb.append("    size: number,\n");
        sb.append("    includeInactive = false,\n");
        sb.append("  ): Promise<[").append(className).append("[], number]> {\n");
        sb.append("    const where: FindOptionsWhere<")
                .append(className)
                .append("> = { deletedAt: IsNull() };\n");
        sb.append("    if (!includeInactive) {\n");
        sb.append("      where.activo = true;\n");
        sb.append("    }\n");
        sb.append("\n");
        sb.append("    return this.repository.findAndCount({\n");
        sb.append("      where,\n");
        sb.append("      skip: page * size,\n");
        sb.append("      take: size,\n");
        sb.append("      order: { createdAt: 'DESC' } as any,\n");
        sb.append("    });\n");
        sb.append("  }\n\n");

        // create
        sb.append("  /**\n");
        sb.append("   * Create a new entity.\n");
        sb.append("   */\n");
        sb.append("  async create(data: Partial<")
                .append(className)
                .append(">): Promise<")
                .append(className)
                .append("> {\n");
        sb.append("    const entity = this.repository.create(data);\n");
        sb.append("    return this.repository.save(entity);\n");
        sb.append("  }\n\n");

        // update
        sb.append("  /**\n");
        sb.append("   * Update an existing entity.\n");
        sb.append("   */\n");
        sb.append("  async update(\n");
        sb.append("    id: number,\n");
        sb.append("    data: Partial<").append(className).append(">,\n");
        sb.append("  ): Promise<").append(className).append(" | null> {\n");
        sb.append("    await this.repository.update(id, data);\n");
        sb.append("    return this.findById(id);\n");
        sb.append("  }\n\n");

        // softDelete
        sb.append("  /**\n");
        sb.append("   * Soft delete an entity (sets deletedAt and activo=false).\n");
        sb.append("   */\n");
        sb.append("  async softDelete(id: number, deletedBy?: string): Promise<boolean> {\n");
        sb.append("    const result = await this.repository.update(id, {\n");
        sb.append("      activo: false,\n");
        sb.append("      deletedAt: new Date(),\n");
        sb.append("      deletedBy: deletedBy || null,\n");
        sb.append("    } as any);\n");
        sb.append("    return (result.affected ?? 0) > 0;\n");
        sb.append("  }\n\n");

        // restore
        sb.append("  /**\n");
        sb.append("   * Restore a soft-deleted entity.\n");
        sb.append("   */\n");
        sb.append("  async restore(id: number): Promise<").append(className).append(" | null> {\n");
        sb.append("    await this.repository.update(id, {\n");
        sb.append("      activo: true,\n");
        sb.append("      deletedAt: null,\n");
        sb.append("      deletedBy: null,\n");
        sb.append("    } as any);\n");
        sb.append("    return this.findById(id);\n");
        sb.append("  }\n\n");

        // forceDelete
        sb.append("  /**\n");
        sb.append("   * Permanently delete an entity.\n");
        sb.append("   */\n");
        sb.append("  async forceDelete(id: number): Promise<boolean> {\n");
        sb.append("    const result = await this.repository.delete(id);\n");
        sb.append("    return (result.affected ?? 0) > 0;\n");
        sb.append("  }\n\n");

        // count
        sb.append("  /**\n");
        sb.append("   * Count active entities.\n");
        sb.append("   */\n");
        sb.append("  async count(includeInactive = false): Promise<number> {\n");
        sb.append("    const where: FindOptionsWhere<")
                .append(className)
                .append("> = { deletedAt: IsNull() };\n");
        sb.append("    if (!includeInactive) {\n");
        sb.append("      where.activo = true;\n");
        sb.append("    }\n");
        sb.append("    return this.repository.count({ where });\n");
        sb.append("  }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
