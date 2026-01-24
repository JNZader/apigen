package com.jnzader.apigen.codegen.generator.go.service;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates service structs and methods for business logic for Go/Gin. */
public class GoServiceGenerator {

    private final GoTypeMapper typeMapper;
    private final String moduleName;

    public GoServiceGenerator(GoTypeMapper typeMapper, String moduleName) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
    }

    /**
     * Generates a service for a table.
     *
     * @param table the SQL table
     * @return the generated Go code
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String serviceName = entityName + "Service";
        String repoName = entityName + "Repository";
        String varName = typeMapper.toUnexportedName(table.getEntityName());

        sb.append("package service\n\n");

        // Imports
        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\n");
        sb.append("\t\"").append(moduleName).append("/internal/dto\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/models\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/repository\"\n");
        sb.append(")\n\n");

        // Errors
        sb.append("var (\n");
        sb.append("\t// Err")
                .append(entityName)
                .append("NotFound is returned when the ")
                .append(varName)
                .append(" is not found.\n");
        sb.append("\tErr")
                .append(entityName)
                .append("NotFound = errors.New(\"")
                .append(varName)
                .append(" not found\")\n");
        sb.append(")\n\n");

        // Interface
        sb.append("// ")
                .append(serviceName)
                .append(" defines the interface for ")
                .append(varName)
                .append(" business logic.\n");
        sb.append("type ").append(serviceName).append(" interface {\n");
        sb.append("\tGetByID(ctx context.Context, id int64) (*dto.")
                .append(entityName)
                .append("Response, error)\n");
        sb.append("\tGetAll(ctx context.Context, page, size int) (*dto.PaginatedResponse[dto.")
                .append(entityName)
                .append("Response], error)\n");
        sb.append("\tCreate(ctx context.Context, req *dto.Create")
                .append(entityName)
                .append("Request) (*dto.")
                .append(entityName)
                .append("Response, error)\n");
        sb.append("\tUpdate(ctx context.Context, id int64, req *dto.Update")
                .append(entityName)
                .append("Request) (*dto.")
                .append(entityName)
                .append("Response, error)\n");
        sb.append("\tDelete(ctx context.Context, id int64) error\n");
        sb.append("}\n\n");

        // Implementation struct
        String implName = typeMapper.toUnexportedName(serviceName) + "Impl";
        sb.append("type ").append(implName).append(" struct {\n");
        sb.append("\trepo repository.").append(repoName).append("\n");
        sb.append("}\n\n");

        // Constructor
        sb.append("// New")
                .append(serviceName)
                .append(" creates a new ")
                .append(serviceName)
                .append(".\n");
        sb.append("func New")
                .append(serviceName)
                .append("(repo repository.")
                .append(repoName)
                .append(") ")
                .append(serviceName)
                .append(" {\n");
        sb.append("\treturn &").append(implName).append("{repo: repo}\n");
        sb.append("}\n\n");

        // GetByID
        sb.append("// GetByID retrieves a ").append(varName).append(" by its ID.\n");
        sb.append("func (s *").append(implName).append(") GetByID(ctx context.Context, id int64) ");
        sb.append("(*dto.").append(entityName).append("Response, error) {\n");
        sb.append("\tentity, err := s.repo.FindByID(ctx, id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, repository.Err")
                .append(entityName)
                .append("NotFound) {\n");
        sb.append("\t\t\treturn nil, Err").append(entityName).append("NotFound\n");
        sb.append("\t\t}\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // GetAll
        sb.append("// GetAll retrieves all ")
                .append(typeMapper.pluralize(varName))
                .append(" with pagination.\n");
        sb.append("func (s *")
                .append(implName)
                .append(") GetAll(ctx context.Context, page, size int) ");
        sb.append("(*dto.PaginatedResponse[dto.")
                .append(entityName)
                .append("Response], error) {\n");
        sb.append("\tentities, total, err := s.repo.FindAll(ctx, page, size)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n\n");
        sb.append("\tresponses := make([]dto.")
                .append(entityName)
                .append("Response, len(entities))\n");
        sb.append("\tfor i, entity := range entities {\n");
        sb.append("\t\tresponses[i] = *to").append(entityName).append("Response(&entity)\n");
        sb.append("\t}\n\n");
        sb.append("\treturn dto.NewPaginatedResponse(responses, page, size, total), nil\n");
        sb.append("}\n\n");

        // Create
        sb.append("// Create creates a new ").append(varName).append(".\n");
        sb.append("func (s *")
                .append(implName)
                .append(") Create(ctx context.Context, req *dto.Create")
                .append(entityName)
                .append("Request) (*dto.")
                .append(entityName)
                .append("Response, error) {\n");
        sb.append("\tentity := to").append(entityName).append("Entity(req)\n");
        sb.append("\tif err := s.repo.Create(ctx, entity); err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // Update
        sb.append("// Update updates an existing ").append(varName).append(".\n");
        sb.append("func (s *")
                .append(implName)
                .append(") Update(ctx context.Context, id int64, req *dto.Update")
                .append(entityName)
                .append("Request) (*dto.")
                .append(entityName)
                .append("Response, error) {\n");
        sb.append("\tentity, err := s.repo.FindByID(ctx, id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, repository.Err")
                .append(entityName)
                .append("NotFound) {\n");
        sb.append("\t\t\treturn nil, Err").append(entityName).append("NotFound\n");
        sb.append("\t\t}\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n\n");
        sb.append("\tupdate").append(entityName).append("Entity(entity, req)\n");
        sb.append("\tif err := s.repo.Update(ctx, entity); err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // Delete
        sb.append("// Delete deletes a ").append(varName).append(" by its ID.\n");
        sb.append("func (s *")
                .append(implName)
                .append(") Delete(ctx context.Context, id int64) error {\n");
        sb.append("\terr := s.repo.Delete(ctx, id)\n");
        sb.append("\tif errors.Is(err, repository.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n");
        sb.append("\treturn err\n");
        sb.append("}\n\n");

        // Mapper functions (entity-specific to avoid redeclaration in same package)
        sb.append("// to")
                .append(entityName)
                .append("Entity converts a create request to an entity.\n");
        sb.append("func to")
                .append(entityName)
                .append("Entity(req *dto.Create")
                .append(entityName)
                .append("Request) *models.")
                .append(entityName)
                .append(" {\n");
        sb.append("\treturn &models.").append(entityName).append("{\n");
        appendEntityMappings(sb, table, "req");
        sb.append("\t}\n");
        sb.append("}\n\n");

        sb.append("// update")
                .append(entityName)
                .append("Entity updates entity fields from an update request.\n");
        sb.append("func update")
                .append(entityName)
                .append("Entity(entity *models.")
                .append(entityName)
                .append(", req *dto.Update")
                .append(entityName)
                .append("Request) {\n");
        appendUpdateMappings(sb, table);
        sb.append("}\n\n");

        sb.append("// to")
                .append(entityName)
                .append("Response converts an entity to a response DTO.\n");
        sb.append("func to")
                .append(entityName)
                .append("Response(entity *models.")
                .append(entityName)
                .append(") *dto.")
                .append(entityName)
                .append("Response {\n");
        sb.append("\treturn &dto.").append(entityName).append("Response{\n");
        sb.append("\t\tID:        entity.ID,\n");
        appendResponseMappings(sb, table);
        sb.append("\t\tCreatedAt: entity.CreatedAt,\n");
        sb.append("\t\tUpdatedAt: entity.UpdatedAt,\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }

    private void appendEntityMappings(StringBuilder sb, SqlTable table, String source) {
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            sb.append("\t\t")
                    .append(fieldName)
                    .append(": ")
                    .append(source)
                    .append(".")
                    .append(fieldName)
                    .append(",\n");
        }
    }

    private void appendUpdateMappings(StringBuilder sb, SqlTable table) {
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            // For update, fields are pointers, so check nil
            sb.append("\tif req.").append(fieldName).append(" != nil {\n");
            if (column.isNullable()) {
                sb.append("\t\tentity.")
                        .append(fieldName)
                        .append(" = req.")
                        .append(fieldName)
                        .append("\n");
            } else {
                sb.append("\t\tentity.")
                        .append(fieldName)
                        .append(" = *req.")
                        .append(fieldName)
                        .append("\n");
            }
            sb.append("\t}\n");
        }
    }

    private void appendResponseMappings(StringBuilder sb, SqlTable table) {
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            sb.append("\t\t").append(fieldName).append(": entity.").append(fieldName).append(",\n");
        }
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase();
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at")
                || lower.equals("createdat")
                || lower.equals("updatedat")
                || lower.equals("deletedat");
    }
}
