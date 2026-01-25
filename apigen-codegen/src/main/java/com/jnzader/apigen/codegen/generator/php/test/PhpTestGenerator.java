/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.php.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates PHPUnit test files for PHP/Laravel projects.
 *
 * @author APiGen
 * @since 2.16.0
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class PhpTestGenerator {

    /**
     * Generates all test files for a table.
     *
     * @param table the table to generate tests for
     * @return map of file path to content
     */
    public Map<String, String> generateTests(SqlTable table) {
        Map<String, String> files = new LinkedHashMap<>();
        String entityName = table.getEntityName();

        files.put("tests/Unit/" + entityName + "ServiceTest.php", generateUnitTest(entityName));
        files.put(
                "tests/Feature/" + entityName + "ControllerTest.php",
                generateFeatureTest(table, entityName));

        return files;
    }

    private String generateUnitTest(String entityName) {
        return String.format(
                """
                <?php

                namespace Tests\\Unit;

                use App\\Models\\%s;
                use App\\Services\\%sService;
                use App\\Repositories\\%sRepository;
                use Illuminate\\Foundation\\Testing\\RefreshDatabase;
                use Mockery;
                use Tests\\TestCase;

                class %sServiceTest extends TestCase
                {
                    use RefreshDatabase;

                    private %sService $service;
                    private $mockRepository;

                    protected function setUp(): void
                    {
                        parent::setUp();
                        $this->mockRepository = Mockery::mock(%sRepository::class);
                        $this->service = new %sService($this->mockRepository);
                    }

                    protected function tearDown(): void
                    {
                        Mockery::close();
                        parent::tearDown();
                    }

                    public function test_get_all_returns_paginated_results(): void
                    {
                        $expected = collect([]);
                        $this->mockRepository
                            ->shouldReceive('paginate')
                            ->once()
                            ->andReturn($expected);

                        $result = $this->service->getAll();

                        $this->assertEquals($expected, $result);
                    }

                    public function test_find_by_id_returns_entity(): void
                    {
                        $expected = new %s(['id' => 1]);
                        $this->mockRepository
                            ->shouldReceive('find')
                            ->with(1)
                            ->once()
                            ->andReturn($expected);

                        $result = $this->service->findById(1);

                        $this->assertEquals($expected, $result);
                    }

                    public function test_find_by_id_returns_null_when_not_found(): void
                    {
                        $this->mockRepository
                            ->shouldReceive('find')
                            ->with(99999)
                            ->once()
                            ->andReturn(null);

                        $result = $this->service->findById(99999);

                        $this->assertNull($result);
                    }

                    public function test_create_returns_new_entity(): void
                    {
                        $data = [];
                        $expected = new %s(['id' => 1]);
                        $this->mockRepository
                            ->shouldReceive('create')
                            ->with($data)
                            ->once()
                            ->andReturn($expected);

                        $result = $this->service->create($data);

                        $this->assertEquals($expected, $result);
                    }

                    public function test_update_returns_updated_entity(): void
                    {
                        $data = [];
                        $existing = new %s(['id' => 1]);
                        $this->mockRepository
                            ->shouldReceive('find')
                            ->with(1)
                            ->once()
                            ->andReturn($existing);
                        $this->mockRepository
                            ->shouldReceive('update')
                            ->once()
                            ->andReturn(true);

                        $result = $this->service->update(1, $data);

                        $this->assertTrue($result);
                    }

                    public function test_delete_removes_entity(): void
                    {
                        $this->mockRepository
                            ->shouldReceive('delete')
                            ->with(1)
                            ->once()
                            ->andReturn(true);

                        $result = $this->service->delete(1);

                        $this->assertTrue($result);
                    }
                }
                """,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName);
    }

    private String generateFeatureTest(SqlTable table, String entityName) {
        String pluralSnakeName = toSnakeCase(table.getName()) + "s";

        return String.format(
                """
                <?php

                namespace Tests\\Feature;

                use App\\Models\\%s;
                use Illuminate\\Foundation\\Testing\\RefreshDatabase;
                use Tests\\TestCase;

                class %sControllerTest extends TestCase
                {
                    use RefreshDatabase;

                    public function test_index_returns_paginated_list(): void
                    {
                        %s::factory()->count(15)->create();

                        $response = $this->getJson('/api/%s');

                        $response->assertStatus(200)
                            ->assertJsonStructure([
                                'data' => [
                                    '*' => ['id']
                                ],
                                'meta' => ['current_page', 'last_page', 'per_page', 'total'],
                                'links',
                            ]);
                    }

                    public function test_show_returns_single_entity(): void
                    {
                        $entity = %s::factory()->create();

                        $response = $this->getJson('/api/%s/' . $entity->id);

                        $response->assertStatus(200)
                            ->assertJsonPath('data.id', $entity->id);
                    }

                    public function test_show_returns_404_when_not_found(): void
                    {
                        $response = $this->getJson('/api/%s/99999');

                        $response->assertStatus(404);
                    }

                    public function test_store_creates_new_entity(): void
                    {
                        $data = [
                            // Add required fields
                        ];

                        $response = $this->postJson('/api/%s', $data);

                        $response->assertStatus(201)
                            ->assertJsonStructure(['data' => ['id']]);
                        $this->assertDatabaseHas('%s', ['id' => $response->json('data.id')]);
                    }

                    public function test_store_validates_required_fields(): void
                    {
                        $response = $this->postJson('/api/%s', []);

                        $response->assertStatus(422)
                            ->assertJsonValidationErrors([]);
                    }

                    public function test_update_modifies_entity(): void
                    {
                        $entity = %s::factory()->create();
                        $data = [
                            // Add fields to update
                        ];

                        $response = $this->putJson('/api/%s/' . $entity->id, $data);

                        $response->assertStatus(200);
                    }

                    public function test_update_returns_404_when_not_found(): void
                    {
                        $response = $this->putJson('/api/%s/99999', []);

                        $response->assertStatus(404);
                    }

                    public function test_destroy_removes_entity(): void
                    {
                        $entity = %s::factory()->create();

                        $response = $this->deleteJson('/api/%s/' . $entity->id);

                        $response->assertStatus(204);
                        $this->assertDatabaseMissing('%s', ['id' => $entity->id]);
                    }

                    public function test_destroy_returns_404_when_not_found(): void
                    {
                        $response = $this->deleteJson('/api/%s/99999');

                        $response->assertStatus(404);
                    }
                }
                """,
                entityName,
                entityName,
                entityName,
                pluralSnakeName,
                entityName,
                pluralSnakeName,
                pluralSnakeName,
                pluralSnakeName,
                pluralSnakeName,
                pluralSnakeName,
                entityName,
                pluralSnakeName,
                pluralSnakeName,
                entityName,
                pluralSnakeName,
                pluralSnakeName,
                pluralSnakeName);
    }
}
