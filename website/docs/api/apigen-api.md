---
id: apigen-api
title: APiGen API
sidebar_label: Introduction
---

# APiGen REST API

This section provides comprehensive API documentation for APiGen.

## API Documentation Options

APiGen provides two views of the API documentation:

### 1. MDX-Based Documentation (This Section)

Interactive API documentation generated from OpenAPI specs using [docusaurus-plugin-openapi-docs](https://github.com/PaloAltoNetworks/docusaurus-openapi-docs).

Features:
- Integrated with the documentation site
- MDX customization support
- Works with Docusaurus search
- Version control friendly

### 2. Redoc View

A beautiful three-panel API reference at [/api/reference](/api/reference).

Features:
- Clean, responsive design
- Try-it-out functionality
- Code samples in multiple languages
- Fast navigation

## Base URL

```
http://localhost:8080
```

## Authentication

The API uses JWT Bearer tokens. Include your token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/api/products
```

## Common Headers

| Header | Description |
|--------|-------------|
| `Authorization` | Bearer token for authentication |
| `Content-Type` | `application/json` for request bodies |
| `Accept` | `application/hal+json` for HATEOAS responses |
| `If-Match` | ETag for optimistic locking (PUT/PATCH) |
| `If-None-Match` | ETag for conditional GET requests |
| `Accept-Language` | Locale for i18n (e.g., `es`, `en`) |

## Response Formats

### Success Response (HATEOAS)

```json
{
  "id": 1,
  "name": "Product Name",
  "price": 99.99,
  "_links": {
    "self": { "href": "/api/products/1" },
    "collection": { "href": "/api/products" }
  }
}
```

### Error Response (RFC 7807)

```json
{
  "type": "https://api.example.com/problems/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product with ID 999 was not found",
  "instance": "/api/products/999"
}
```

## Rate Limiting

The API includes rate limiting headers:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705312800
```

When exceeded, you'll receive a `429 Too Many Requests` response with a `Retry-After` header.

## Generating Fresh Documentation

To regenerate the API documentation from the OpenAPI spec:

```bash
cd website
npm run gen-api-docs
```
