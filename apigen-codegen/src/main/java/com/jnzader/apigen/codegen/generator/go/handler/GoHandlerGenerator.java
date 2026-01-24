package com.jnzader.apigen.codegen.generator.go.handler;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates HTTP handler functions for Gin routes for Go/Gin. */
public class GoHandlerGenerator {

    private final GoTypeMapper typeMapper;
    private final String moduleName;

    public GoHandlerGenerator(GoTypeMapper typeMapper, String moduleName) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
    }

    /**
     * Generates a handler for a table.
     *
     * @param table the SQL table
     * @return the generated Go code
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String entityName = typeMapper.toExportedName(table.getEntityName());
        String handlerName = entityName + "Handler";
        String serviceName = entityName + "Service";
        String varName = typeMapper.toUnexportedName(table.getEntityName());
        String pluralName = typeMapper.pluralize(varName);
        String pluralPath = typeMapper.toSnakeCase(typeMapper.pluralize(table.getEntityName()));

        sb.append("package handler\n\n");

        // Imports
        sb.append("import (\n");
        sb.append("\t\"errors\"\n");
        sb.append("\t\"net/http\"\n");
        sb.append("\t\"strconv\"\n");
        sb.append("\n");
        sb.append("\t\"").append(moduleName).append("/internal/dto\"\n");
        sb.append("\t\"").append(moduleName).append("/internal/service\"\n");
        sb.append("\t\"github.com/gin-gonic/gin\"\n");
        sb.append(")\n\n");

        // Handler struct
        sb.append("// ")
                .append(handlerName)
                .append(" handles HTTP requests for ")
                .append(pluralName)
                .append(".\n");
        sb.append("type ").append(handlerName).append(" struct {\n");
        sb.append("\tservice service.").append(serviceName).append("\n");
        sb.append("}\n\n");

        // Constructor
        sb.append("// New")
                .append(handlerName)
                .append(" creates a new ")
                .append(handlerName)
                .append(".\n");
        sb.append("func New")
                .append(handlerName)
                .append("(service service.")
                .append(serviceName)
                .append(") *")
                .append(handlerName)
                .append(" {\n");
        sb.append("\treturn &").append(handlerName).append("{service: service}\n");
        sb.append("}\n\n");

        // GetByID
        generateGetByID(sb, entityName, handlerName, varName, pluralPath);

        // GetAll
        generateGetAll(sb, entityName, handlerName, pluralName, pluralPath);

        // Create
        generateCreate(sb, entityName, handlerName, varName, pluralPath);

        // Update
        generateUpdate(sb, entityName, handlerName, varName, pluralPath);

        // Delete
        generateDelete(sb, entityName, handlerName, varName, pluralPath);

        return sb.toString();
    }

    private void generateGetByID(
            StringBuilder sb, String entityName, String handlerName, String varName, String path) {

        sb.append("// GetByID retrieves a ").append(varName).append(" by ID.\n");
        sb.append("//\n");
        sb.append("// @Summary      Get ").append(varName).append(" by ID\n");
        sb.append("// @Description  Retrieves a ").append(varName).append(" by its ID\n");
        sb.append("// @Tags         ").append(path).append("\n");
        sb.append("// @Accept       json\n");
        sb.append("// @Produce      json\n");
        sb.append("// @Param        id   path      int  true  \"")
                .append(entityName)
                .append(" ID\"\n");
        sb.append("// @Success      200  {object}  dto.").append(entityName).append("Response\n");
        sb.append("// @Failure      400  {object}  dto.ErrorResponse\n");
        sb.append("// @Failure      404  {object}  dto.ErrorResponse\n");
        sb.append("// @Failure      500  {object}  dto.ErrorResponse\n");
        sb.append("// @Router       /api/v1/").append(path).append("/{id} [get]\n");
        sb.append("func (h *").append(handlerName).append(") GetByID(c *gin.Context) {\n");
        sb.append("\tid, err := strconv.ParseInt(c.Param(\"id\"), 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusBadRequest, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusBadRequest, \"invalid id parameter\", c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.GetByID(c.Request.Context(), id)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\tc.JSON(http.StatusNotFound, dto.NewErrorResponse(\n");
        sb.append("\t\t\t\thttp.StatusNotFound, \"")
                .append(varName)
                .append(" not found\", c.Request.URL.Path))\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\tc.JSON(http.StatusInternalServerError, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusInternalServerError, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tc.JSON(http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateGetAll(
            StringBuilder sb,
            String entityName,
            String handlerName,
            String pluralName,
            String path) {

        sb.append("// GetAll retrieves all ").append(pluralName).append(" with pagination.\n");
        sb.append("//\n");
        sb.append("// @Summary      List ").append(pluralName).append("\n");
        sb.append("// @Description  Retrieves a paginated list of ")
                .append(pluralName)
                .append("\n");
        sb.append("// @Tags         ").append(path).append("\n");
        sb.append("// @Accept       json\n");
        sb.append("// @Produce      json\n");
        sb.append(
                "// @Param        page  query     int  false  \"Page number (0-based)\" "
                        + " default(0)\n");
        sb.append("// @Param        size  query     int  false  \"Page size\"  default(10)\n");
        sb.append("// @Success      200   {object}  dto.PaginatedResponse[dto.")
                .append(entityName)
                .append("Response]\n");
        sb.append("// @Failure      500   {object}  dto.ErrorResponse\n");
        sb.append("// @Router       /api/v1/").append(path).append(" [get]\n");
        sb.append("func (h *").append(handlerName).append(") GetAll(c *gin.Context) {\n");
        sb.append("\tpage := 0\n");
        sb.append("\tsize := 10\n\n");
        sb.append("\tif p := c.Query(\"page\"); p != \"\" {\n");
        sb.append("\t\tif parsed, err := strconv.Atoi(p); err == nil && parsed >= 0 {\n");
        sb.append("\t\t\tpage = parsed\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n");
        sb.append("\tif s := c.Query(\"size\"); s != \"\" {\n");
        sb.append(
                "\t\tif parsed, err := strconv.Atoi(s); err == nil && parsed > 0 && parsed <= 100"
                        + " {\n");
        sb.append("\t\t\tsize = parsed\n");
        sb.append("\t\t}\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.GetAll(c.Request.Context(), page, size)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusInternalServerError, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusInternalServerError, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tc.JSON(http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateCreate(
            StringBuilder sb, String entityName, String handlerName, String varName, String path) {

        sb.append("// Create creates a new ").append(varName).append(".\n");
        sb.append("//\n");
        sb.append("// @Summary      Create ").append(varName).append("\n");
        sb.append("// @Description  Creates a new ").append(varName).append("\n");
        sb.append("// @Tags         ").append(path).append("\n");
        sb.append("// @Accept       json\n");
        sb.append("// @Produce      json\n");
        sb.append("// @Param        request  body      dto.Create")
                .append(entityName)
                .append("Request  true  \"Create request\"\n");
        sb.append("// @Success      201      {object}  dto.")
                .append(entityName)
                .append("Response\n");
        sb.append("// @Failure      400      {object}  dto.ValidationErrorResponse\n");
        sb.append("// @Failure      500      {object}  dto.ErrorResponse\n");
        sb.append("// @Router       /api/v1/").append(path).append(" [post]\n");
        sb.append("func (h *").append(handlerName).append(") Create(c *gin.Context) {\n");
        sb.append("\tvar req dto.Create").append(entityName).append("Request\n");
        sb.append("\tif err := c.ShouldBindJSON(&req); err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusBadRequest, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusBadRequest, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.Create(c.Request.Context(), &req)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusInternalServerError, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusInternalServerError, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tc.JSON(http.StatusCreated, response)\n");
        sb.append("}\n\n");
    }

    private void generateUpdate(
            StringBuilder sb, String entityName, String handlerName, String varName, String path) {

        sb.append("// Update updates an existing ").append(varName).append(".\n");
        sb.append("//\n");
        sb.append("// @Summary      Update ").append(varName).append("\n");
        sb.append("// @Description  Updates an existing ").append(varName).append("\n");
        sb.append("// @Tags         ").append(path).append("\n");
        sb.append("// @Accept       json\n");
        sb.append("// @Produce      json\n");
        sb.append("// @Param        id       path      int                           true  \"")
                .append(entityName)
                .append(" ID\"\n");
        sb.append("// @Param        request  body      dto.Update")
                .append(entityName)
                .append("Request  true  \"Update request\"\n");
        sb.append("// @Success      200      {object}  dto.")
                .append(entityName)
                .append("Response\n");
        sb.append("// @Failure      400      {object}  dto.ValidationErrorResponse\n");
        sb.append("// @Failure      404      {object}  dto.ErrorResponse\n");
        sb.append("// @Failure      500      {object}  dto.ErrorResponse\n");
        sb.append("// @Router       /api/v1/").append(path).append("/{id} [put]\n");
        sb.append("func (h *").append(handlerName).append(") Update(c *gin.Context) {\n");
        sb.append("\tid, err := strconv.ParseInt(c.Param(\"id\"), 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusBadRequest, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusBadRequest, \"invalid id parameter\", c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tvar req dto.Update").append(entityName).append("Request\n");
        sb.append("\tif err := c.ShouldBindJSON(&req); err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusBadRequest, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusBadRequest, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tresponse, err := h.service.Update(c.Request.Context(), id, &req)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\tc.JSON(http.StatusNotFound, dto.NewErrorResponse(\n");
        sb.append("\t\t\t\thttp.StatusNotFound, \"")
                .append(varName)
                .append(" not found\", c.Request.URL.Path))\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\tc.JSON(http.StatusInternalServerError, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusInternalServerError, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tc.JSON(http.StatusOK, response)\n");
        sb.append("}\n\n");
    }

    private void generateDelete(
            StringBuilder sb, String entityName, String handlerName, String varName, String path) {

        sb.append("// Delete deletes a ").append(varName).append(".\n");
        sb.append("//\n");
        sb.append("// @Summary      Delete ").append(varName).append("\n");
        sb.append("// @Description  Soft-deletes a ").append(varName).append("\n");
        sb.append("// @Tags         ").append(path).append("\n");
        sb.append("// @Accept       json\n");
        sb.append("// @Produce      json\n");
        sb.append("// @Param        id   path      int  true  \"")
                .append(entityName)
                .append(" ID\"\n");
        sb.append("// @Success      204  \"No Content\"\n");
        sb.append("// @Failure      400  {object}  dto.ErrorResponse\n");
        sb.append("// @Failure      404  {object}  dto.ErrorResponse\n");
        sb.append("// @Failure      500  {object}  dto.ErrorResponse\n");
        sb.append("// @Router       /api/v1/").append(path).append("/{id} [delete]\n");
        sb.append("func (h *").append(handlerName).append(") Delete(c *gin.Context) {\n");
        sb.append("\tid, err := strconv.ParseInt(c.Param(\"id\"), 10, 64)\n");
        sb.append("\tif err != nil {\n");
        sb.append("\t\tc.JSON(http.StatusBadRequest, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusBadRequest, \"invalid id parameter\", c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tif err := h.service.Delete(c.Request.Context(), id); err != nil {\n");
        sb.append("\t\tif errors.Is(err, service.Err").append(entityName).append("NotFound) {\n");
        sb.append("\t\t\tc.JSON(http.StatusNotFound, dto.NewErrorResponse(\n");
        sb.append("\t\t\t\thttp.StatusNotFound, \"")
                .append(varName)
                .append(" not found\", c.Request.URL.Path))\n");
        sb.append("\t\t\treturn\n");
        sb.append("\t\t}\n");
        sb.append("\t\tc.JSON(http.StatusInternalServerError, dto.NewErrorResponse(\n");
        sb.append("\t\t\thttp.StatusInternalServerError, err.Error(), c.Request.URL.Path))\n");
        sb.append("\t\treturn\n");
        sb.append("\t}\n\n");
        sb.append("\tc.Status(http.StatusNoContent)\n");
        sb.append("}\n");
    }
}
