# APiGen Security

JWT authentication module for APiGen applications.

## Purpose

Provides complete authentication flow with JWT access/refresh tokens, token blacklisting, rate limiting, and Spring Security integration.

## Features

- JWT authentication with HS512 algorithm
- Access token + Refresh token flow
- Token blacklisting for logout/revocation
- Rate limiting on authentication endpoints
- User, Role, and Permission entities
- Security audit logging
- Spring Security auto-configuration

## Usage

**Gradle:**
```groovy
dependencies {
    implementation 'com.jnzader:apigen-security:1.0.0-SNAPSHOT'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.jnzader</groupId>
    <artifactId>apigen-security</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
apigen:
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}  # Min 64 chars for HS512
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 days
```

## Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Authenticate user |
| POST | `/auth/register` | Register new user |
| POST | `/auth/refresh` | Refresh access token |
| POST | `/auth/logout` | Invalidate token |

## Entities

- `User` - User account with credentials
- `Role` - Role with permissions (ADMIN, USER, GUEST)
- `Permission` - Fine-grained permissions (READ, CREATE, UPDATE, DELETE)
- `TokenBlacklist` - Revoked tokens

See [main README](../README.md) for complete documentation.
