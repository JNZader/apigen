# Security Module

The `apigen-security` module provides authentication, authorization, and security features for Spring Boot applications.

## Features

- **JWT Authentication** - HS256 with access + refresh tokens
- **OAuth2 Resource Server** - Auth0, Keycloak, Azure AD support
- **RBAC** - User > Roles > Permissions hierarchy
- **Rate Limiting** - Bucket4j with in-memory or Redis storage
- **Security Headers** - CSP, HSTS, X-Frame-Options
- **Account Protection** - Lockout after failed attempts

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-security:v2.18.0'
```

## Configuration

```yaml
apigen:
  security:
    enabled: true
    mode: jwt  # 'jwt' or 'oauth2'
    jwt:
      secret: ${JWT_SECRET}
      expiration-minutes: 15
      refresh-expiration-minutes: 10080
      issuer: apigen
    rate-limit:
      enabled: true
      storage-mode: in-memory  # or 'redis'
      requests-per-second: 100
    account-lockout:
      enabled: true
      max-failed-attempts: 5
      lockout-duration-minutes: 15
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

### Response

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 900
}
```

### Using the Token

```bash
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Refresh

```bash
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

## Role-Based Access Control

### Entity Structure

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;  // Argon2 hashed

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
```

### Securing Endpoints

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    // Only admins can access
}

@GetMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public List<Report> getReports() {
    // Admins and Managers can access
}
```

## Rate Limiting

### Configuration

```yaml
apigen:
  security:
    rate-limit:
      enabled: true
      storage-mode: in-memory  # or 'redis'
      requests-per-second: 100
      tier-limits:
        ANONYMOUS: 10
        USER: 100
        ADMIN: 1000
```

### Response Headers

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1642000000
```

### When Limit Exceeded

```
HTTP/1.1 429 Too Many Requests
Retry-After: 60
```

## Security Headers

Automatically configured headers:

| Header | Value |
|--------|-------|
| Content-Security-Policy | default-src 'self' |
| Strict-Transport-Security | max-age=31536000; includeSubDomains |
| X-Frame-Options | DENY |
| X-Content-Type-Options | nosniff |
| Referrer-Policy | strict-origin-when-cross-origin |

## Account Lockout

```yaml
apigen:
  security:
    account-lockout:
      enabled: true
      max-failed-attempts: 5
      lockout-duration-minutes: 15
      exponential-backoff: true
```

After 5 failed login attempts, the account is locked for 15 minutes.

## OAuth2 Resource Server

```yaml
apigen:
  security:
    mode: oauth2
    oauth2:
      issuer-uri: https://your-auth-server.com/
      audience: your-api-audience
```

## Password Requirements

Strong password validation:

- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 digit
- At least 1 special character
