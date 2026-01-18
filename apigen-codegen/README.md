# APiGen CodeGen

Code generation utility that creates complete API structure from SQL schemas.

## Purpose

Automatically generate Entity, DTO, Repository, Service, Controller, Mapper, and tests from your SQL DDL - speeding up development significantly.

## Features

- Parse SQL CREATE TABLE statements
- Generate JPA entities extending `Base`
- Generate record DTOs with validation annotations
- Generate MapStruct mappers
- Generate JPA repositories
- Generate service implementations
- Generate REST controllers with HATEOAS
- Generate Flyway migrations
- Generate unit and integration tests

## Usage

**As CLI:**
```bash
java -jar apigen-codegen.jar schema.sql ./output com.mycompany
```

**As Library:**
```groovy
dependencies {
    implementation 'com.jnzader:apigen-codegen:1.0.0-SNAPSHOT'
}
```

```java
SqlToApiGenerator generator = new SqlToApiGenerator();
generator.generate(sqlContent, outputPath, basePackage);
```

## Generated Files

From a single `products` table:

```
output/
├── entity/
│   └── Product.java
├── dto/
│   └── ProductDTO.java
├── repository/
│   └── ProductRepository.java
├── service/
│   └── ProductService.java
├── controller/
│   └── ProductController.java
├── mapper/
│   └── ProductMapper.java
├── assembler/
│   └── ProductResourceAssembler.java
└── migration/
    └── V1__create_products.sql
```

## SQL Type Mapping

| SQL Type | Java Type |
|----------|-----------|
| INT, INTEGER | Integer |
| BIGINT | Long |
| VARCHAR, TEXT | String |
| DECIMAL, NUMERIC | BigDecimal |
| BOOLEAN | Boolean |
| DATE | LocalDate |
| TIMESTAMP | LocalDateTime |

See [main README](../README.md) for complete documentation.
