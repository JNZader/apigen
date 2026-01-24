package com.jnzader.apigen.codegen.generator.php.service;

import com.jnzader.apigen.codegen.generator.php.PhpTypeMapper;
import com.jnzader.apigen.codegen.model.SqlTable;

/**
 * Generates Service classes for Laravel projects.
 *
 * <p>Services contain business logic and coordinate between controllers and models.
 */
public class PhpServiceGenerator {

    private final PhpTypeMapper typeMapper;

    public PhpServiceGenerator() {
        this.typeMapper = new PhpTypeMapper();
    }

    /**
     * Generates a base service class with common CRUD operations.
     *
     * @return the BaseService.php content
     */
    public String generateBase() {
        return """
        <?php

        namespace App\\Services;

        use Illuminate\\Database\\Eloquent\\Model;
        use Illuminate\\Pagination\\LengthAwarePaginator;
        use Illuminate\\Support\\Collection;

        abstract class BaseService
        {
            /**
             * The model class this service works with.
             */
            protected string $modelClass;

            /**
             * Get the model instance.
             */
            protected function model(): Model
            {
                return new $this->modelClass;
            }

            /**
             * Get a record by ID.
             */
            public function findById(int $id): ?Model
            {
                return $this->model()
                    ->newQuery()
                    ->active()
                    ->find($id);
            }

            /**
             * Get a record by ID or fail with 404.
             */
            public function findByIdOrFail(int $id): Model
            {
                return $this->model()
                    ->newQuery()
                    ->active()
                    ->findOrFail($id);
            }

            /**
             * Get all records with pagination.
             */
            public function getAll(int $page = 1, int $perPage = 10, bool $includeInactive = false): LengthAwarePaginator
            {
                $query = $this->model()->newQuery();

                if (!$includeInactive) {
                    $query->active();
                }

                return $query->orderBy('created_at', 'desc')
                    ->paginate($perPage, ['*'], 'page', $page);
            }

            /**
             * Create a new record.
             */
            public function create(array $data): Model
            {
                return $this->model()->create($data);
            }

            /**
             * Update an existing record.
             */
            public function update(int $id, array $data): Model
            {
                $model = $this->findByIdOrFail($id);
                $model->update($data);
                return $model->fresh();
            }

            /**
             * Soft delete a record.
             */
            public function delete(int $id): bool
            {
                $model = $this->findByIdOrFail($id);
                return $model->softDelete();
            }

            /**
             * Permanently delete a record.
             */
            public function forceDelete(int $id): bool
            {
                $model = $this->findByIdOrFail($id);
                return $model->delete();
            }

            /**
             * Restore a soft-deleted record.
             */
            public function restore(int $id): Model
            {
                $model = $this->model()
                    ->newQuery()
                    ->where('id', $id)
                    ->inactive()
                    ->firstOrFail();

                $model->restore();
                return $model->fresh();
            }

            /**
             * Count total records.
             */
            public function count(bool $includeInactive = false): int
            {
                $query = $this->model()->newQuery();

                if (!$includeInactive) {
                    $query->active();
                }

                return $query->count();
            }
        }
        """;
    }

    /**
     * Generates a service for a specific entity.
     *
     * @param table the SQL table
     * @return the Service.php content
     */
    public String generate(SqlTable table) {
        String className = table.getEntityName();
        String varName = typeMapper.toCamelCase(className);

        StringBuilder sb = new StringBuilder();

        sb.append("<?php\n\n");
        sb.append("namespace App\\Services;\n\n");
        sb.append("use App\\Models\\").append(className).append(";\n");
        sb.append("use Illuminate\\Database\\Eloquent\\Model;\n\n");

        sb.append("class ").append(className).append("Service extends BaseService\n");
        sb.append("{\n");

        // Model class property
        sb.append("    /**\n");
        sb.append("     * The model class this service works with.\n");
        sb.append("     */\n");
        sb.append("    protected string $modelClass = ").append(className).append("::class;\n\n");

        // Custom methods placeholder
        sb.append("    // Add custom business logic methods here\n");
        sb.append("    // Example:\n");
        sb.append("    // public function findByEmail(string $email): ?")
                .append(className)
                .append("\n");
        sb.append("    // {\n");
        sb.append("    //     return ").append(className).append("::active()\n");
        sb.append("    //         ->where('email', $email)\n");
        sb.append("    //         ->first();\n");
        sb.append("    // }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
