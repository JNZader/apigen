package com.jnzader.apigen.codegen.generator.go.router;

import com.jnzader.apigen.codegen.generator.go.GoTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;

/** Generates Gin router configuration for Go/Gin. */
public class GoRouterGenerator {

    private final GoTypeMapper typeMapper;
    private final String moduleName;

    public GoRouterGenerator(GoTypeMapper typeMapper, String moduleName) {
        this.typeMapper = typeMapper;
        this.moduleName = moduleName;
    }

    /**
     * Generates the main router setup.
     *
     * @param schema the SQL schema
     * @return the generated Go code
     */
    public String generate(SqlSchema schema) {
        StringBuilder sb = new StringBuilder();

        sb.append("package router\n\n");

        // Imports
        sb.append("import (\n");
        sb.append("\t\"").append(moduleName).append("/internal/handler\"\n");
        sb.append("\t\"github.com/gin-gonic/gin\"\n");
        sb.append("\tswaggerFiles \"github.com/swaggo/files\"\n");
        sb.append("\tginSwagger \"github.com/swaggo/gin-swagger\"\n");
        sb.append(")\n\n");

        // Handlers struct
        sb.append("// Handlers contains all HTTP handlers.\n");
        sb.append("type Handlers struct {\n");
        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String fieldName = entityName;
            sb.append("\t")
                    .append(fieldName)
                    .append(" *handler.")
                    .append(entityName)
                    .append("Handler\n");
        }
        sb.append("}\n\n");

        // SetupRouter function
        sb.append("// SetupRouter configures all routes for the application.\n");
        sb.append("func SetupRouter(h *Handlers) *gin.Engine {\n");
        sb.append("\tr := gin.Default()\n\n");

        // Health check
        sb.append("\t// Health check\n");
        sb.append("\tr.GET(\"/health\", func(c *gin.Context) {\n");
        sb.append("\t\tc.JSON(200, gin.H{\"status\": \"UP\"})\n");
        sb.append("\t})\n\n");

        // Swagger
        sb.append("\t// Swagger documentation\n");
        sb.append("\tr.GET(\"/swagger/*any\", ginSwagger.WrapHandler(swaggerFiles.Handler))\n\n");

        // API v1 routes
        sb.append("\t// API v1 routes\n");
        sb.append("\tv1 := r.Group(\"/api/v1\")\n");
        sb.append("\t{\n");

        for (SqlTable table : schema.getEntityTables()) {
            String entityName = typeMapper.toExportedName(table.getEntityName());
            String pluralPath = typeMapper.toSnakeCase(typeMapper.pluralize(table.getEntityName()));
            String handlerVar = "h." + entityName;

            sb.append("\t\t// ").append(entityName).append(" routes\n");
            sb.append("\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(" := v1.Group(\"/")
                    .append(pluralPath)
                    .append("\")\n");
            sb.append("\t\t{\n");
            sb.append("\t\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(".GET(\"\", ")
                    .append(handlerVar)
                    .append(".GetAll)\n");
            sb.append("\t\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(".GET(\"/:id\", ")
                    .append(handlerVar)
                    .append(".GetByID)\n");
            sb.append("\t\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(".POST(\"\", ")
                    .append(handlerVar)
                    .append(".Create)\n");
            sb.append("\t\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(".PUT(\"/:id\", ")
                    .append(handlerVar)
                    .append(".Update)\n");
            sb.append("\t\t\t")
                    .append(
                            typeMapper.toUnexportedName(
                                    typeMapper.pluralize(table.getEntityName())))
                    .append(".DELETE(\"/:id\", ")
                    .append(handlerVar)
                    .append(".Delete)\n");
            sb.append("\t\t}\n\n");
        }

        sb.append("\t}\n\n");
        sb.append("\treturn r\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates middleware for common functionality.
     *
     * @return the generated Go code
     */
    public String generateMiddleware() {
        return """
        package middleware

        import (
        	"log"
        	"time"

        	"github.com/gin-gonic/gin"
        )

        // Logger returns a middleware that logs request information.
        func Logger() gin.HandlerFunc {
        	return func(c *gin.Context) {
        		start := time.Now()
        		path := c.Request.URL.Path
        		method := c.Request.Method

        		c.Next()

        		latency := time.Since(start)
        		status := c.Writer.Status()

        		log.Printf("[%s] %s %s %d %v",
        			method, path, c.ClientIP(), status, latency)
        	}
        }

        // Recovery returns a middleware that recovers from panics.
        func Recovery() gin.HandlerFunc {
        	return func(c *gin.Context) {
        		defer func() {
        			if err := recover(); err != nil {
        				log.Printf("panic recovered: %v", err)
        				c.AbortWithStatusJSON(500, gin.H{
        					"status":  500,
        					"message": "Internal Server Error",
        				})
        			}
        		}()
        		c.Next()
        	}
        }

        // CORS returns a middleware that handles CORS.
        func CORS() gin.HandlerFunc {
        	return func(c *gin.Context) {
        		c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
        		c.Writer.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        		c.Writer.Header().Set("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization")

        		if c.Request.Method == "OPTIONS" {
        			c.AbortWithStatus(204)
        			return
        		}

        		c.Next()
        	}
        }

        // RateLimit returns a simple rate limiting middleware.
        func RateLimit(requestsPerSecond int) gin.HandlerFunc {
        	// Simple implementation - for production use a proper rate limiter
        	return func(c *gin.Context) {
        		c.Next()
        	}
        }
        """;
    }
}
