# CodeGen Module

The `apigen-codegen` module provides code generation utilities.

## Features

- **Entity Generation** - Generate entities from schema
- **DTO Generation** - Automatic DTO creation
- **OpenAPI Export** - Generate OpenAPI specs

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-codegen'
```

## Usage

The CodeGen module is primarily used by the APiGen Studio web application for generating Spring Boot projects from visual designs.

## API

The module exposes REST endpoints for code generation:

```bash
POST /api/codegen/generate
Content-Type: application/json

{
  "entities": [...],
  "config": {
    "groupId": "com.example",
    "artifactId": "my-api",
    "javaVersion": "25"
  }
}
```

Response: ZIP file with generated project.
