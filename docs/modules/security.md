# Security Module

The `apigen-security` module provides authentication and authorization.

## Features

- **JWT Authentication** - Token-based auth
- **OAuth2/OIDC** - Social login support
- **RBAC** - Role-based access control
- **Rate Limiting** - Request throttling
- **Security Headers** - OWASP recommendations

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-security'
```

## Configuration

```yaml
apigen:
  security:
    jwt:
      secret: your-secret-key
      expiration: 86400000  # 24 hours
    rate-limiting:
      enabled: true
      requests-per-minute: 60
```

## JWT Authentication

### Login Endpoint

```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}
```

### Using the Token

```bash
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Role-Based Access Control

```java
@Entity
@ApiGenCrud
@PreAuthorize("hasRole('ADMIN')")
public class AdminResource extends BaseEntity {
    // Only admins can access
}
```

## Rate Limiting

Automatically applies rate limiting headers:

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1642000000
```
