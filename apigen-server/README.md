# APiGen Server

Standalone HTTP server for the code generation service.

## Purpose

Provides a REST API endpoint for generating code from SQL schemas without requiring local CLI installation.

## Usage

**Run the server:**
```bash
./gradlew :apigen-server:bootRun
```

**Generate code via API:**
```bash
curl -X POST http://localhost:8080/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "sql": "CREATE TABLE products (id BIGINT PRIMARY KEY, name VARCHAR(255));",
    "basePackage": "com.mycompany"
  }' \
  --output generated.zip
```

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/generate` | Generate code from SQL schema |

## Request Body

```json
{
  "sql": "CREATE TABLE ...",
  "basePackage": "com.example.myapp"
}
```

## Response

Returns a ZIP file containing all generated Java files.

See [main README](../README.md) for complete documentation.
