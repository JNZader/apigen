package com.jnzader.apigen.codegen.generator.gochi.service;

import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;
import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.Locale;

/** Generates service layer for Go/Chi. */
@SuppressWarnings({
    "java:S1068",
    "java:S1192",
    "java:S2479"
}) // S1068: options reserved; S1192: template strings; S2479: tabs for Go formatting
public class GoChiServiceGenerator {

    private final GoChiTypeMapper typeMapper;
    private final String moduleName;
    private final GoChiOptions options;

    public GoChiServiceGenerator(
            GoChiTypeMapper typeMapper, String moduleName, GoChiOptions options) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
        this.options = options;
    }

    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String serviceName = entityName + "Service";
        String repoName = entityName + "Repository";

        sb.append("package service\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n\n");
        sb.append("\t\"").append(moduleName).append("/internal/dto\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/model\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/repository\"\n");
        sb.append(")\n\n");

        // Errors
        sb.append("var (\n");
        sb.append("\t// Err")
                .append(entityName)
                .append("NotFound indicates entity was not found.\n");
        sb.append("\tErr")
                .append(entityName)
                .append("NotFound = errors.New(\"")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(" not found\")\n");
        sb.append(")\n\n");

        // Interface
        sb.append("// ").append(serviceName).append(" defines the service interface.\n");
        sb.append("type ").append(serviceName).append(" interface {\n");
        sb.append("\tGetByID(ctx context.Context, id int64) (*dto.")
                .append(entityName)
                .append("Response, error)\n");
        sb.append(
                        "\tGetAll(ctx context.Context, params dto.PaginationParams)"
                                + " (*dto.PaginatedResponse[dto.")
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

        // Implementation
        String implName = typeMapper.toUnexportedName(entityName) + "Service";
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
        sb.append("// GetByID retrieves an entity by ID.\n");
        sb.append("func (s *")
                .append(implName)
                .append(") GetByID(ctx context.Context, id int64) (*dto.")
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
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // GetAll
        sb.append("// GetAll retrieves all entities with pagination.\n");
        sb.append("func (s *")
                .append(implName)
                .append(
                        ") GetAll(ctx context.Context, params dto.PaginationParams)"
                                + " (*dto.PaginatedResponse[dto.")
                .append(entityName)
                .append("Response], error) {\n");
        sb.append(
                "\tentities, total, err := s.repo.FindAll(ctx, params.Offset(),"
                        + " params.PageSize)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n\n");
        sb.append("\tresponses := make([]dto.")
                .append(entityName)
                .append("Response, len(entities))\n");
        sb.append("\tfor i, entity := range entities {\n");
        sb.append("\t\tresponses[i] = *to").append(entityName).append("Response(&entity)\n");
        sb.append("\t}\n\n");
        sb.append(
                "\treturn dto.NewPaginatedResponse(responses, params.Page, params.PageSize, total),"
                        + " nil\n");
        sb.append("}\n\n");

        // Create
        sb.append("// Create creates a new entity.\n");
        sb.append("func (s *")
                .append(implName)
                .append(") Create(ctx context.Context, req *dto.Create")
                .append(entityName)
                .append("Request) (*dto.")
                .append(entityName)
                .append("Response, error) {\n");
        sb.append("\tentity := to").append(entityName).append("Model(req)\n");
        sb.append("\tif err := s.repo.Create(ctx, entity); err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // Update
        sb.append("// Update updates an existing entity.\n");
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
        sb.append("\tupdate").append(entityName).append("Model(entity, req)\n");
        sb.append("\tif err := s.repo.Update(ctx, entity); err != nil {\n");
        sb.append("\t\treturn nil, err\n");
        sb.append("\t}\n");
        sb.append("\treturn to").append(entityName).append("Response(entity), nil\n");
        sb.append("}\n\n");

        // Delete
        sb.append("// Delete soft-deletes an entity.\n");
        sb.append("func (s *")
                .append(implName)
                .append(") Delete(ctx context.Context, id int64) error {\n");
        sb.append("\terr := s.repo.Delete(ctx, id)\n");
        sb.append("\tif errors.Is(err, repository.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n");
        sb.append("\treturn err\n");
        sb.append("}\n\n");

        // Mapper: toModel
        sb.append("// to").append(entityName).append("Model converts create request to model.\n");
        sb.append("func to")
                .append(entityName)
                .append("Model(req *dto.Create")
                .append(entityName)
                .append("Request) *model.")
                .append(entityName)
                .append(" {\n");
        sb.append("\treturn &model.").append(entityName).append("{\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            sb.append("\t\t").append(fieldName).append(": req.").append(fieldName).append(",\n");
        }
        sb.append("\t}\n");
        sb.append("}\n\n");

        // Mapper: updateModel
        sb.append("// update")
                .append(entityName)
                .append("Model updates model from update request.\n");
        sb.append("func update")
                .append(entityName)
                .append("Model(m *model.")
                .append(entityName)
                .append(", req *dto.Update")
                .append(entityName)
                .append("Request) {\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            sb.append("\tif req.").append(fieldName).append(" != nil {\n");
            sb.append("\t\tm.").append(fieldName).append(" = ");
            if (!column.isNullable()) {
                sb.append("*");
            }
            sb.append("req.").append(fieldName).append("\n");
            sb.append("\t}\n");
        }
        sb.append("}\n\n");

        // Mapper: toResponse
        sb.append("// to").append(entityName).append("Response converts model to response DTO.\n");
        sb.append("func to")
                .append(entityName)
                .append("Response(m *model.")
                .append(entityName)
                .append(") *dto.")
                .append(entityName)
                .append("Response {\n");
        sb.append("\treturn &dto.").append(entityName).append("Response{\n");
        sb.append("\t\tID:        m.ID,\n");
        for (SqlColumn column : table.getColumns()) {
            if (isBaseField(column.getName()) || column.isPrimaryKey()) {
                continue;
            }
            String fieldName = typeMapper.toExportedName(column.getName());
            sb.append("\t\t").append(fieldName).append(": m.").append(fieldName).append(",\n");
        }
        sb.append("\t\tCreatedAt: m.CreatedAt,\n");
        sb.append("\t\tUpdatedAt: m.UpdatedAt,\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }

    private boolean isBaseField(String columnName) {
        String lower = columnName.toLowerCase(Locale.ROOT);
        return lower.equals("id")
                || lower.equals("created_at")
                || lower.equals("updated_at")
                || lower.equals("deleted_at");
    }
}
