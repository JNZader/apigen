# My API

Generated with [APiGen Studio](https://github.com/jnzader/apigen)

## Quick Start

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

## API Documentation

Once running, access:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs
- H2 Console: http://localhost:8080/h2-console

## Project Structure

```
src/main/java/com/example/myapi/
├── Application.java           # Main entry point
├── [module]/
│   ├── domain/
│   │   ├── entity/           # JPA entities
│   │   └── repository/       # Spring Data repositories
│   ├── application/
│   │   ├── dto/              # Data Transfer Objects
│   │   ├── mapper/           # MapStruct mappers
│   │   └── service/          # Business services
│   └── infrastructure/
│       └── controller/       # REST controllers
```

## Features

- CRUD operations with pagination and filtering
- HATEOAS links
- Soft delete support
- Audit trails (created/updated by)
- OpenAPI documentation
- Caching support
