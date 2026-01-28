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
package com.jnzader.apigen.codegen.generator.typescript.test;

import static com.jnzader.apigen.codegen.generator.util.NamingUtils.*;

import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates Jest test files for TypeScript/NestJS projects.
 *
 * @author APiGen
 * @since 2.16.0
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class TypeScriptTestGenerator {

    /**
     * Generates all test files for a table.
     *
     * @param table the table to generate tests for
     * @return map of file path to content
     */
    public Map<String, String> generateTests(SqlTable table) {
        Map<String, String> files = new LinkedHashMap<>();
        String entityName = table.getEntityName();
        String kebabName = toKebabCase(table.getName());

        files.put(
                "src/" + kebabName + "/entities/" + kebabName + ".entity.spec.ts",
                generateEntityTest(entityName, kebabName));
        files.put(
                "src/" + kebabName + "/dto/" + kebabName + ".dto.spec.ts",
                generateDtoTest(entityName, kebabName));
        files.put(
                "src/" + kebabName + "/" + kebabName + ".service.spec.ts",
                generateServiceTest(entityName, kebabName));
        files.put(
                "src/" + kebabName + "/" + kebabName + ".controller.spec.ts",
                generateControllerTest(entityName, kebabName));

        return files;
    }

    private String generateEntityTest(String entityName, String kebabName) {
        return String.format(
                """
                import { %s } from './%s.entity';

                describe('%s Entity', () => {
                  let entity: %s;

                  beforeEach(() => {
                    entity = new %s();
                  });

                  describe('initialization', () => {
                    it('should create an instance', () => {
                      expect(entity).toBeDefined();
                      expect(entity).toBeInstanceOf(%s);
                    });

                    it('should have default estado as true', () => {
                      expect(entity.estado).toBe(true);
                    });

                    it('should have undefined id initially', () => {
                      expect(entity.id).toBeUndefined();
                    });
                  });

                  describe('properties', () => {
                    it('should set and get id', () => {
                      entity.id = 1;
                      expect(entity.id).toBe(1);
                    });

                    it('should set and get estado', () => {
                      entity.estado = false;
                      expect(entity.estado).toBe(false);
                    });

                    it('should set and get audit fields', () => {
                      const now = new Date();
                      entity.createdAt = now;
                      entity.updatedAt = now;
                      entity.createdBy = 'testUser';
                      entity.updatedBy = 'testUser';

                      expect(entity.createdAt).toBe(now);
                      expect(entity.updatedAt).toBe(now);
                      expect(entity.createdBy).toBe('testUser');
                      expect(entity.updatedBy).toBe('testUser');
                    });

                    it('should set and get soft delete fields', () => {
                      const now = new Date();
                      entity.deletedAt = now;
                      entity.deletedBy = 'admin';

                      expect(entity.deletedAt).toBe(now);
                      expect(entity.deletedBy).toBe('admin');
                    });
                  });
                });
                """,
                entityName, kebabName, entityName, entityName, entityName, entityName);
    }

    private String generateDtoTest(String entityName, String kebabName) {
        return String.format(
                """
                import { validate } from 'class-validator';
                import { Create%sDto, Update%sDto, %sDto } from './%s.dto';

                describe('%s DTOs', () => {
                  describe('Create%sDto', () => {
                    it('should pass validation with valid data', async () => {
                      const dto = new Create%sDto();
                      // Set required fields here
                      const errors = await validate(dto);
                      // Expect errors to be empty when required fields are set
                      expect(Array.isArray(errors)).toBe(true);
                    });

                    it('should fail validation with empty data', async () => {
                      const dto = new Create%sDto();
                      const errors = await validate(dto);
                      // May have validation errors for required fields
                      expect(Array.isArray(errors)).toBe(true);
                    });
                  });

                  describe('Update%sDto', () => {
                    it('should pass validation with partial data', async () => {
                      const dto = new Update%sDto();
                      const errors = await validate(dto);
                      // Update DTOs typically allow partial updates
                      expect(errors.length).toBe(0);
                    });
                  });

                  describe('%sDto', () => {
                    it('should create response DTO with all fields', () => {
                      const dto = new %sDto();
                      dto.id = 1;
                      dto.activo = true;

                      expect(dto.id).toBe(1);
                      expect(dto.activo).toBe(true);
                    });
                  });
                });
                """,
                entityName,
                entityName,
                entityName,
                kebabName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName);
    }

    /**
     * Generates e2e test for API endpoints.
     *
     * @param table the table
     * @return e2e test content
     */
    public String generateE2eTest(SqlTable table) {
        String entityName = table.getEntityName();
        String kebabName = toKebabCase(table.getName());
        String pluralName = kebabName + "s";

        return String.format(
                """
                import { Test, TestingModule } from '@nestjs/testing';
                import { INestApplication, ValidationPipe } from '@nestjs/common';
                import * as request from 'supertest';
                import { AppModule } from '../src/app.module';

                describe('%sController (e2e)', () => {
                  let app: INestApplication;
                  let createdId: number;

                  beforeAll(async () => {
                    const moduleFixture: TestingModule = await Test.createTestingModule({
                      imports: [AppModule],
                    }).compile();

                    app = moduleFixture.createNestApplication();
                    app.useGlobalPipes(new ValidationPipe({ transform: true }));
                    await app.init();
                  });

                  afterAll(async () => {
                    await app.close();
                  });

                  describe('POST /api/%s', () => {
                    it('should create a new %s', async () => {
                      const createDto = {
                        // Add required fields
                      };

                      const response = await request(app.getHttpServer())
                        .post('/api/%s')
                        .send(createDto)
                        .expect(201);

                      expect(response.body).toHaveProperty('id');
                      createdId = response.body.id;
                    });

                    it('should fail with invalid data', async () => {
                      await request(app.getHttpServer())
                        .post('/api/%s')
                        .send({})
                        .expect(400);
                    });
                  });

                  describe('GET /api/%s', () => {
                    it('should return paginated list', async () => {
                      const response = await request(app.getHttpServer())
                        .get('/api/%s')
                        .expect(200);

                      expect(response.body).toHaveProperty('items');
                      expect(response.body).toHaveProperty('meta');
                      expect(Array.isArray(response.body.items)).toBe(true);
                    });

                    it('should support pagination params', async () => {
                      const response = await request(app.getHttpServer())
                        .get('/api/%s?page=1&limit=10')
                        .expect(200);

                      expect(response.body.meta.page).toBe(1);
                      expect(response.body.meta.limit).toBe(10);
                    });
                  });

                  describe('GET /api/%s/:id', () => {
                    it('should return a %s by id', async () => {
                      const response = await request(app.getHttpServer())
                        .get(`/api/%s/${createdId}`)
                        .expect(200);

                      expect(response.body.id).toBe(createdId);
                    });

                    it('should return 404 for non-existent id', async () => {
                      await request(app.getHttpServer())
                        .get('/api/%s/99999')
                        .expect(404);
                    });
                  });

                  describe('PUT /api/%s/:id', () => {
                    it('should update a %s', async () => {
                      const updateDto = {
                        // Add fields to update
                      };

                      const response = await request(app.getHttpServer())
                        .put(`/api/%s/${createdId}`)
                        .send(updateDto)
                        .expect(200);

                      expect(response.body.id).toBe(createdId);
                    });
                  });

                  describe('DELETE /api/%s/:id', () => {
                    it('should delete a %s', async () => {
                      await request(app.getHttpServer())
                        .delete(`/api/%s/${createdId}`)
                        .expect(200);

                      // Verify deletion
                      await request(app.getHttpServer())
                        .get(`/api/%s/${createdId}`)
                        .expect(404);
                    });
                  });
                });
                """,
                entityName,
                pluralName,
                kebabName,
                pluralName,
                pluralName,
                pluralName,
                pluralName,
                pluralName,
                pluralName,
                kebabName,
                pluralName,
                pluralName,
                pluralName,
                kebabName,
                pluralName,
                pluralName,
                kebabName,
                pluralName,
                pluralName);
    }

    private String generateServiceTest(String entityName, String kebabName) {
        String camelName = toCamelCase(entityName);

        return String.format(
                """
                import { Test, TestingModule } from '@nestjs/testing';
                import { getRepositoryToken } from '@nestjs/typeorm';
                import { Repository } from 'typeorm';
                import { %sService } from './%s.service';
                import { %s } from './entities/%s.entity';
                import { Create%sDto } from './dto/create-%s.dto';
                import { Update%sDto } from './dto/update-%s.dto';

                const mock%sRepository = () => ({
                  find: jest.fn(),
                  findOne: jest.fn(),
                  create: jest.fn(),
                  save: jest.fn(),
                  update: jest.fn(),
                  delete: jest.fn(),
                  count: jest.fn(),
                });

                type MockRepository<T = any> = Partial<Record<keyof Repository<T>, jest.Mock>>;

                describe('%sService', () => {
                  let service: %sService;
                  let repository: MockRepository<%s>;

                  beforeEach(async () => {
                    const module: TestingModule = await Test.createTestingModule({
                      providers: [
                        %sService,
                        {
                          provide: getRepositoryToken(%s),
                          useFactory: mock%sRepository,
                        },
                      ],
                    }).compile();

                    service = module.get<%sService>(%sService);
                    repository = module.get<MockRepository<%s>>(getRepositoryToken(%s));
                  });

                  it('should be defined', () => {
                    expect(service).toBeDefined();
                  });

                  describe('findAll', () => {
                    it('should return an array of %s', async () => {
                      const expected%s: %s[] = [{ id: 1 } as %s];
                      repository.find.mockResolvedValue(expected%s);
                      repository.count.mockResolvedValue(1);

                      const result = await service.findAll({ page: 1, limit: 10 });

                      expect(result.items).toEqual(expected%s);
                      expect(result.meta.total).toBe(1);
                    });
                  });

                  describe('findOne', () => {
                    it('should return a %s by id', async () => {
                      const expected%s = { id: 1 } as %s;
                      repository.findOne.mockResolvedValue(expected%s);

                      const result = await service.findOne(1);

                      expect(result).toEqual(expected%s);
                      expect(repository.findOne).toHaveBeenCalledWith({ where: { id: 1 } });
                    });

                    it('should throw NotFoundException when %s not found', async () => {
                      repository.findOne.mockResolvedValue(null);

                      await expect(service.findOne(99999)).rejects.toThrow();
                    });
                  });

                  describe('create', () => {
                    it('should create a new %s', async () => {
                      const createDto: Create%sDto = {};
                      const created%s = { id: 1 } as %s;
                      repository.create.mockReturnValue(created%s);
                      repository.save.mockResolvedValue(created%s);

                      const result = await service.create(createDto);

                      expect(result).toEqual(created%s);
                      expect(repository.create).toHaveBeenCalledWith(createDto);
                      expect(repository.save).toHaveBeenCalled();
                    });
                  });

                  describe('update', () => {
                    it('should update a %s', async () => {
                      const updateDto: Update%sDto = {};
                      const existing%s = { id: 1 } as %s;
                      const updated%s = { id: 1, ...updateDto } as %s;
                      repository.findOne.mockResolvedValue(existing%s);
                      repository.save.mockResolvedValue(updated%s);

                      const result = await service.update(1, updateDto);

                      expect(result).toEqual(updated%s);
                    });
                  });

                  describe('remove', () => {
                    it('should remove a %s', async () => {
                      const existing%s = { id: 1 } as %s;
                      repository.findOne.mockResolvedValue(existing%s);
                      repository.delete.mockResolvedValue({ affected: 1 });

                      await service.remove(1);

                      expect(repository.delete).toHaveBeenCalledWith(1);
                    });
                  });
                });
                """,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                camelName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
                entityName,
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

    private String generateControllerTest(String entityName, String kebabName) {
        return String.format(
                """
                import { Test, TestingModule } from '@nestjs/testing';
                import { %sController } from './%s.controller';
                import { %sService } from './%s.service';
                import { Create%sDto } from './dto/create-%s.dto';
                import { Update%sDto } from './dto/update-%s.dto';

                const mock%sService = () => ({
                  findAll: jest.fn(),
                  findOne: jest.fn(),
                  create: jest.fn(),
                  update: jest.fn(),
                  remove: jest.fn(),
                });

                describe('%sController', () => {
                  let controller: %sController;
                  let service: ReturnType<typeof mock%sService>;

                  beforeEach(async () => {
                    const module: TestingModule = await Test.createTestingModule({
                      controllers: [%sController],
                      providers: [
                        {
                          provide: %sService,
                          useFactory: mock%sService,
                        },
                      ],
                    }).compile();

                    controller = module.get<%sController>(%sController);
                    service = module.get<%sService>(%sService) as unknown as ReturnType<typeof mock%sService>;
                  });

                  it('should be defined', () => {
                    expect(controller).toBeDefined();
                  });

                  describe('findAll', () => {
                    it('should return paginated list', async () => {
                      const expected = { items: [], meta: { total: 0, page: 1, limit: 10 } };
                      service.findAll.mockResolvedValue(expected);

                      const result = await controller.findAll({ page: 1, limit: 10 });

                      expect(result).toEqual(expected);
                    });
                  });

                  describe('findOne', () => {
                    it('should return a single item', async () => {
                      const expected = { id: 1 };
                      service.findOne.mockResolvedValue(expected);

                      const result = await controller.findOne(1);

                      expect(result).toEqual(expected);
                    });
                  });

                  describe('create', () => {
                    it('should create a new item', async () => {
                      const createDto: Create%sDto = {};
                      const expected = { id: 1 };
                      service.create.mockResolvedValue(expected);

                      const result = await controller.create(createDto);

                      expect(result).toEqual(expected);
                      expect(service.create).toHaveBeenCalledWith(createDto);
                    });
                  });

                  describe('update', () => {
                    it('should update an item', async () => {
                      const updateDto: Update%sDto = {};
                      const expected = { id: 1 };
                      service.update.mockResolvedValue(expected);

                      const result = await controller.update(1, updateDto);

                      expect(result).toEqual(expected);
                      expect(service.update).toHaveBeenCalledWith(1, updateDto);
                    });
                  });

                  describe('remove', () => {
                    it('should remove an item', async () => {
                      service.remove.mockResolvedValue(undefined);

                      await controller.remove(1);

                      expect(service.remove).toHaveBeenCalledWith(1);
                    });
                  });
                });
                """,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                kebabName,
                entityName,
                entityName,
                entityName,
                entityName,
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
}
