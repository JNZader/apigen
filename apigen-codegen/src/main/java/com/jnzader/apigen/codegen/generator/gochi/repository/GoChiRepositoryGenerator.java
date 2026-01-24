package com.jnzader.apigen.codegen.generator.gochi.repository;

import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;
import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlColumn;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Generates repository layer using pgx (raw SQL, no ORM). */
@SuppressWarnings("java:S1068") // Options field reserved for future feature flags
public class GoChiRepositoryGenerator {

    private final GoChiTypeMapper typeMapper;
    private final String moduleName;
    private final GoChiOptions options;

    public GoChiRepositoryGenerator(
            GoChiTypeMapper typeMapper, String moduleName, GoChiOptions options) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
        this.options = options;
    }

    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String tableName = table.getName();
        String repoName = entityName + "Repository";

        // Get columns for SQL generation
        List<SqlColumn> columns = getNonBaseColumns(table);
        List<String> columnNames =
                columns.stream().map(c -> c.getName().toLowerCase(Locale.ROOT)).toList();

        sb.append("package repository\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"fmt\"\n\n");
        sb.append("\t\"").append(moduleName).append("/internal/model\"\n");
        sb.append("\t\"github.com/jackc/pgx/v5\"\n");
        sb.append("\t\"github.com/jackc/pgx/v5/pgxpool\"\n");
        sb.append(")\n\n");

        // Error variables
        sb.append("var (\n");
        sb.append("\t// Err")
                .append(entityName)
                .append("NotFound is returned when entity is not found.\n");
        sb.append("\tErr")
                .append(entityName)
                .append("NotFound = errors.New(\"")
                .append(entityName.toLowerCase(Locale.ROOT))
                .append(" not found\")\n");
        sb.append(")\n\n");

        // Interface
        sb.append("// ").append(repoName).append(" defines the repository interface.\n");
        sb.append("type ").append(repoName).append(" interface {\n");
        sb.append("\tFindByID(ctx context.Context, id int64) (*model.")
                .append(entityName)
                .append(", error)\n");
        sb.append("\tFindAll(ctx context.Context, offset, limit int) ([]model.")
                .append(entityName)
                .append(", int64, error)\n");
        sb.append("\tCreate(ctx context.Context, m *model.").append(entityName).append(") error\n");
        sb.append("\tUpdate(ctx context.Context, m *model.").append(entityName).append(") error\n");
        sb.append("\tDelete(ctx context.Context, id int64) error\n");
        sb.append("\tHardDelete(ctx context.Context, id int64) error\n");
        sb.append("}\n\n");

        // Implementation
        String implName = typeMapper.toUnexportedName(entityName) + "Repository";
        sb.append("type ").append(implName).append(" struct {\n");
        sb.append("\tdb *pgxpool.Pool\n");
        sb.append("}\n\n");

        // Constructor
        sb.append("// New")
                .append(repoName)
                .append(" creates a new ")
                .append(repoName)
                .append(".\n");
        sb.append("func New")
                .append(repoName)
                .append("(db *pgxpool.Pool) ")
                .append(repoName)
                .append(" {\n");
        sb.append("\treturn &").append(implName).append("{db: db}\n");
        sb.append("}\n\n");

        // FindByID
        sb.append("// FindByID retrieves an entity by ID.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") FindByID(ctx context.Context, id int64) (*model.")
                .append(entityName)
                .append(", error) {\n");
        sb.append("\tquery := `\n");
        sb.append("\t\tSELECT id, ")
                .append(String.join(", ", columnNames))
                .append(", created_at, updated_at, deleted_at\n");
        sb.append("\t\tFROM ").append(tableName).append("\n");
        sb.append("\t\tWHERE id = $1 AND deleted_at IS NULL\n");
        sb.append("\t`\n\n");
        sb.append("\tvar m model.").append(entityName).append("\n");
        sb.append("\terr := r.db.QueryRow(ctx, query, id).Scan(\n");
        sb.append("\t\t&m.ID,\n");
        for (String col : columnNames) {
            sb.append("\t\t&m.").append(typeMapper.toExportedName(col)).append(",\n");
        }
        sb.append("\t\t&m.CreatedAt,\n");
        sb.append("\t\t&m.UpdatedAt,\n");
        sb.append("\t\t&m.DeletedAt,\n");
        sb.append("\t)\n\n");
        sb.append("\tif errors.Is(err, pgx.ErrNoRows) {\n");
        sb.append("\t\treturn nil, Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, fmt.Errorf(\"query error: %w\", err)\n");
        sb.append("\t}\n\n");
        sb.append("\treturn &m, nil\n");
        sb.append("}\n\n");

        // FindAll
        sb.append("// FindAll retrieves all entities with pagination.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") FindAll(ctx context.Context, offset, limit int) ([]model.")
                .append(entityName)
                .append(", int64, error) {\n");

        sb.append("\t// Count total\n");
        sb.append("\tcountQuery := `SELECT COUNT(*) FROM ")
                .append(tableName)
                .append(" WHERE deleted_at IS NULL`\n");
        sb.append("\tvar total int64\n");
        sb.append("\tif err := r.db.QueryRow(ctx, countQuery).Scan(&total); err != nil {\n");
        sb.append("\t\treturn nil, 0, fmt.Errorf(\"count error: %w\", err)\n");
        sb.append("\t}\n\n");

        sb.append("\t// Query items\n");
        sb.append("\tquery := `\n");
        sb.append("\t\tSELECT id, ")
                .append(String.join(", ", columnNames))
                .append(", created_at, updated_at, deleted_at\n");
        sb.append("\t\tFROM ").append(tableName).append("\n");
        sb.append("\t\tWHERE deleted_at IS NULL\n");
        sb.append("\t\tORDER BY id DESC\n");
        sb.append("\t\tLIMIT $1 OFFSET $2\n");
        sb.append("\t`\n\n");

        sb.append("\trows, err := r.db.Query(ctx, query, limit, offset)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn nil, 0, fmt.Errorf(\"query error: %w\", err)\n");
        sb.append("\t}\n");
        sb.append("\tdefer rows.Close()\n\n");

        sb.append("\tvar items []model.").append(entityName).append("\n");
        sb.append("\tfor rows.Next() {\n");
        sb.append("\t\tvar m model.").append(entityName).append("\n");
        sb.append("\t\tif err := rows.Scan(\n");
        sb.append("\t\t\t&m.ID,\n");
        for (String col : columnNames) {
            sb.append("\t\t\t&m.").append(typeMapper.toExportedName(col)).append(",\n");
        }
        sb.append("\t\t\t&m.CreatedAt,\n");
        sb.append("\t\t\t&m.UpdatedAt,\n");
        sb.append("\t\t\t&m.DeletedAt,\n");
        sb.append("\t\t); err != nil {\n");
        sb.append("\t\t\treturn nil, 0, fmt.Errorf(\"scan error: %w\", err)\n");
        sb.append("\t\t}\n");
        sb.append("\t\titems = append(items, m)\n");
        sb.append("\t}\n\n");

        sb.append("\treturn items, total, nil\n");
        sb.append("}\n\n");

        // Create
        sb.append("// Create inserts a new entity.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Create(ctx context.Context, m *model.")
                .append(entityName)
                .append(") error {\n");
        sb.append("\tquery := `\n");
        sb.append("\t\tINSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columnNames))
                .append(", created_at, updated_at)\n");
        sb.append("\t\tVALUES (");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append("$").append(i + 1);
            if (i < columnNames.size() - 1) sb.append(", ");
        }
        sb.append(", NOW(), NOW())\n");
        sb.append("\t\tRETURNING id, created_at, updated_at\n");
        sb.append("\t`\n\n");

        sb.append("\terr := r.db.QueryRow(ctx, query,\n");
        for (String col : columnNames) {
            sb.append("\t\tm.").append(typeMapper.toExportedName(col)).append(",\n");
        }
        sb.append("\t).Scan(&m.ID, &m.CreatedAt, &m.UpdatedAt)\n\n");

        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn fmt.Errorf(\"insert error: %w\", err)\n");
        sb.append("\t}\n\n");
        sb.append("\treturn nil\n");
        sb.append("}\n\n");

        // Update
        sb.append("// Update updates an existing entity.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Update(ctx context.Context, m *model.")
                .append(entityName)
                .append(") error {\n");
        sb.append("\tquery := `\n");
        sb.append("\t\tUPDATE ").append(tableName).append("\n");
        sb.append("\t\tSET ");
        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(columnNames.get(i)).append(" = $").append(i + 1);
            if (i < columnNames.size() - 1) sb.append(", ");
        }
        sb.append(", updated_at = NOW()\n");
        sb.append("\t\tWHERE id = $")
                .append(columnNames.size() + 1)
                .append(" AND deleted_at IS NULL\n");
        sb.append("\t\tRETURNING updated_at\n");
        sb.append("\t`\n\n");

        sb.append("\terr := r.db.QueryRow(ctx, query,\n");
        for (String col : columnNames) {
            sb.append("\t\tm.").append(typeMapper.toExportedName(col)).append(",\n");
        }
        sb.append("\t\tm.ID,\n");
        sb.append("\t).Scan(&m.UpdatedAt)\n\n");

        sb.append("\tif errors.Is(err, pgx.ErrNoRows) {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn fmt.Errorf(\"update error: %w\", err)\n");
        sb.append("\t}\n\n");
        sb.append("\treturn nil\n");
        sb.append("}\n\n");

        // Soft Delete
        sb.append("// Delete soft-deletes an entity.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") Delete(ctx context.Context, id int64) error {\n");
        sb.append("\tquery := `\n");
        sb.append("\t\tUPDATE ")
                .append(tableName)
                .append(" SET deleted_at = NOW() WHERE id = $1 AND deleted_at IS NULL\n");
        sb.append("\t`\n\n");
        sb.append("\tresult, err := r.db.Exec(ctx, query, id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn fmt.Errorf(\"delete error: %w\", err)\n");
        sb.append("\t}\n\n");
        sb.append("\tif result.RowsAffected() == 0 {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n\n");
        sb.append("\treturn nil\n");
        sb.append("}\n\n");

        // Hard Delete
        sb.append("// HardDelete permanently deletes an entity.\n");
        sb.append("func (r *")
                .append(implName)
                .append(") HardDelete(ctx context.Context, id int64) error {\n");
        sb.append("\tquery := `DELETE FROM ").append(tableName).append(" WHERE id = $1`\n\n");
        sb.append("\tresult, err := r.db.Exec(ctx, query, id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\treturn fmt.Errorf(\"hard delete error: %w\", err)\n");
        sb.append("\t}\n\n");
        sb.append("\tif result.RowsAffected() == 0 {\n");
        sb.append("\t\treturn Err").append(entityName).append("NotFound\n");
        sb.append("\t}\n\n");
        sb.append("\treturn nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    private List<SqlColumn> getNonBaseColumns(SqlTable table) {
        List<SqlColumn> result = new ArrayList<>();
        for (SqlColumn col : table.getColumns()) {
            String lower = col.getName().toLowerCase(Locale.ROOT);
            if (!lower.equals("id")
                    && !lower.equals("created_at")
                    && !lower.equals("updated_at")
                    && !lower.equals("deleted_at")) {
                result.add(col);
            }
        }
        return result;
    }

    public String generateDatabaseConnection() {
        return """
        package database

        import (
        	"context"
        	"fmt"
        	"time"

        	"%s/internal/config"
        	"github.com/jackc/pgx/v5/pgxpool"
        )

        // NewPostgres creates a new PostgreSQL connection pool.
        func NewPostgres(cfg *config.Config) (*pgxpool.Pool, error) {
        	dsn := fmt.Sprintf(
        		"host=%%s port=%%d user=%%s password=%%s dbname=%%s sslmode=%%s",
        		cfg.Database.Host,
        		cfg.Database.Port,
        		cfg.Database.User,
        		cfg.Database.Password,
        		cfg.Database.Name,
        		cfg.Database.SSLMode,
        	)

        	poolConfig, err := pgxpool.ParseConfig(dsn)
        	if err != nil {
        		return nil, fmt.Errorf("failed to parse config: %%w", err)
        	}

        	poolConfig.MaxConns = int32(cfg.Database.MaxOpenConns)
        	poolConfig.MinConns = int32(cfg.Database.MaxIdleConns)
        	poolConfig.MaxConnLifetime = time.Hour

        	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
        	defer cancel()

        	pool, err := pgxpool.NewWithConfig(ctx, poolConfig)
        	if err != nil {
        		return nil, fmt.Errorf("failed to create pool: %%w", err)
        	}

        	if err := pool.Ping(ctx); err != nil {
        		return nil, fmt.Errorf("failed to ping: %%w", err)
        	}

        	return pool, nil
        }
        """
                .formatted(moduleName);
    }

    public String generateSqliteConnection() {
        return """
        package database

        import (
        	"database/sql"
        	"fmt"

        	_ "github.com/mattn/go-sqlite3"
        )

        // NewSQLite creates a new SQLite connection.
        func NewSQLite(path string) (*sql.DB, error) {
        	db, err := sql.Open("sqlite3", path)
        	if err != nil {
        		return nil, fmt.Errorf("failed to open sqlite: %%w", err)
        	}

        	if err := db.Ping(); err != nil {
        		return nil, fmt.Errorf("failed to ping sqlite: %%w", err)
        	}

        	// Enable WAL mode for better concurrency
        	if _, err := db.Exec("PRAGMA journal_mode=WAL"); err != nil {
        		return nil, fmt.Errorf("failed to enable WAL: %%w", err)
        	}

        	return db, nil
        }
        """;
    }

    public String generateMigrations(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        sb.append("package database\n\n");
        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"fmt\"\n");
        sb.append("\t\"log/slog\"\n\n");
        sb.append("\t\"github.com/jackc/pgx/v5/pgxpool\"\n");
        sb.append(")\n\n");

        sb.append("// RunMigrations runs database migrations.\n");
        sb.append("func RunMigrations(db *pgxpool.Pool) error {\n");
        sb.append("\tctx := context.Background()\n\n");

        sb.append("\tmigrations := []string{\n");

        // Generate CREATE TABLE for each entity
        for (SqlTable table : schema.getEntityTables()) {
            sb.append("\t\t`\n");
            sb.append("\t\tCREATE TABLE IF NOT EXISTS ").append(table.getName()).append(" (\n");
            sb.append("\t\t\tid BIGSERIAL PRIMARY KEY,\n");

            for (SqlColumn col : table.getColumns()) {
                String lower = col.getName().toLowerCase(Locale.ROOT);
                if (lower.equals("id")
                        || lower.equals("created_at")
                        || lower.equals("updated_at")
                        || lower.equals("deleted_at")) {
                    continue;
                }
                sb.append("\t\t\t")
                        .append(col.getName().toLowerCase(Locale.ROOT))
                        .append(" ")
                        .append(mapToPostgresType(col));
                if (!col.isNullable()) {
                    sb.append(" NOT NULL");
                }
                if (col.isUnique()) {
                    sb.append(" UNIQUE");
                }
                sb.append(",\n");
            }

            sb.append("\t\t\tcreated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n");
            sb.append("\t\t\tupdated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),\n");
            sb.append("\t\t\tdeleted_at TIMESTAMPTZ\n");
            sb.append("\t\t);\n");
            sb.append("\t\t`,\n");

            // Index on deleted_at
            sb.append("\t\t`CREATE INDEX IF NOT EXISTS idx_")
                    .append(table.getName())
                    .append("_deleted_at ON ")
                    .append(table.getName())
                    .append("(deleted_at)`,\n");
        }

        sb.append("\t}\n\n");

        sb.append("\tfor i, migration := range migrations {\n");
        sb.append("\t\tif _, err := db.Exec(ctx, migration); err != nil {\n");
        sb.append("\t\t\treturn fmt.Errorf(\"migration %d failed: %w\", i, err)\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n\n");

        sb.append("\tslog.Info(\"migrations completed\")\n");
        sb.append("\treturn nil\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String mapToPostgresType(SqlColumn column) {
        return switch (column.getJavaType()) {
            case "String" -> {
                int len =
                        column.getLength() != null && column.getLength() > 0
                                ? column.getLength()
                                : 255;
                yield len > 10000 ? "TEXT" : "VARCHAR(" + len + ")";
            }
            case "Integer" -> "INTEGER";
            case "Long" -> "BIGINT";
            case "Boolean" -> "BOOLEAN";
            case "BigDecimal" -> {
                int precision =
                        column.getPrecision() != null && column.getPrecision() > 0
                                ? column.getPrecision()
                                : 19;
                int scale =
                        column.getScale() != null && column.getScale() > 0 ? column.getScale() : 2;
                yield "NUMERIC(" + precision + "," + scale + ")";
            }
            case "Double" -> "DOUBLE PRECISION";
            case "Float" -> "REAL";
            case "LocalDate" -> "DATE";
            case "LocalDateTime", "Instant", "Timestamp" -> "TIMESTAMPTZ";
            case "LocalTime" -> "TIME";
            case "UUID" -> "UUID";
            case "byte[]" -> "BYTEA";
            default -> "TEXT";
        };
    }
}
