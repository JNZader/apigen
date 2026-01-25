package com.jnzader.apigen.codegen.generator.typescript.module;

import com.jnzader.apigen.codegen.generator.typescript.TypeScriptTypeMapper;
import com.jnzader.apigen.codegen.model.SqlSchema;
import com.jnzader.apigen.codegen.model.SqlTable;
import java.util.List;

/**
 * Generates NestJS module classes.
 *
 * <p>Generates:
 *
 * <ul>
 *   <li>Feature modules for each entity
 *   <li>App module with all imports
 *   <li>Database module for TypeORM configuration
 * </ul>
 */
@SuppressWarnings("java:S1192") // Duplicate strings intentional for code generation templates
public class TypeScriptModuleGenerator {

    private final TypeScriptTypeMapper typeMapper;

    public TypeScriptModuleGenerator() {
        this.typeMapper = new TypeScriptTypeMapper();
    }

    /**
     * Generates a feature module for an entity.
     *
     * @param table the SQL table
     * @return the module.ts content
     */
    public String generate(SqlTable table) {
        StringBuilder sb = new StringBuilder();
        String className = table.getEntityName();
        String entityKebab = typeMapper.toKebabCase(className);

        // Imports
        sb.append("import { Module } from '@nestjs/common';\n");
        sb.append("import { TypeOrmModule } from '@nestjs/typeorm';\n");
        sb.append("import { ")
                .append(className)
                .append(" } from './entities/")
                .append(entityKebab)
                .append(".entity';\n");
        sb.append("import { ")
                .append(className)
                .append("Repository } from './repositories/")
                .append(entityKebab)
                .append(".repository';\n");
        sb.append("import { ")
                .append(className)
                .append("Service } from './services/")
                .append(entityKebab)
                .append(".service';\n");
        sb.append("import { ")
                .append(className)
                .append("Controller } from './controllers/")
                .append(entityKebab)
                .append(".controller';\n");
        sb.append("\n");

        // Module decorator
        sb.append("@Module({\n");
        sb.append("  imports: [TypeOrmModule.forFeature([").append(className).append("])],\n");
        sb.append("  controllers: [").append(className).append("Controller],\n");
        sb.append("  providers: [")
                .append(className)
                .append("Repository, ")
                .append(className)
                .append("Service],\n");
        sb.append("  exports: [").append(className).append("Service],\n");
        sb.append("})\n");
        sb.append("export class ").append(className).append("Module {}\n");

        return sb.toString();
    }

    /**
     * Generates the main app module.
     *
     * @param schema the SQL schema
     * @param projectName the project name
     * @return the app.module.ts content
     */
    public String generateAppModule(SqlSchema schema, String projectName) {
        StringBuilder sb = new StringBuilder();
        List<SqlTable> entityTables = schema.getEntityTables();

        // Imports
        sb.append("import { Module } from '@nestjs/common';\n");
        sb.append("import { ConfigModule, ConfigService } from '@nestjs/config';\n");
        sb.append("import { TypeOrmModule } from '@nestjs/typeorm';\n");

        // Import feature modules
        for (SqlTable table : entityTables) {
            String className = table.getEntityName();
            String moduleKebab = typeMapper.toKebabCase(className);
            sb.append("import { ")
                    .append(className)
                    .append("Module } from './modules/")
                    .append(moduleKebab)
                    .append("/")
                    .append(moduleKebab)
                    .append(".module';\n");
        }
        sb.append("\n");

        // Module decorator
        sb.append("@Module({\n");
        sb.append("  imports: [\n");

        // ConfigModule
        sb.append("    ConfigModule.forRoot({\n");
        sb.append("      isGlobal: true,\n");
        sb.append("      envFilePath: ['.env.local', '.env'],\n");
        sb.append("    }),\n");

        // TypeOrmModule
        sb.append("    TypeOrmModule.forRootAsync({\n");
        sb.append("      imports: [ConfigModule],\n");
        sb.append("      useFactory: (configService: ConfigService) => ({\n");
        sb.append("        type: 'postgres',\n");
        sb.append("        host: configService.get('DB_HOST', 'localhost'),\n");
        sb.append("        port: configService.get('DB_PORT', 5432),\n");
        sb.append("        username: configService.get('DB_USERNAME', 'postgres'),\n");
        sb.append("        password: configService.get('DB_PASSWORD', 'postgres'),\n");
        sb.append("        database: configService.get('DB_DATABASE', '")
                .append(toSnakeCase(projectName))
                .append("'),\n");
        sb.append("        autoLoadEntities: true,\n");
        sb.append("        synchronize: configService.get('NODE_ENV') !== 'production',\n");
        sb.append("        logging: configService.get('NODE_ENV') !== 'production',\n");
        sb.append("      }),\n");
        sb.append("      inject: [ConfigService],\n");
        sb.append("    }),\n");

        // Feature modules
        for (SqlTable table : entityTables) {
            String className = table.getEntityName();
            sb.append("    ").append(className).append("Module,\n");
        }

        sb.append("  ],\n");
        sb.append("})\n");
        sb.append("export class AppModule {}\n");

        return sb.toString();
    }

    /**
     * Generates the main.ts entry point.
     *
     * @param projectName the project name
     * @return the main.ts content
     */
    public String generateMain(String projectName) {
        return """
        import { NestFactory } from '@nestjs/core';
        import { ValidationPipe } from '@nestjs/common';
        import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
        import { AppModule } from './app.module';

        async function bootstrap() {
          const app = await NestFactory.create(AppModule);

          // Enable validation
          app.useGlobalPipes(
            new ValidationPipe({
              whitelist: true,
              forbidNonWhitelisted: true,
              transform: true,
              transformOptions: {
                enableImplicitConversion: true,
              },
            }),
          );

          // Enable CORS
          app.enableCors();

          // Swagger documentation
          const config = new DocumentBuilder()
            .setTitle('%s API')
            .setDescription('REST API for %s')
            .setVersion('1.0')
            .addTag('api')
            .build();
          const document = SwaggerModule.createDocument(app, config);
          SwaggerModule.setup('api/docs', app, document);

          const port = process.env.PORT || 3000;
          await app.listen(port);
          console.log(`Application is running on: http://localhost:${port}`);
          console.log(`Swagger docs available at: http://localhost:${port}/api/docs`);
        }
        bootstrap();
        """
                .formatted(projectName, projectName);
    }

    private String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").replace("-", "_").toLowerCase();
    }
}
