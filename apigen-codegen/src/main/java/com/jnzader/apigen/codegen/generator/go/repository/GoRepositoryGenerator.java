package com.jnzader.apigen.codegen.generator.go.repository;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates repository structs and methods for data access with GORM for Go/Gin. */
@SuppressWarnings({
    "java:S2479",
    "java:S1192"
}) // Literal tabs intentional for Go code; duplicate strings for templates
public class GoRepositoryGenerator {

    private final GoTypeMapper typeMapper;
    private final String moduleName;

    public GoRepositoryGenerator(GoTypeMapper typeMapper, String moduleName) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
    }

    /**
     * Generates a repository for a table.
     *
     * @param table the SQL table
     * @return the generated Go code
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String repoName = entityName + "Repository";
        String varName = typeMapper.toUnexportedName(table.getEntityName());

        sb.append("package repository\n\n");

        // Imports
        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\n");
        sb.append("\t\"").append(moduleName).append("/internal/models\"\n");
        sb.append("\t\"gorm.io/gorm\"\n");
        sb.append(")\n\n");

        // Errors
        sb.append("var (\n");
        sb.append("\t// Err")
                .append(entityName)
                .append("NotFound is returned when ")
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
                .append(repoName)
                .append(" defines the interface for ")
                .append(varName)
                .append(" data access.\n");
        sb.append("type ").append(repoName).append(" interface {\n");
        sb.append("\tFindByID(ctx context.Context, id int64) (*models.")
                .append(entityName)
                .append(", error)\n");
        sb.append("\tFindAll(ctx context.Context, page, size int) ([]models.")
                .append(entityName)
                .append(", int64, error)\n");
        sb.append("\tCreate(ctx context.Context, entity *models.")
                .append(entityName)
                .append(") error\n");
        sb.append("\tUpdate(ctx context.Context, entity *models.")
                .append(entityName)
                .append(") error\n");
        sb.append("\tDelete(ctx context.Context, id int64) error\n");
        sb.append("\tExistsByID(ctx context.Context, id int64) (bool, error)\n");
        sb.append("}\n\n");

        // Implementation struct
        String implName = typeMapper.toUnexportedName(repoName) + "Impl";
        sb.append("type ").append(implName).append(" struct {\n");
        sb.append("\tdb *gorm.DB\n");
        sb.append("}\n\n");

        // Constructor
        sb.append("// New")
                .append(repoName)
                .append(" creates a new ")
                .append(repoName)
                .append(".\n");
        sb.append("func New")
                .append(repoName)
                .append("(db *gorm.DB) ")
                .append(repoName)
                .append(" {\n");
        sb.append("\treturn &").append(implName).append("{db: db}\n");
        sb.append("}\n\n");

        // FindByID
        sb.append("// FindByID retrieves a ").append(varName).append(" by its ID.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") FindByID(ctx context.Context, id int64) ");
        sb.append("(*models.").append(entityName).append(", error) {\n");
        sb.append("\tvar entity models.").append(entityName).append("\n");
        sb.append("\tresult := r.db.WithContext(ctx).First(&entity, id)\n");
        sb.append("\tif result.Error != nil {\n");
        sb.append("\t\tif errors.Is(result.Error, gorm.ErrRecordNotFound) {\n");
        sb.append("\t\t\treturn nil, Err").append(entityName).append("NotFound\n");
        sb.append("\t\t}\n");
        sb.append("\t\treturn nil, result.Error\n");
        sb.append("\t}\n");
        sb.append("\treturn &entity, nil\n");
        sb.append("}\n\n");

        // FindAll
        sb.append("// FindAll retrieves all ")
                .append(typeMapper.pluralize(varName))
                .append(" with pagination.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") FindAll(ctx context.Context, page, size int) ");
        sb.append("([]models.").append(entityName).append(", int64, error) {\n");
        sb.append("\tvar entities []models.").append(entityName).append("\n");
        sb.append("\tvar total int64\n\n");
        sb.append("\t// Count total\n");
        sb.append("\tif err := r.db.WithContext(ctx).Model(&models.")
                .append(entityName)
                .append("{}).Count(&total).Error; err != nil {\n");
        sb.append("\t\treturn nil, 0, err\n");
        sb.append("\t}\n\n");
        sb.append("\t// Get page\n");
        sb.append("\toffset := page * size\n");
        sb.append("\tresult := r.db.WithContext(ctx).\n");
        sb.append("\t\tOffset(offset).\n");
        sb.append("\t\tLimit(size).\n");
        sb.append("\t\tOrder(\"id ASC\").\n");
        sb.append("\t\tFind(&entities)\n\n");
        sb.append("\tif result.Error != nil {\n");
        sb.append("\t\treturn nil, 0, result.Error\n");
        sb.append("\t}\n\n");
        sb.append("\treturn entities, total, nil\n");
        sb.append("}\n\n");

        // Create
        sb.append("// Create saves a new ").append(varName).append(".\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Create(ctx context.Context, entity *models.")
                .append(entityName)
                .append(") error {\n");
        sb.append("\treturn r.db.WithContext(ctx).Create(entity).Error\n");
        sb.append("}\n\n");

        // Update
        sb.append("// Update saves changes to an existing ").append(varName).append(".\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Update(ctx context.Context, entity *models.")
                .append(entityName)
                .append(") error {\n");
        sb.append("\treturn r.db.WithContext(ctx).Save(entity).Error\n");
        sb.append("}\n\n");

        // Delete (soft delete)
        sb.append("// Delete soft-deletes a ").append(varName).append(" by its ID.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Delete(ctx context.Context, id int64) error {\n");
        sb.append("\tresult := r.db.WithContext(ctx).Delete(&models.")
                .append(entityName)
                .append("{}, id)\n");
        sb.append("\tif result.Error != nil {\n");
        sb.append("\t\treturn result.Error\n");
        sb.append("\t}\n");
        sb.append("\tif result.RowsAffected == 0 {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n");
        sb.append("\treturn nil\n");
        sb.append("}\n\n");

        // ExistsByID
        sb.append("// ExistsByID checks if a ").append(varName).append(" exists by its ID.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") ExistsByID(ctx context.Context, id int64) ");
        sb.append("(bool, error) {\n");
        sb.append("\tvar count int64\n");
        sb.append("\terr := r.db.WithContext(ctx).\n");
        sb.append("\t\tModel(&models.").append(entityName).append("{}).\n");
        sb.append("\t\tWhere(\"id = ?\", id).\n");
        sb.append("\t\tCount(&count).Error\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn false, err\n");
        sb.append("\t}\n");
        sb.append("\treturn count > 0, nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates a base repository with common methods.
     *
     * @return the generated Go code for base repository
     */
    public String generateBaseRepository() {
        return """
        package repository

        import (
        \t"context"

        \t"gorm.io/gorm"
        )

        // BaseRepository provides common database operations.
        type BaseRepository[T any] interface {
        \tFindByID(ctx context.Context, id int64) (*T, error)
        \tFindAll(ctx context.Context, page, size int) ([]T, int64, error)
        \tCreate(ctx context.Context, entity *T) error
        \tUpdate(ctx context.Context, entity *T) error
        \tDelete(ctx context.Context, id int64) error
        \tExistsByID(ctx context.Context, id int64) (bool, error)
        }

        // GormRepository provides a generic GORM repository implementation.
        type GormRepository[T any] struct {
        \tDB *gorm.DB
        }

        // NewGormRepository creates a new generic GORM repository.
        func NewGormRepository[T any](db *gorm.DB) *GormRepository[T] {
        \treturn &GormRepository[T]{DB: db}
        }

        // WithTx returns a new repository with a transaction.
        func (r *GormRepository[T]) WithTx(tx *gorm.DB) *GormRepository[T] {
        \treturn &GormRepository[T]{DB: tx}
        }
        """;
    }
}
