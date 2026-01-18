# Security Module

El módulo `security` implementa **autenticación JWT** y **autorización basada en roles**.

## Estructura

```
security/
├── domain/
│   └── entity/
│       ├── User.java           # Usuario (implements UserDetails)
│       ├── Role.java           # Rol con permisos
│       ├── Permission.java     # Permiso individual
│       └── TokenBlacklist.java # Tokens revocados
├── application/
│   ├── dto/
│   │   ├── LoginRequestDTO.java
│   │   ├── RegisterRequestDTO.java
│   │   ├── AuthResponseDTO.java
│   │   └── RefreshTokenRequestDTO.java
│   └── service/
│       ├── AuthService.java
│       ├── UserService.java
│       └── TokenBlacklistService.java
└── infrastructure/
    ├── controller/
    │   └── AuthController.java    # /api/v1/auth/*
    ├── config/
    │   └── SecurityConfig.java    # Spring Security config
    ├── jwt/
    │   ├── JwtService.java        # Genera/valida tokens
    │   └── JwtAuthenticationFilter.java
    └── filter/
        └── RateLimitFilter.java   # Rate limiting para auth
```

## Flujo de Autenticación

```
┌─────────┐       POST /auth/login        ┌─────────────┐
│ Cliente │ ──────────────────────────────► │ AuthController │
│         │ { username, password }        │             │
└─────────┘                               └──────┬──────┘
                                                 │
                                                 ▼
                                          ┌─────────────┐
                                          │ AuthService │
                                          │ - Validate  │
                                          │ - Generate  │
                                          └──────┬──────┘
                                                 │
                                                 ▼
                                          ┌─────────────┐
                                          │ JwtService  │
                                          │ - Sign JWT  │
                                          └──────┬──────┘
                                                 │
┌─────────┐                               ◄──────┘
│ Cliente │ { accessToken, refreshToken }
│         │ ◄────────────────────────────
└─────────┘
```

## Endpoints

| Método | Endpoint | Descripción | Auth |
|--------|----------|-------------|------|
| POST | `/api/v1/auth/login` | Iniciar sesión | No |
| POST | `/api/v1/auth/register` | Registrar usuario | No |
| POST | `/api/v1/auth/refresh` | Renovar access token | No* |
| POST | `/api/v1/auth/logout` | Cerrar sesión | Sí |

*Requiere refresh token válido en body

## Estructura del JWT

### Access Token
```json
{
  "sub": "username",
  "userId": 1,
  "role": "ADMIN",
  "email": "user@example.com",
  "type": "access",
  "iat": 1700000000,
  "exp": 1700000900,
  "jti": "uuid-único"
}
```

### Refresh Token
```json
{
  "sub": "username",
  "userId": 1,
  "type": "refresh",
  "iat": 1700000000,
  "exp": 1700604800,
  "jti": "uuid-único"
}
```

## Roles y Permisos

| Rol | Permisos | Descripción |
|-----|----------|-------------|
| `ADMIN` | READ, CREATE, UPDATE, DELETE, ADMIN | Acceso total |
| `USER` | READ, CREATE, UPDATE | Usuario estándar |
| `GUEST` | READ | Solo lectura |

## Proteger Endpoints

```java
// Por rol
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminOnly() { }

// Múltiples roles
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<?> authenticated() { }

// Por permiso
@PreAuthorize("hasAuthority('DELETE')")
public ResponseEntity<?> canDelete() { }

// Expresión compleja
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public ResponseEntity<?> selfOrAdmin(@PathVariable Long userId) { }
```

## Configuración

```yaml
apigen:
  security:
    jwt:
      secret: ${JWT_SECRET}           # Base64, mín 64 chars
      expiration-minutes: 15          # Access token TTL
      refresh-expiration-minutes: 10080  # 7 días
      issuer: apigen

app:
  security:
    public-endpoints: /api/*/auth/**,/swagger-ui/**,/v3/api-docs/**
    auth-rate-limit:
      max-attempts: 5
      lockout-minutes: 15
```

## Seguridad Implementada

| Feature | Descripción |
|---------|-------------|
| **JWT Stateless** | Sin sesiones server-side |
| **Refresh Tokens** | Renovación sin re-login |
| **Token Blacklist** | Revocación en logout |
| **BCrypt** | Hash de contraseñas (strength 12) |
| **Rate Limiting** | Protección brute-force |
| **CORS** | Orígenes configurables |
| **Security Headers** | XSS, HSTS, Frame deny |
| **CSRF Disabled** | Apropiado para APIs stateless |

## Testing

```java
// Con usuario mock
@Test
@WithMockUser(username = "admin", roles = "ADMIN")
void adminShouldAccessProtected() { }

// Con JWT real
@Test
void shouldAuthenticateWithValidToken() {
    String token = jwtService.generateAccessToken(user);

    mockMvc.perform(get("/api/v1/products")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
}
```
