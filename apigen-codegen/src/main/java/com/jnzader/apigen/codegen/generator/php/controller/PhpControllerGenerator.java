package com.jnzader.apigen.codegen.generator.php.controller;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates Laravel Controller classes for REST API endpoints.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>API Controllers with CRUD operations
 *   <li>OpenAPI documentation annotations
 *   <li>Proper HTTP responses
 * </ul>
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class PhpControllerGenerator {

    private final PhpTypeMapper typeMapper;

    public PhpControllerGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates a controller for a specific entity.
     *
     * @param table the SQL table
     * @return the Controller.php content
     */
    public String generate(SqlTable table) {
        String className = table.getEntityName();
        String varName = typeMapper.toCamelCase(className);
        String pluralName = typeMapper.pluralize(varName);

        StringBuilder sb = new StringBuilder();

        sb.append("<?php\n\n");
        sb.append("namespace App\\Http\\Controllers\\Api\\V1;\n\n");
        sb.append("use App\\Http\\Controllers\\Controller;\n");
        sb.append("use App\\Http\\Requests\\Create").append(className).append("Request;\n");
        sb.append("use App\\Http\\Requests\\Update").append(className).append("Request;\n");
        sb.append("use App\\Http\\Resources\\").append(className).append("Collection;\n");
        sb.append("use App\\Http\\Resources\\").append(className).append("Resource;\n");
        sb.append("use App\\Services\\").append(className).append("Service;\n");
        sb.append("use Illuminate\\Http\\JsonResponse;\n");
        sb.append("use Illuminate\\Http\\Request;\n");
        sb.append("use Illuminate\\Http\\Response;\n");
        sb.append("use OpenApi\\Attributes as OA;\n\n");

        sb.append("#[OA\\Tag(name: '")
                .append(className)
                .append("s', description: '")
                .append(className)
                .append(" management endpoints')]\n");
        sb.append("class ").append(className).append("Controller extends Controller\n");
        sb.append("{\n");

        // Constructor with DI
        sb.append("    public function __construct(\n");
        sb.append("        private readonly ").append(className).append("Service $service\n");
        sb.append("    ) {}\n\n");

        // Index (GET all with pagination)
        generateIndexMethod(sb, className, pluralName);

        // Show (GET by ID)
        generateShowMethod(sb, className, varName);

        // Store (POST create)
        generateStoreMethod(sb, className, varName);

        // Update (PUT/PATCH)
        generateUpdateMethod(sb, className, varName);

        // Destroy (DELETE - soft delete)
        generateDestroyMethod(sb, className, varName);

        // Restore (POST restore)
        generateRestoreMethod(sb, className, varName);

        sb.append("}\n");

        return sb.toString();
    }

    private void generateIndexMethod(StringBuilder sb, String className, String pluralName) {
        sb.append("    /**\n");
        sb.append("     * Display a paginated listing of ").append(pluralName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Get(\n");
        sb.append("        path: '/api/v1/").append(pluralName.toLowerCase()).append("',\n");
        sb.append("        summary: 'Get all ").append(pluralName).append("',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        parameters: [\n");
        sb.append(
                "            new OA\\Parameter(name: 'page', in: 'query', schema: new"
                        + " OA\\Schema(type: 'integer', default: 1)),\n");
        sb.append(
                "            new OA\\Parameter(name: 'per_page', in: 'query', schema: new"
                        + " OA\\Schema(type: 'integer', default: 10, maximum: 100)),\n");
        sb.append("        ],\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 200, description: 'Successful"
                        + " operation'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function index(Request $request): ")
                .append(className)
                .append("Collection\n");
        sb.append("    {\n");
        sb.append("        $page = (int) $request->get('page', 1);\n");
        sb.append("        $perPage = min((int) $request->get('per_page', 10), 100);\n\n");
        sb.append("        $paginator = $this->service->getAll($page, $perPage);\n\n");
        sb.append("        return new ").append(className).append("Collection($paginator);\n");
        sb.append("    }\n\n");
    }

    private void generateShowMethod(StringBuilder sb, String className, String varName) {
        sb.append("    /**\n");
        sb.append("     * Display the specified ").append(varName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Get(\n");
        sb.append("        path: '/api/v1/")
                .append(typeMapper.pluralize(varName).toLowerCase())
                .append("/{id}',\n");
        sb.append("        summary: 'Get ").append(varName).append(" by ID',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        parameters: [\n");
        sb.append(
                "            new OA\\Parameter(name: 'id', in: 'path', required: true, schema: new"
                        + " OA\\Schema(type: 'integer')),\n");
        sb.append("        ],\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 200, description: 'Successful"
                        + " operation'),\n");
        sb.append("            new OA\\Response(response: 404, description: 'Not found'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function show(int $id): ").append(className).append("Resource\n");
        sb.append("    {\n");
        sb.append("        $")
                .append(varName)
                .append(" = $this->service->findByIdOrFail($id);\n\n");
        sb.append("        return new ")
                .append(className)
                .append("Resource($")
                .append(varName)
                .append(");\n");
        sb.append("    }\n\n");
    }

    private void generateStoreMethod(StringBuilder sb, String className, String varName) {
        sb.append("    /**\n");
        sb.append("     * Store a newly created ").append(varName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Post(\n");
        sb.append("        path: '/api/v1/")
                .append(typeMapper.pluralize(varName).toLowerCase())
                .append("',\n");
        sb.append("        summary: 'Create a new ").append(varName).append("',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        requestBody: new OA\\RequestBody(required: true),\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 201, description: 'Created"
                        + " successfully'),\n");
        sb.append(
                "            new OA\\Response(response: 422, description: 'Validation error'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function store(Create")
                .append(className)
                .append("Request $request): JsonResponse\n");
        sb.append("    {\n");
        sb.append("        $")
                .append(varName)
                .append(" = $this->service->create($request->validated());\n\n");
        sb.append("        return (new ")
                .append(className)
                .append("Resource($")
                .append(varName)
                .append("))\n");
        sb.append("            ->response()\n");
        sb.append("            ->setStatusCode(Response::HTTP_CREATED);\n");
        sb.append("    }\n\n");
    }

    private void generateUpdateMethod(StringBuilder sb, String className, String varName) {
        sb.append("    /**\n");
        sb.append("     * Update the specified ").append(varName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Put(\n");
        sb.append("        path: '/api/v1/")
                .append(typeMapper.pluralize(varName).toLowerCase())
                .append("/{id}',\n");
        sb.append("        summary: 'Update ").append(varName).append("',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        parameters: [\n");
        sb.append(
                "            new OA\\Parameter(name: 'id', in: 'path', required: true, schema: new"
                        + " OA\\Schema(type: 'integer')),\n");
        sb.append("        ],\n");
        sb.append("        requestBody: new OA\\RequestBody(required: true),\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 200, description: 'Updated"
                        + " successfully'),\n");
        sb.append("            new OA\\Response(response: 404, description: 'Not found'),\n");
        sb.append(
                "            new OA\\Response(response: 422, description: 'Validation error'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function update(Update")
                .append(className)
                .append("Request $request, int $id): ")
                .append(className)
                .append("Resource\n");
        sb.append("    {\n");
        sb.append("        $")
                .append(varName)
                .append(" = $this->service->update($id, $request->validated());\n\n");
        sb.append("        return new ")
                .append(className)
                .append("Resource($")
                .append(varName)
                .append(");\n");
        sb.append("    }\n\n");
    }

    private void generateDestroyMethod(StringBuilder sb, String className, String varName) {
        sb.append("    /**\n");
        sb.append("     * Soft delete the specified ").append(varName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Delete(\n");
        sb.append("        path: '/api/v1/")
                .append(typeMapper.pluralize(varName).toLowerCase())
                .append("/{id}',\n");
        sb.append("        summary: 'Delete ").append(varName).append("',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        parameters: [\n");
        sb.append(
                "            new OA\\Parameter(name: 'id', in: 'path', required: true, schema: new"
                        + " OA\\Schema(type: 'integer')),\n");
        sb.append("        ],\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 204, description: 'Deleted"
                        + " successfully'),\n");
        sb.append("            new OA\\Response(response: 404, description: 'Not found'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function destroy(int $id): JsonResponse\n");
        sb.append("    {\n");
        sb.append("        $this->service->delete($id);\n\n");
        sb.append("        return response()->json(null, Response::HTTP_NO_CONTENT);\n");
        sb.append("    }\n\n");
    }

    private void generateRestoreMethod(StringBuilder sb, String className, String varName) {
        sb.append("    /**\n");
        sb.append("     * Restore a soft-deleted ").append(varName).append(".\n");
        sb.append("     */\n");
        sb.append("    #[OA\\Post(\n");
        sb.append("        path: '/api/v1/")
                .append(typeMapper.pluralize(varName).toLowerCase())
                .append("/{id}/restore',\n");
        sb.append("        summary: 'Restore deleted ").append(varName).append("',\n");
        sb.append("        tags: ['").append(className).append("s'],\n");
        sb.append("        parameters: [\n");
        sb.append(
                "            new OA\\Parameter(name: 'id', in: 'path', required: true, schema: new"
                        + " OA\\Schema(type: 'integer')),\n");
        sb.append("        ],\n");
        sb.append("        responses: [\n");
        sb.append(
                "            new OA\\Response(response: 200, description: 'Restored"
                        + " successfully'),\n");
        sb.append("            new OA\\Response(response: 404, description: 'Not found'),\n");
        sb.append("        ]\n");
        sb.append("    )]\n");
        sb.append("    public function restore(int $id): ").append(className).append("Resource\n");
        sb.append("    {\n");
        sb.append("        $").append(varName).append(" = $this->service->restore($id);\n\n");
        sb.append("        return new ")
                .append(className)
                .append("Resource($")
                .append(varName)
                .append(");\n");
        sb.append("    }\n");
    }
}
