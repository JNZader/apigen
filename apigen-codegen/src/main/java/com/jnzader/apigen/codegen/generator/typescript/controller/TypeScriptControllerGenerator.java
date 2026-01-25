package com.jnzader.apigen.codegen.generator.typescript.controller;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates NestJS controller classes with REST endpoints.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Controllers with OpenAPI decorators
 *   <li>CRUD endpoints
 *   <li>Pagination and filtering support
 * </ul>
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class TypeScriptControllerGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptControllerGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates a controller class for a table.
     *
     * @param table the SQL table
     * @return the controller.ts content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String entityKebab = typeMapper.toKebabCase(className);
        String varName = typeMapper.toCamelCase(className);
        String pluralKebab = typeMapper.toKebabCase(typeMapper.pluralize(className));

        // Imports
        sb.append("import {\n");
        sb.append("  Controller,\n");
        sb.append("  Get,\n");
        sb.append("  Post,\n");
        sb.append("  Put,\n");
        sb.append("  Delete,\n");
        sb.append("  Body,\n");
        sb.append("  Param,\n");
        sb.append("  Query,\n");
        sb.append("  ParseIntPipe,\n");
        sb.append("  HttpCode,\n");
        sb.append("  HttpStatus,\n");
        sb.append("} from '@nestjs/common';\n");
        sb.append("import {\n");
        sb.append("  ApiTags,\n");
        sb.append("  ApiOperation,\n");
        sb.append("  ApiResponse,\n");
        sb.append("  ApiQuery,\n");
        sb.append("  ApiParam,\n");
        sb.append("} from '@nestjs/swagger';\n");
        sb.append("import { ")
                .append(className)
                .append("Service } from '../services/")
                .append(entityKebab)
                .append(".service';\n");
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
        sb.append("import { PaginatedResponseDto } from '../../../dto/paginated-response.dto';\n");
        sb.append("\n");

        // Class definition
        sb.append("@ApiTags('").append(pluralKebab).append("')\n");
        sb.append("@Controller('api/v1/").append(pluralKebab).append("')\n");
        sb.append("export class ").append(className).append("Controller {\n");

        // Constructor
        sb.append("  constructor(private readonly service: ")
                .append(className)
                .append("Service) {}\n\n");

        // GET all (index)
        sb.append("  /**\n");
        sb.append("   * Get all ")
                .append(typeMapper.pluralize(varName))
                .append(" with pagination.\n");
        sb.append("   */\n");
        sb.append("  @Get()\n");
        sb.append("  @ApiOperation({ summary: 'Get all ")
                .append(typeMapper.pluralize(varName))
                .append("' })\n");
        sb.append(
                "  @ApiQuery({ name: 'page', required: false, type: Number, description: 'Page"
                        + " number (0-indexed)' })\n");
        sb.append(
                "  @ApiQuery({ name: 'size', required: false, type: Number, description: 'Items per"
                        + " page' })\n");
        sb.append(
                "  @ApiQuery({ name: 'includeInactive', required: false, type: Boolean,"
                        + " description: 'Include inactive records' })\n");
        sb.append("  @ApiResponse({ status: 200, description: 'List of ")
                .append(typeMapper.pluralize(varName))
                .append("' })\n");
        sb.append("  async findAll(\n");
        sb.append("    @Query('page') page = 0,\n");
        sb.append("    @Query('size') size = 10,\n");
        sb.append("    @Query('includeInactive') includeInactive = false,\n");
        sb.append("  ): Promise<PaginatedResponseDto<")
                .append(className)
                .append("ResponseDto>> {\n");
        sb.append(
                "    return this.service.findAll(+page, +size,"
                        + " String(includeInactive) === 'true');\n");
        sb.append("  }\n\n");

        // GET by ID (show)
        sb.append("  /**\n");
        sb.append("   * Get a ").append(varName).append(" by ID.\n");
        sb.append("   */\n");
        sb.append("  @Get(':id')\n");
        sb.append("  @ApiOperation({ summary: 'Get ").append(varName).append(" by ID' })\n");
        sb.append("  @ApiParam({ name: 'id', type: Number, description: '")
                .append(className)
                .append(" ID' })\n");
        sb.append("  @ApiResponse({ status: 200, description: 'The ")
                .append(varName)
                .append("', type: ")
                .append(className)
                .append("ResponseDto })\n");
        sb.append("  @ApiResponse({ status: 404, description: '")
                .append(className)
                .append(" not found' })\n");
        sb.append("  async findById(@Param('id', ParseIntPipe) id: number): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    return this.service.findById(id);\n");
        sb.append("  }\n\n");

        // POST (store)
        sb.append("  /**\n");
        sb.append("   * Create a new ").append(varName).append(".\n");
        sb.append("   */\n");
        sb.append("  @Post()\n");
        sb.append("  @HttpCode(HttpStatus.CREATED)\n");
        sb.append("  @ApiOperation({ summary: 'Create a new ").append(varName).append("' })\n");
        sb.append("  @ApiResponse({ status: 201, description: 'The created ")
                .append(varName)
                .append("', type: ")
                .append(className)
                .append("ResponseDto })\n");
        sb.append("  @ApiResponse({ status: 400, description: 'Validation error' })\n");
        sb.append("  async create(@Body() dto: Create")
                .append(className)
                .append("Dto): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    return this.service.create(dto);\n");
        sb.append("  }\n\n");

        // PUT (update)
        sb.append("  /**\n");
        sb.append("   * Update an existing ").append(varName).append(".\n");
        sb.append("   */\n");
        sb.append("  @Put(':id')\n");
        sb.append("  @ApiOperation({ summary: 'Update ").append(varName).append("' })\n");
        sb.append("  @ApiParam({ name: 'id', type: Number, description: '")
                .append(className)
                .append(" ID' })\n");
        sb.append("  @ApiResponse({ status: 200, description: 'The updated ")
                .append(varName)
                .append("', type: ")
                .append(className)
                .append("ResponseDto })\n");
        sb.append("  @ApiResponse({ status: 404, description: '")
                .append(className)
                .append(" not found' })\n");
        sb.append("  @ApiResponse({ status: 400, description: 'Validation error' })\n");
        sb.append("  async update(\n");
        sb.append("    @Param('id', ParseIntPipe) id: number,\n");
        sb.append("    @Body() dto: Update").append(className).append("Dto,\n");
        sb.append("  ): Promise<").append(className).append("ResponseDto> {\n");
        sb.append("    return this.service.update(id, dto);\n");
        sb.append("  }\n\n");

        // DELETE (destroy - soft delete)
        sb.append("  /**\n");
        sb.append("   * Soft delete a ").append(varName).append(".\n");
        sb.append("   */\n");
        sb.append("  @Delete(':id')\n");
        sb.append("  @HttpCode(HttpStatus.NO_CONTENT)\n");
        sb.append("  @ApiOperation({ summary: 'Delete ")
                .append(varName)
                .append(" (soft delete)' })\n");
        sb.append("  @ApiParam({ name: 'id', type: Number, description: '")
                .append(className)
                .append(" ID' })\n");
        sb.append("  @ApiResponse({ status: 204, description: '")
                .append(className)
                .append(" deleted successfully' })\n");
        sb.append("  @ApiResponse({ status: 404, description: '")
                .append(className)
                .append(" not found' })\n");
        sb.append("  async delete(@Param('id', ParseIntPipe) id: number): Promise<void> {\n");
        sb.append("    return this.service.delete(id);\n");
        sb.append("  }\n\n");

        // POST restore
        sb.append("  /**\n");
        sb.append("   * Restore a soft-deleted ").append(varName).append(".\n");
        sb.append("   */\n");
        sb.append("  @Post(':id/restore')\n");
        sb.append("  @ApiOperation({ summary: 'Restore deleted ").append(varName).append("' })\n");
        sb.append("  @ApiParam({ name: 'id', type: Number, description: '")
                .append(className)
                .append(" ID' })\n");
        sb.append("  @ApiResponse({ status: 200, description: 'The restored ")
                .append(varName)
                .append("', type: ")
                .append(className)
                .append("ResponseDto })\n");
        sb.append("  @ApiResponse({ status: 404, description: '")
                .append(className)
                .append(" not found' })\n");
        sb.append("  async restore(@Param('id', ParseIntPipe) id: number): Promise<")
                .append(className)
                .append("ResponseDto> {\n");
        sb.append("    return this.service.restore(id);\n");
        sb.append("  }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
