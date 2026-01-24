package com.jnzader.apigen.codegen.generator.gochi.router;

import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;
import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/** Generates Chi router configuration. */
public class GoChiRouterGenerator {

    private final GoChiTypeMapper typeMapper;
    private final String moduleName;
    private final GoChiOptions options;

    public GoChiRouterGenerator(
            GoChiTypeMapper typeMapper, String moduleName, GoChiOptions options) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
        this.options = options;
    }

    /** Generates the router from a schema. */
    public String generate(SqlSchema schema) {
        return generateRouter(schema.getEntityTables());
    }

    /** Generates the router from a list of tables. */
    public String generateRouter(List<SqlTable> tables) {
        StringBuilder sb = new StringBuilder();

        sb.append("package router\n\n");

        sb.append("import (\n");
        sb.append("\t\"log/slog\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/go-chi/chi/v5\"\n");
        sb.append("\t\"github.com/go-chi/chi/v5/middleware\"\n");
        sb.append("\t\"github.com/go-chi/cors\"\n");
        sb.append("\t\"github.com/go-playground/validator/v10\"\n");
        sb.append("\t\"github.com/jackc/pgx/v5/pgxpool\"\n\n");
        sb.append("\t\"").append(moduleName).append("/internal/handler\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/repository\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/service\"\n");

        if (options.useJwt()) {
            sb.append("\tmw \"").append(moduleName).append("/internal/middleware\"\n");
        }

        sb.append(")\n\n");

        // NewRouter function
        sb.append("// NewRouter creates and configures a new Chi router.\n");
        sb.append("func NewRouter(pool *pgxpool.Pool, logger *slog.Logger) *chi.Mux {\n");
        sb.append("\tr := chi.NewRouter()\n\n");

        // Middleware stack
        sb.append("\t// Middleware stack\n");
        sb.append("\tr.Use(middleware.RequestID)\n");
        sb.append("\tr.Use(middleware.RealIP)\n");
        sb.append("\tr.Use(middleware.Logger)\n");
        sb.append("\tr.Use(middleware.Recoverer)\n");
        sb.append("\tr.Use(middleware.Timeout(60 * time.Second))\n\n");

        // CORS configuration
        sb.append("\t// CORS configuration\n");
        sb.append("\tr.Use(cors.Handler(cors.Options{\n");
        sb.append("\t\tAllowedOrigins:   []string{\"*\"},\n");
        sb.append(
                "\t\tAllowedMethods:   []string{\"GET\", \"POST\", \"PUT\", \"DELETE\","
                        + " \"OPTIONS\"},\n");
        sb.append(
                "\t\tAllowedHeaders:   []string{\"Accept\", \"Authorization\", \"Content-Type\","
                        + " \"X-Request-ID\"},\n");
        sb.append("\t\tExposedHeaders:   []string{\"Link\", \"X-Request-ID\"},\n");
        sb.append("\t\tAllowCredentials: true,\n");
        sb.append("\t\tMaxAge:           300,\n");
        sb.append("\t}))\n\n");

        // Validator
        sb.append("\tvalidate := validator.New()\n\n");

        // Health check
        sb.append("\t// Health check endpoint\n");
        sb.append("\tr.Get(\"/health\", handler.HealthCheck(pool))\n\n");

        // API routes
        sb.append("\t// API routes\n");
        sb.append("\tr.Route(\"/api/v1\", func(r chi.Router) {\n");

        if (options.useJwt()) {
            sb.append("\t\t// Public routes\n");
            sb.append("\t\tr.Group(func(r chi.Router) {\n");
            sb.append("\t\t\t// Add public endpoints here if needed\n");
            sb.append("\t\t})\n\n");
            sb.append("\t\t// Protected routes\n");
            sb.append("\t\tr.Group(func(r chi.Router) {\n");
            sb.append("\t\t\tr.Use(mw.JWTAuth)\n\n");
        }

        // Generate routes for each table
        for (SqlTable table : tables) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String resourcePath = table.getName().toLowerCase();
            String handlerVar = typeMapper.toUnexportedName(entityName) + "Handler";
            String repoVar = typeMapper.toUnexportedName(entityName) + "Repo";
            String svcVar = typeMapper.toUnexportedName(entityName) + "Svc";

            String indent = options.useJwt() ? "\t\t\t" : "\t\t";

            sb.append(indent).append("// ").append(entityName).append(" routes\n");
            sb.append(indent)
                    .append(repoVar)
                    .append(" := repository.New")
                    .append(entityName)
                    .append("Repository(pool)\n");
            sb.append(indent)
                    .append(svcVar)
                    .append(" := service.New")
                    .append(entityName)
                    .append("Service(")
                    .append(repoVar)
                    .append(")\n");
            sb.append(indent)
                    .append(handlerVar)
                    .append(" := handler.New")
                    .append(entityName)
                    .append("Handler(")
                    .append(svcVar)
                    .append(", validate, logger)\n");
            sb.append(indent)
                    .append("r.Route(\"/")
                    .append(resourcePath)
                    .append("\", func(r chi.Router) {\n");
            sb.append(indent).append("\tr.Get(\"/\", ").append(handlerVar).append(".GetAll)\n");
            sb.append(indent).append("\tr.Post(\"/\", ").append(handlerVar).append(".Create)\n");
            sb.append(indent)
                    .append("\tr.Get(\"/{id}\", ")
                    .append(handlerVar)
                    .append(".GetByID)\n");
            sb.append(indent).append("\tr.Put(\"/{id}\", ").append(handlerVar).append(".Update)\n");
            sb.append(indent)
                    .append("\tr.Delete(\"/{id}\", ")
                    .append(handlerVar)
                    .append(".Delete)\n");
            sb.append(indent).append("})\n\n");
        }

        if (options.useJwt()) {
            sb.append("\t\t})\n");
        }

        sb.append("\t})\n\n");
        sb.append("\treturn r\n");
        sb.append("}\n");

        return sb.toString();
    }

    public String generateHealthHandler() {
        StringBuilder sb = new StringBuilder();

        sb.append("package handler\n\n");

        sb.append("import (\n");
        sb.append("\t\"context\"\n");
        sb.append("\t\"encoding/json\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"time\"\n\n");
        sb.append("\t\"github.com/jackc/pgx/v5/pgxpool\"\n");
        sb.append(")\n\n");

        sb.append("// HealthResponse represents the health check response.\n");
        sb.append("type HealthResponse struct {\n");
        sb.append("\tStatus    string `json:\"status\"`\n");
        sb.append("\tTimestamp string `json:\"timestamp\"`\n");
        sb.append("\tDatabase  string `json:\"database\"`\n");
        sb.append("}\n\n");

        sb.append("// HealthCheck returns a health check handler.\n");
        sb.append("func HealthCheck(pool *pgxpool.Pool) http.HandlerFunc {\n");
        sb.append("\treturn func(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\t\tctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)\n");
        sb.append("\t\tdefer cancel()\n\n");
        sb.append("\t\tdbStatus := \"up\"\n");
        sb.append("\t\tif err := pool.Ping(ctx); err != nil {\n");
        sb.append("\t\t\tdbStatus = \"down\"\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tstatus := \"healthy\"\n");
        sb.append("\t\thttpStatus := http.StatusOK\n");
        sb.append("\t\tif dbStatus == \"down\" {\n");
        sb.append("\t\t\tstatus = \"unhealthy\"\n");
        sb.append("\t\t\thttpStatus = http.StatusServiceUnavailable\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tresponse := HealthResponse{\n");
        sb.append("\t\t\tStatus:    status,\n");
        sb.append("\t\t\tTimestamp: time.Now().UTC().Format(time.RFC3339),\n");
        sb.append("\t\t\tDatabase:  dbStatus,\n");
        sb.append("\t\t}\n\n");
        sb.append("\t\tw.Header().Set(\"Content-Type\", \"application/json\")\n");
        sb.append("\t\tw.WriteHeader(httpStatus)\n");
        sb.append("\t\t_ = json.NewEncoder(w).Encode(response)\n");
        sb.append("\t}\n");
        sb.append("}\n");

        return sb.toString();
    }
}
