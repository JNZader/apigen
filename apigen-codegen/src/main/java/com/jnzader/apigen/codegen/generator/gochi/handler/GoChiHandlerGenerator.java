package com.jnzader.apigen.codegen.generator.gochi.handler;

import com.jnzader.apigen.codegen.generator.gochi.GoChiOptions;
import com.jnzader.apigen.codegen.generator.gochi.GoChiTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates HTTP handlers for Go/Chi router. */
public class GoChiHandlerGenerator {

    private final GoChiTypeMapper typeMapper;
    private final String moduleName;
    private final GoChiOptions options;

    public GoChiHandlerGenerator(
            GoChiTypeMapper typeMapper, String moduleName, GoChiOptions options) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
        this.options = options;
    }

    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String handlerName = entityName + "Handler";
        String serviceName = entityName + "Service";

        sb.append("package handler\n\n");

        sb.append("import (\n");
        sb.append("\t\"encoding/json\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"log/slog\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"strconv\"\n\n");
        sb.append("\t\"github.com/go-chi/chi/v5\"\n");
        sb.append("\t\"github.com/go-playground/validator/v10\"\n\n");
        sb.append("\t\"").append(moduleName).append("/internal/dto\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/service\"\n");
        sb.append(")\n\n");

        // Handler struct
        sb.append("// ")
                .append(handlerName)
                .append(" handles HTTP requests for ")
                .append(entityName)
                .append(".\n");
        sb.append("type ").append(handlerName).append(" struct {\n");
        sb.append("\tservice  service.").append(serviceName).append("\n");
        sb.append("\tvalidate *validator.Validate\n");
        sb.append("\tlogger   *slog.Logger\n");
        sb.append("}\n\n");

        // Constructor
        sb.append("// New")
                .append(handlerName)
                .append(" creates a new ")
                .append(handlerName)
                .append(".\n");
        sb.append("func New").append(handlerName).append("(svc service.").append(serviceName);
        sb.append(", validate *validator.Validate, logger *slog.Logger) *")
                .append(handlerName)
                .append(" {\n");
        sb.append("\treturn &").append(handlerName).append("{\n");
        sb.append("\t\tservice:  svc,\n");
        sb.append("\t\tvalidate: validate,\n");
        sb.append("\t\tlogger:   logger.With(\"handler\", \"")
                .append(entityName.toLowerCase())
                .append("\"),\n");
        sb.append("\t}\n");
        sb.append("}\n\n");

        // GetByID handler
        generateGetByID(sb, entityName, handlerName);

        // GetAll handler
        generateGetAll(sb, entityName, handlerName);

        // Create handler
        generateCreate(sb, entityName, handlerName);

        // Update handler
        generateUpdate(sb, entityName, handlerName);

        // Delete handler
        generateDelete(sb, entityName, handlerName);

        return sb.toString();
    }

    /** Generates the shared helpers.go file with common utility functions. */
    public String generateHelpers() {
        StringBuilder sb = new StringBuilder();

        sb.append("package handler\n\n");

        sb.append("import (\n");
        sb.append("\t\"encoding/json\"\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"net/http\"\n\n");
        sb.append("\t\"github.com/go-playground/validator/v10\"\n\n");
        sb.append("\t\"").append(moduleName).append("/internal/dto\"\n");
        sb.append(")\n\n");

        sb.append("// writeJSON writes a JSON response.\n");
        sb.append("func writeJSON(w http.ResponseWriter, status int, data any) {\n");
        sb.append("\tw.Header().Set(\"Content-Type\", \"application/json\")\n");
        sb.append("\tw.WriteHeader(status)\n");
        sb.append("\t_ = json.NewEncoder(w).Encode(data)\n");
        sb.append("}\n\n");

        sb.append("// writeError writes an error response.\n");
        sb.append("func writeError(w http.ResponseWriter, status int, message string) {\n");
        sb.append("\twriteJSON(w, status, dto.NewErrorResponse(status, message))\n");
        sb.append("}\n\n");

        sb.append("// writeValidationError writes a validation error response.\n");
        sb.append("func writeValidationError(w http.ResponseWriter, err error) {\n");
        sb.append("\tvar validationErrors validator.ValidationErrors\n");
        sb.append("\tif errors.As(err, &validationErrors) {\n");
        sb.append("\t\tdetails := make(map[string]string)\n");
        sb.append("\t\tfor _, fieldErr := range validationErrors {\n");
        sb.append("\t\t\tdetails[fieldErr.Field()] = fieldErr.Tag()\n");
        sb.append("\t\t}\n");
        sb.append(
                "\t\twriteJSON(w, http.StatusBadRequest,"
                        + " dto.NewErrorResponse(http.StatusBadRequest, \"validation"
                        + " failed\").WithDetails(details))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n");
        sb.append("\twriteError(w, http.StatusBadRequest, \"validation failed\")\n");
        sb.append("}\n");

        return sb.toString();
    }

    private void generateGetByID(StringBuilder sb, String entityName, String handlerName) {
        sb.append("// GetByID handles GET /").append(entityName.toLowerCase()).append("s/{id}\n");
        sb.append("func (h *")
                .append(handlerName)
                .append(") GetByID(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tidStr := chi.URLParam(r, \"id\")\n");
        sb.append("\tid, err := strconv.ParseInt(idStr, 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\th.logger.Warn(\"invalid id parameter\", \"id\", idStr, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusBadRequest, \"invalid id parameter\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.GetByID(r.Context(), id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\twriteError(w, http.StatusNotFound, \"")
                .append(entityName.toLowerCase())
                .append(" not found\")\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\th.logger.Error(\"failed to get ")
                .append(entityName.toLowerCase())
                .append("\", \"id\", id, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusInternalServerError, \"internal server error\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\twriteJSON(w, http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateGetAll(StringBuilder sb, String entityName, String handlerName) {
        sb.append("// GetAll handles GET /").append(entityName.toLowerCase()).append("s\n");
        sb.append("func (h *")
                .append(handlerName)
                .append(") GetAll(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tpage, _ := strconv.Atoi(r.URL.Query().Get(\"page\"))\n");
        sb.append("\tpageSize, _ := strconv.Atoi(r.URL.Query().Get(\"page_size\"))\n");
        sb.append("\tparams := dto.NewPaginationParams(page, pageSize)\n\n");
        sb.append("\tresponse, err := h.service.GetAll(r.Context(), params)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\th.logger.Error(\"failed to get ")
                .append(entityName.toLowerCase())
                .append("s\", \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusInternalServerError, \"internal server error\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\twriteJSON(w, http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateCreate(StringBuilder sb, String entityName, String handlerName) {
        sb.append("// Create handles POST /").append(entityName.toLowerCase()).append("s\n");
        sb.append("func (h *")
                .append(handlerName)
                .append(") Create(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tvar req dto.Create").append(entityName).append("Request\n");
        sb.append("\tif err := json.NewDecoder(r.Body).Decode(&req); err != nil {\n");
        sb.append("\t\th.logger.Warn(\"invalid request body\", \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusBadRequest, \"invalid request body\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tif err := h.validate.Struct(&req); err != nil {\n");
        sb.append("\t\th.logger.Warn(\"validation failed\", \"error\", err)\n");
        sb.append("\t\twriteValidationError(w, err)\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.Create(r.Context(), &req)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\th.logger.Error(\"failed to create ")
                .append(entityName.toLowerCase())
                .append("\", \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusInternalServerError, \"internal server error\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\twriteJSON(w, http.StatusCreated, response)\n");
        sb.append("}\n\n");
    }

    private void generateUpdate(StringBuilder sb, String entityName, String handlerName) {
        sb.append("// Update handles PUT /").append(entityName.toLowerCase()).append("s/{id}\n");
        sb.append("func (h *")
                .append(handlerName)
                .append(") Update(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tidStr := chi.URLParam(r, \"id\")\n");
        sb.append("\tid, err := strconv.ParseInt(idStr, 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\th.logger.Warn(\"invalid id parameter\", \"id\", idStr, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusBadRequest, \"invalid id parameter\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tvar req dto.Update").append(entityName).append("Request\n");
        sb.append("\tif err := json.NewDecoder(r.Body).Decode(&req); err != nil {\n");
        sb.append("\t\th.logger.Warn(\"invalid request body\", \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusBadRequest, \"invalid request body\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tif err := h.validate.Struct(&req); err != nil {\n");
        sb.append("\t\th.logger.Warn(\"validation failed\", \"error\", err)\n");
        sb.append("\t\twriteValidationError(w, err)\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.Update(r.Context(), id, &req)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\twriteError(w, http.StatusNotFound, \"")
                .append(entityName.toLowerCase())
                .append(" not found\")\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\th.logger.Error(\"failed to update ")
                .append(entityName.toLowerCase())
                .append("\", \"id\", id, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusInternalServerError, \"internal server error\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\twriteJSON(w, http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateDelete(StringBuilder sb, String entityName, String handlerName) {
        sb.append("// Delete handles DELETE /").append(entityName.toLowerCase()).append("s/{id}\n");
        sb.append("func (h *")
                .append(handlerName)
                .append(") Delete(w http.ResponseWriter, r *http.Request) {\n");
        sb.append("\tidStr := chi.URLParam(r, \"id\")\n");
        sb.append("\tid, err := strconv.ParseInt(idStr, 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\th.logger.Warn(\"invalid id parameter\", \"id\", idStr, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusBadRequest, \"invalid id parameter\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tif err := h.service.Delete(r.Context(), id); err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\twriteError(w, http.StatusNotFound, \"")
                .append(entityName.toLowerCase())
                .append(" not found\")\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\th.logger.Error(\"failed to delete ")
                .append(entityName.toLowerCase())
                .append("\", \"id\", id, \"error\", err)\n");
        sb.append("\t\twriteError(w, http.StatusInternalServerError, \"internal server error\")\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tw.WriteHeader(http.StatusNoContent)\n");
        sb.append("}\n");
    }
}
