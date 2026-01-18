# JWT (JSON Web Tokens)

Implementación de generación y validación de tokens JWT.

## Archivos

| Archivo | Descripción |
|---------|-------------|
| `JwtService.java` | Genera y valida tokens |
| `JwtAuthenticationFilter.java` | Extrae JWT de requests |

## JwtService

### Generar Tokens

```java
// Access token (15 min default)
String accessToken = jwtService.generateAccessToken(user);

// Refresh token (7 días default)
String refreshToken = jwtService.generateRefreshToken(user);
```

### Validar Tokens

```java
// Validar y extraer claims
Claims claims = jwtService.validateToken(token);
String username = claims.getSubject();
Long userId = claims.get("userId", Long.class);
String role = claims.get("role", String.class);

// Verificar tipo
boolean isAccessToken = "access".equals(claims.get("type"));
```

### Estructura del Token

```
Header: { "alg": "HS256", "typ": "JWT" }
Payload: {
  "sub": "username",
  "userId": 1,
  "role": "ADMIN",
  "email": "user@example.com",
  "type": "access",
  "iat": 1700000000,
  "exp": 1700000900,
  "jti": "uuid-token-id"
}
Signature: HMACSHA256(header + "." + payload, secret)
```

## JwtAuthenticationFilter

Intercepta requests y extrae el JWT:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtService.validateToken(token);

                // Verificar blacklist
                if (tokenBlacklistService.isBlacklisted(claims.getId())) {
                    throw new JwtException("Token revocado");
                }

                // Crear Authentication
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        claims.getSubject(),
                        null,
                        getAuthorities(claims)
                    );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // Token inválido, continúa sin auth
            }
        }

        chain.doFilter(request, response);
    }
}
```

## Configuración

```yaml
apigen:
  security:
    jwt:
      secret: ${JWT_SECRET}           # Min 64 chars, Base64
      expiration-minutes: 15          # Access token
      refresh-expiration-minutes: 10080  # 7 días
      issuer: apigen
```

## Seguridad

| Aspecto | Implementación |
|---------|----------------|
| **Algoritmo** | HS256 (HMAC-SHA256) |
| **Secret** | Mínimo 256 bits (64 chars base64) |
| **JTI** | UUID único por token (para blacklist) |
| **Tipo** | Claim `type` distingue access/refresh |

## Manejo de Errores

| Error | Causa | HTTP Status |
|-------|-------|-------------|
| `ExpiredJwtException` | Token expirado | 401 |
| `MalformedJwtException` | Token mal formado | 401 |
| `SignatureException` | Firma inválida | 401 |
| Token en blacklist | Revocado en logout | 401 |
