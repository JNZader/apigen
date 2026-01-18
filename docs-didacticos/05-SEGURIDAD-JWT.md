# 05 - Seguridad JWT: Autenticacion y Autorizacion

## Objetivo

Entender el modulo de seguridad que proporciona autenticacion JWT completa. Este documento cubre el modulo `apigen-security`:

- Sistema de autenticacion JWT dual-token
- Role-Based Access Control (RBAC)
- Token blacklist y rotacion
- Rate limiting para autenticacion

## Ubicacion en el Proyecto Multi-Modulo

```
apigen/
├── apigen-security/                  # <-- Modulo de seguridad (opcional)
│   └── src/main/java/com/jnzader/apigen/security/
│       ├── domain/
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Role.java
│       │   │   ├── Permission.java
│       │   │   └── TokenBlacklist.java
│       │   └── repository/
│       ├── application/
│       │   ├── dto/
│       │   │   ├── LoginRequestDTO.java
│       │   │   ├── AuthResponseDTO.java
│       │   │   └── RegisterRequestDTO.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── JwtService.java
│       │   │   └── TokenBlacklistService.java
│       │   └── validation/
│       │       └── StrongPasswordValidator.java
│       └── infrastructure/
│           ├── config/
│           │   ├── ApigenSecurityAutoConfiguration.java
│           │   ├── SecurityConfig.java
│           │   └── SecurityProperties.java
│           ├── controller/
│           │   └── AuthController.java
│           ├── filter/
│           │   ├── JwtAuthenticationFilter.java
│           │   └── AuthRateLimitFilter.java
│           └── jwt/
│               └── JwtService.java
│
└── apigen-example/                   # Ejemplo de uso
    └── src/main/resources/
        └── application.yaml          # Configura apigen.security.enabled
```

## Formas de Usar el Modulo de Seguridad

### Opcion 1: Como Dependencia (Recomendado)

```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // Agregar seguridad
}
```

```yaml
# application.yaml
apigen:
  security:
    enabled: true  # Habilitar seguridad
    jwt:
      secret: ${JWT_SECRET}  # REQUERIDO: variable de entorno
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 dias
```

### Opcion 2: Sin Seguridad (Desarrollo)

```yaml
# application.yaml
apigen:
  security:
    enabled: false  # Deshabilitado
```

---

## Introduccion

APiGen implementa un sistema de seguridad robusto basado en **JSON Web Tokens (JWT)** con las siguientes caracteristicas:

### Caracteristicas Principales

1. **Autenticacion JWT Dual Token**
   - Access tokens de corta duracion (15 minutos)
   - Refresh tokens de larga duracion (7 dias)
   - Rotacion automatica de refresh tokens

2. **Sistema RBAC (Role-Based Access Control)**
   - Roles jerarquicos
   - Permisos granulares
   - Cache de authorities para optimizar rendimiento

3. **Seguridad Avanzada**
   - Blacklist de tokens para revocacion
   - Rate limiting especifico para autenticacion
   - Proteccion contra fuerza bruta
   - Headers de seguridad (XSS, HSTS, CSP, Frame Options)

4. **Auditoria Completa**
   - Registro de eventos de seguridad
   - Tracking de IPs y User-Agents
   - Eventos asincronos para no impactar rendimiento

5. **Feature Flag**
   - Seguridad habilitada/deshabilitada via configuracion
   - Ideal para desarrollo y testing

---

## Arquitectura del Sistema de Seguridad

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP REQUEST                             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              AuthRateLimitFilter (Order: 1)                      │
│  - Limita intentos de login por IP (5 intentos / 15 min)        │
│  - Bloqueo temporal en caso de exceso                           │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│           JwtAuthenticationFilter (Order: 2)                     │
│  - Extrae JWT del header Authorization                          │
│  - Valida token y verifica blacklist                            │
│  - Puebla SecurityContext                                       │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Security                                │
│  - Autoriza acceso segun roles/permisos                         │
│  - Ejecuta @PreAuthorize, @Secured                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CONTROLLER                                  │
│                   (AuthController, etc.)                         │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              SecurityAuditListener (Async)                       │
│  - Registra eventos de autenticacion/autorizacion               │
│  - Log estructurado para SIEM                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Modelo de Datos

### Diagrama ER de Seguridad

```
┌──────────────────┐           ┌──────────────────┐
│      User        │           │       Role       │
├──────────────────┤           ├──────────────────┤
│ id (PK)          │  N     1  │ id (PK)          │
│ username (UK)    ├───────────┤ name (UK)        │
│ password         │           │ description      │
│ email (UK)       │           └─────────┬────────┘
│ firstName        │                     │
│ lastName         │                     │ M
│ role_id (FK)     │                     │
│ enabled          │                     │ N
│ accountNonExpired│           ┌─────────┴────────┐
│ accountNonLocked │           │   Permission     │
│ credentialsNon...│           ├──────────────────┤
│ lastLoginAt      │           │ id (PK)          │
│ lastLoginIp      │           │ name (UK)        │
└──────────────────┘           │ description      │
                               │ category         │
                               └──────────────────┘

┌──────────────────┐
│ TokenBlacklist   │
├──────────────────┤
│ id (PK)          │
│ tokenId (UK)     │   <- JTI claim del JWT
│ username         │
│ expiration       │
│ blacklistedAt    │
│ reason           │   <- LOGOUT, PASSWORD_CHANGE, etc.
└──────────────────┘
```

### 1. User Entity

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/domain/entity/User.java`

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_email", columnList = "email")
})
public class User extends Base implements UserDetails {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Estados de la cuenta (UserDetails)
    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @Column(nullable = false)
    private boolean credentialsNonExpired = true;

    @Column(nullable = false)
    private boolean enabled = true;

    // Tracking de login
    @Column
    private Instant lastLoginAt;

    @Column
    private String lastLoginIp;

    // Cache de authorities para evitar N+1 queries
    @Transient
    private transient Collection<? extends GrantedAuthority> cachedAuthorities;
}
```

#### Puntos Clave:

1. **Extiende Base**: Hereda campos de auditoria (id, fechaCreacion, creadoPor, etc.)

2. **Implementa UserDetails**: Integracion directa con Spring Security
   - `getAuthorities()`: Retorna rol + permisos como `GrantedAuthority`
   - `getPassword()`, `getUsername()`: Para autenticacion
   - `isEnabled()`: Combina `enabled` con `estado` de Base

3. **Cache de Authorities**:
   ```java
   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
       if (cachedAuthorities != null) {
           return cachedAuthorities;
       }

       // Construir desde rol.permissions + ROLE_XXX
       Set<SimpleGrantedAuthority> authorities = role.getPermissions()
           .stream()
           .map(p -> new SimpleGrantedAuthority(p.getName()))
           .collect(Collectors.toSet());

       authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

       this.cachedAuthorities = Collections.unmodifiableSet(authorities);
       return cachedAuthorities;
   }
   ```
   - **Problema resuelto**: Evita N+1 queries al acceder a permisos multiples veces
   - **Invalidacion**: Se limpia al cambiar el rol via `setRole()`

4. **Indices**: Optimizan busquedas por username y email (queries frecuentes)

---

### 2. Role Entity

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/domain/entity/Role.java`

```java
@Entity
@Table(name = "roles")
public class Role extends Base {

    @Column(nullable = false, unique = true, length = 50)
    private String name;  // ADMIN, MANAGER, USER, GUEST

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @BatchSize(size = 25)  // Optimizacion para carga batch
    private Set<Permission> permissions = new HashSet<>();

    public boolean hasPermission(String permissionName) {
        return permissions.stream()
            .anyMatch(p -> p.getName().equals(permissionName));
    }
}
```

#### Puntos Clave:

1. **ManyToMany con Permissions**: Un rol puede tener multiples permisos
2. **@BatchSize(25)**: Optimiza N+1 queries al cargar permisos
3. **Convencion de nombres**: UPPERCASE (ADMIN, USER, MANAGER)
4. **Helper method**: `hasPermission()` simplifica verificaciones

---

### 3. Permission Entity

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/domain/entity/Permission.java`

```java
@Entity
@Table(name = "permissions")
public class Permission extends Base {

    @Column(nullable = false, unique = true, length = 100)
    private String name;  // CREATE_USER, READ_PRODUCT, DELETE_ORDER

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String category;  // USERS, PRODUCTS, ORDERS, SYSTEM
}
```

#### Convencion de Nombres:

```
VERBO_RECURSO
├── CREATE_USER
├── READ_USERS
├── UPDATE_USER
├── DELETE_USER
├── CREATE_PRODUCT
├── READ_PRODUCTS
└── EXPORT_DATA
```

#### Categorias:
- **USERS**: Gestion de usuarios
- **PRODUCTS**: Gestion de productos
- **ORDERS**: Gestion de pedidos
- **SYSTEM**: Configuracion del sistema
- **REPORTS**: Reportes y exportacion

---

### 4. TokenBlacklist Entity

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/domain/entity/TokenBlacklist.java`

```java
@Entity
@Table(name = "token_blacklist", indexes = {
    @Index(name = "idx_token_blacklist_token_id", columnList = "token_id"),
    @Index(name = "idx_token_blacklist_expiration", columnList = "expiration"),
    @Index(name = "idx_token_blacklist_username", columnList = "username")
})
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;  // JTI claim del JWT

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private Instant expiration;  // Para cleanup automatico

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private BlacklistReason reason;

    public enum BlacklistReason {
        LOGOUT,
        PASSWORD_CHANGE,
        ADMIN_REVOKE,
        SECURITY_BREACH,
        SESSION_EXPIRED,
        TOKEN_ROTATED  // Usado en refresh token rotation
    }
}
```

#### Puntos Clave:

1. **tokenId**: Es el claim `jti` (JWT ID) del token
2. **expiration**: Permite cleanup automatico de tokens ya expirados
3. **Indices**:
   - `token_id`: Busqueda rapida para validacion
   - `expiration`: Optimiza cleanup scheduled
   - `username`: Revocar todos los tokens de un usuario

4. **Razones de blacklist**:
   - `LOGOUT`: Usuario cerro sesion
   - `PASSWORD_CHANGE`: Cambio de contrasena invalida tokens previos
   - `ADMIN_REVOKE`: Administrador revoco acceso
   - `TOKEN_ROTATED`: Refresh token usado (single-use)

---

## Servicios Core

### 1. JwtService

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/application/service/JwtService.java`

```java
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class JwtService {

    private final SecurityProperties securityProperties;
    private final TokenBlacklistService blacklistService;
    private final SecretKey secretKey;

    public JwtService(SecurityProperties props, TokenBlacklistService blacklist) {
        this.securityProperties = props;
        this.blacklistService = blacklist;
        this.secretKey = Keys.hmacShaKeyFor(
            props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }
}
```

#### Generacion de Access Token

```java
public String generateAccessToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("role", user.getRole().getName());
    claims.put("email", user.getEmail());
    claims.put("type", "access");  // Importante: tipo de token

    return buildToken(
        claims,
        user.getUsername(),
        securityProperties.getJwt().getExpirationMinutes()
    );
}
```

**Claims Incluidos:**
- `sub`: username (subject del JWT)
- `userId`: ID del usuario
- `role`: Rol del usuario (ej: "ADMIN")
- `email`: Email del usuario
- `type`: "access" (distingue de refresh token)
- `jti`: ID unico del token (UUID)
- `iss`: Issuer (ej: "apigen")
- `iat`: Issued at (timestamp de creacion)
- `exp`: Expiration (timestamp de expiracion)

#### Generacion de Refresh Token

```java
public String generateRefreshToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    claims.put("type", "refresh");  // Tipo: refresh

    return buildToken(
        claims,
        user.getUsername(),
        securityProperties.getJwt().getRefreshExpirationMinutes()
    );
}
```

**Diferencias con Access Token:**
- **Claims minimos**: Solo userId y type (menos informacion sensible)
- **Duracion mayor**: 7 dias vs 15 minutos
- **Uso unico**: Se invalida al usarse (rotacion)

#### Construccion de Token

```java
private String buildToken(Map<String, Object> claims, String subject, int expirationMinutes) {
    Instant now = Instant.now();
    Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

    return Jwts.builder()
        .id(UUID.randomUUID().toString())  // JTI: Token ID unico
        .claims(claims)
        .subject(subject)
        .issuer(securityProperties.getJwt().getIssuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(secretKey, Jwts.SIG.HS256)  // HMAC-SHA256
        .compact();
}
```

#### Validacion de Token

```java
public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
        final String username = extractUsername(token);
        final String tokenId = extractTokenId(token);

        // 1. Verificar blacklist
        if (blacklistService.isBlacklisted(tokenId)) {
            log.debug("Token {} esta en blacklist", tokenId);
            return false;
        }

        // 2. Verificar username y expiracion
        return username.equals(userDetails.getUsername())
            && !isTokenExpired(token);

    } catch (JwtException | IllegalArgumentException e) {
        log.debug("Token invalido: {}", e.getMessage());
        return false;
    }
}
```

**Proceso de Validacion:**
1. Extrae username y tokenId del JWT
2. Verifica si esta en blacklist
3. Compara username con UserDetails
4. Verifica que no este expirado

#### Extraccion de Claims

```java
public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
}

public String extractTokenId(String token) {
    return extractClaim(token, Claims::getId);
}

public Long extractUserId(String token) {
    return extractClaim(token, claims -> claims.get("userId", Long.class));
}

public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
}

public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
}

private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
}
```

#### Manejo de Tokens Expirados (Refresh)

```java
public Claims extractClaimsIgnoringExpiration(String token) {
    try {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    } catch (ExpiredJwtException e) {
        return e.getClaims();  // Recupera claims del token expirado
    }
}
```

**Uso:** Permite leer claims de refresh tokens expirados para generar nuevos tokens.

---

### 2. AuthService

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/application/service/AuthService.java`

```java
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
}
```

#### Login

```java
@Transactional(readOnly = true)
public AuthResponseDTO login(LoginRequestDTO request) {
    // 1. Autenticar credenciales (lanza excepcion si falla)
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.username(),
            request.password()
        )
    );

    // 2. Cargar usuario
    User user = userRepository.findActiveByUsername(request.username())
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    // 3. Generar tokens
    return generateAuthResponse(user);
}
```

**Flujo:**
1. `AuthenticationManager` valida username + password
2. Si credenciales son incorrectas, lanza `BadCredentialsException`
3. Si correctas, carga el usuario activo
4. Genera access token + refresh token

#### Registro

```java
@Transactional
public AuthResponseDTO register(RegisterRequestDTO request) {
    // 1. Validar unicidad
    if (userRepository.findByUsername(request.username()).isPresent()) {
        throw new RuntimeException("El nombre de usuario ya existe");
    }
    if (userRepository.findByEmail(request.email()).isPresent()) {
        throw new RuntimeException("El email ya esta registrado");
    }

    // 2. Obtener rol por defecto
    Role defaultRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("Rol por defecto no encontrado"));

    // 3. Crear usuario
    User user = new User();
    user.setUsername(request.username());
    user.setPassword(passwordEncoder.encode(request.password()));
    user.setEmail(request.email());
    user.setFirstName(request.firstName());
    user.setLastName(request.lastName());
    user.setRole(defaultRole);
    user.setEnabled(true);

    user = userRepository.save(user);

    // 4. Generar tokens
    return generateAuthResponse(user);
}
```

#### Refresh Token (con Rotacion)

```java
@Transactional
public AuthResponseDTO refreshToken(RefreshTokenRequestDTO request) {
    String refreshToken = request.refreshToken();

    // 1. Validar que sea un refresh token
    if (!jwtService.isRefreshToken(refreshToken)) {
        throw new RuntimeException("Token invalido: no es un refresh token");
    }

    // 2. Validar estructura
    if (!jwtService.isTokenStructureValid(refreshToken)) {
        throw new RuntimeException("Refresh token invalido o expirado");
    }

    // 3. Verificar blacklist
    String tokenId = jwtService.extractTokenId(refreshToken);
    if (tokenBlacklistService.isBlacklisted(tokenId)) {
        throw new RuntimeException("Refresh token ya fue utilizado o revocado");
    }

    // 4. Extraer usuario
    String username = jwtService.extractUsername(refreshToken);
    User user = userRepository.findActiveByUsername(username)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    // 5. ROTACION: Invalidar refresh token usado (single-use)
    tokenBlacklistService.blacklistToken(
        tokenId,
        username,
        jwtService.extractExpiration(refreshToken),
        BlacklistReason.TOKEN_ROTATED
    );

    // 6. Generar NUEVOS tokens (access + refresh)
    return generateAuthResponse(user);
}
```

**Refresh Token Rotation:**
- Cada refresh token se puede usar **una sola vez**
- Al usarse, se invalida (blacklist) y se genera uno nuevo
- Mejora seguridad: si un refresh token es robado, solo puede usarse una vez

#### Logout

```java
@Transactional
public void logout(String token) {
    String tokenId = jwtService.extractTokenId(token);
    String username = jwtService.extractUsername(token);
    Instant expiration = jwtService.extractExpiration(token);

    tokenBlacklistService.blacklistToken(
        tokenId,
        username,
        expiration,
        BlacklistReason.LOGOUT
    );
}
```

#### Generar Respuesta de Autenticacion

```java
private AuthResponseDTO generateAuthResponse(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    var userInfo = new AuthResponseDTO.UserInfoDTO(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFullName(),
        user.getRole().getName(),
        user.getRole().getPermissions().stream()
            .map(Permission::getName)
            .collect(Collectors.toSet())
    );

    return new AuthResponseDTO(
        accessToken,
        refreshToken,
        jwtService.extractExpiration(accessToken),
        userInfo
    );
}
```

---

### 3. TokenBlacklistService

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/application/service/TokenBlacklistService.java`

```java
@Service
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class TokenBlacklistService {

    private final TokenBlacklistRepository repository;
}
```

#### Blacklist Token

```java
@Transactional
public void blacklistToken(String tokenId, String username,
                          Instant expiration, BlacklistReason reason) {
    if (isBlacklisted(tokenId)) {
        log.debug("Token {} ya esta en la blacklist", tokenId);
        return;
    }

    TokenBlacklist entry = new TokenBlacklist(tokenId, username, expiration, reason);
    repository.save(entry);
    log.info("Token {} del usuario {} anadido a blacklist. Razon: {}",
             tokenId, username, reason);
}
```

#### Verificar Blacklist

```java
@Transactional(readOnly = true)
public boolean isBlacklisted(String tokenId) {
    return repository.existsByTokenId(tokenId);
}
```

#### Cleanup Automatico

```java
@Scheduled(fixedRate = 3600000)  // Cada hora
@Transactional
public void cleanupExpiredTokens() {
    int deleted = repository.deleteExpiredTokens(Instant.now());
    if (deleted > 0) {
        log.info("Limpiados {} tokens expirados de la blacklist", deleted);
    }
}
```

**Optimizacion:** Elimina tokens cuya expiracion ya paso (no es necesario mantenerlos)

---

## Filtros y Middleware

### 1. JwtAuthenticationFilter

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/infrastructure/filter/JwtAuthenticationFilter.java`

```java
@Component
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
}
```

#### Flujo de Procesamiento

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException {

    final String authHeader = request.getHeader(AUTHORIZATION_HEADER);

    // 1. Verificar presencia de header Authorization
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
        filterChain.doFilter(request, response);
        return;
    }

    try {
        // 2. Extraer JWT
        final String jwt = authHeader.substring(BEARER_PREFIX.length());
        final String username = jwtService.extractUsername(jwt);

        // 3. Si hay username y no hay autenticacion previa
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Cargar UserDetails
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Validar token (tipo access + validez + blacklist)
            if (jwtService.isAccessToken(jwt) && jwtService.isTokenValid(jwt, userDetails)) {

                // 6. Crear autenticacion
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );

                // 7. Agregar detalles de request
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 8. Poblar SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Usuario autenticado: {}", username);
            }
        }
    } catch (Exception e) {
        log.debug("Error procesando token JWT: {}", e.getMessage());
        // No lanzar excepcion, continuar sin autenticacion
    }

    // 9. Continuar cadena de filtros
    filterChain.doFilter(request, response);
}
```

**Puntos Clave:**

1. **OncePerRequestFilter**: Garantiza ejecucion una sola vez por request
2. **No lanza excepciones**: Si el token es invalido, simplemente no autentica
3. **Verifica tipo de token**: Solo access tokens, no refresh tokens
4. **Pobla SecurityContext**: Permite que `@PreAuthorize`, `@Secured` funcionen

#### Exclusion de Endpoints

```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/api/auth/");  // No filtrar login, register, etc.
}
```

---

### 2. AuthRateLimitFilter

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/infrastructure/filter/AuthRateLimitFilter.java`

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)  // Antes del JwtAuthenticationFilter
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final int maxLoginAttempts;  // Default: 5
    private final Duration lockoutDuration;  // Default: 15 minutos
    private final Cache<String, AtomicInteger> loginAttempts;
}
```

#### Inicializacion

```java
public AuthRateLimitFilter(
    @Value("${app.security.auth-rate-limit.max-attempts:5}") int maxAttempts,
    @Value("${app.security.auth-rate-limit.lockout-minutes:15}") int lockoutMinutes
) {
    this.maxLoginAttempts = maxAttempts;
    this.lockoutDuration = Duration.ofMinutes(lockoutMinutes);

    this.loginAttempts = Caffeine.newBuilder()
        .expireAfterWrite(lockoutDuration)  // Auto-expira despues de 15 min
        .maximumSize(10000)  // Max 10k IPs en tracking
        .build();
}
```

**Caffeine Cache:**
- **expireAfterWrite**: Entradas expiran automaticamente
- **maximumSize**: Limita memoria usada
- **Thread-safe**: Maneja concurrencia

#### Procesamiento

```java
@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
) throws ServletException, IOException {

    // Solo aplicar a endpoints de login
    if (!isLoginEndpoint(request)) {
        filterChain.doFilter(request, response);
        return;
    }

    String clientIp = getClientIp(request);
    AtomicInteger attempts = loginAttempts.get(clientIp, k -> new AtomicInteger(0));

    // Verificar si esta bloqueado
    if (attempts.get() >= maxLoginAttempts) {
        log.warn("Auth rate limit exceeded for IP: {}", clientIp);
        sendRateLimitResponse(response, clientIp);
        return;
    }

    // Agregar headers informativos
    response.setHeader("X-Auth-RateLimit-Limit", String.valueOf(maxLoginAttempts));
    response.setHeader("X-Auth-RateLimit-Remaining",
        String.valueOf(Math.max(0, maxLoginAttempts - attempts.get())));

    // Continuar con la cadena
    filterChain.doFilter(request, response);

    // Post-procesamiento: incrementar en caso de fallo
    if (response.getStatus() == 401 || response.getStatus() == 403) {
        int currentAttempts = attempts.incrementAndGet();
        log.info("Failed login attempt #{} from IP: {}", currentAttempts, clientIp);

        if (currentAttempts >= maxLoginAttempts) {
            log.warn("IP {} blocked after {} failed attempts", clientIp, currentAttempts);
        }
    } else if (response.getStatus() == 200) {
        // Reset en login exitoso
        loginAttempts.invalidate(clientIp);
        log.debug("Successful login from IP: {}, resetting counter", clientIp);
    }
}
```

#### Extraccion de IP del Cliente

```java
private String getClientIp(HttpServletRequest request) {
    String[] headerNames = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP"
    };

    for (String header : headerNames) {
        String ip = request.getHeader(header);
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();  // Primera IP en cadena
        }
    }

    return request.getRemoteAddr();
}
```

**Proteccion contra Proxies:** Verifica headers de proxy para obtener IP real del cliente.

---

## Controladores y DTOs

### 1. AuthController

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/infrastructure/controller/AuthController.java`

```java
@RestController
@RequestMapping("${app.api.base-path:}/v1/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticacion")
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class AuthController {

    private final AuthService authService;
}
```

#### Endpoint: Login

```java
@Operation(summary = "Iniciar sesion")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Login exitoso"),
    @ApiResponse(responseCode = "401", description = "Credenciales invalidas")
})
@PostMapping("/login")
public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    return ResponseEntity.ok(authService.login(request));
}
```

**Request:**
```json
{
  "username": "admin",
  "password": "AdminPass123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2025-11-27T15:30:00Z",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "fullName": "Admin User",
    "role": "ADMIN",
    "permissions": ["CREATE_USER", "READ_USERS", "UPDATE_USER", "DELETE_USER"]
  }
}
```

#### Endpoint: Register

```java
@Operation(summary = "Registrar usuario")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Registro exitoso"),
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
})
@PostMapping("/register")
public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
    return ResponseEntity.ok(authService.register(request));
}
```

**Request:**
```json
{
  "username": "johndoe",
  "password": "SecurePass123!",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Endpoint: Refresh Token

```java
@Operation(summary = "Refrescar token")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Tokens renovados"),
    @ApiResponse(responseCode = "401", description = "Refresh token invalido")
})
@PostMapping("/refresh")
public ResponseEntity<AuthResponseDTO> refreshToken(
    @Valid @RequestBody RefreshTokenRequestDTO request
) {
    return ResponseEntity.ok(authService.refreshToken(request));
}
```

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Endpoint: Logout

```java
@Operation(
    summary = "Cerrar sesion",
    security = @SecurityRequirement(name = "bearerAuth")
)
@PostMapping("/logout")
public ResponseEntity<Void> logout(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        authService.logout(token);
    }

    return ResponseEntity.noContent().build();
}
```

**Request:**
```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### 2. DTOs

#### LoginRequestDTO

```java
public record LoginRequestDTO(
    @NotBlank(message = "El nombre de usuario es requerido")
    String username,

    @NotBlank(message = "La contrasena es requerida")
    String password
) {}
```

#### RegisterRequestDTO

```java
public record RegisterRequestDTO(
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
             message = "Solo letras, numeros y guiones bajos")
    String username,

    @NotBlank
    @StrongPassword  // Validador personalizado
    String password,

    @NotBlank
    @Email
    @Size(max = 100)
    String email,

    @Size(max = 100)
    String firstName,

    @Size(max = 100)
    String lastName
) {}
```

#### AuthResponseDTO

```java
public record AuthResponseDTO(
    String accessToken,
    String refreshToken,
    String tokenType,
    Instant expiresAt,
    UserInfoDTO user
) {
    public record UserInfoDTO(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        Set<String> permissions
    ) {}
}
```

---

## Configuracion

### 1. SecurityConfig

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/infrastructure/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ConditionalOnProperty(name = "apigen.security.enabled", havingValue = "true")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // CSRF deshabilitado (API REST stateless)
            .csrf(AbstractHttpConfigurer::disable)

            // Sesion stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Headers de seguridad
            .headers(headers -> headers
                .xssProtection(xss -> xss.headerValue(ENABLED_MODE_BLOCK))
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> {})
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self'; " +
                    "style-src 'self'; " +
                    "img-src 'self' data:; " +
                    "frame-ancestors 'none'"
                ))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
            )

            // Autorizacion
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            // Proveedor de autenticacion
            .authenticationProvider(authenticationProvider())

            // Filtro JWT
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

#### CORS Configuration

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOrigins(List.of("http://localhost:3000"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization", "ETag", "Last-Modified"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
}
```

#### Password Encoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // Factor de trabajo 12 (mas seguro que default 10)
}
```

**Benchmark:**
- Factor 10: ~65ms
- Factor 12: ~260ms
- Factor 14: ~1040ms

**Recomendacion:** 12 es un buen balance entre seguridad y rendimiento.

---

### 2. SecurityProperties

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/infrastructure/config/SecurityProperties.java`

```java
@Component
@ConfigurationProperties(prefix = "apigen.security")
public class SecurityProperties {

    private boolean enabled = false;
    private JwtProperties jwt = new JwtProperties();

    @PostConstruct
    public void validate() {
        if (enabled) {
            validateJwtSecret();
        }
    }

    private void validateJwtSecret() {
        if (jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET es requerido cuando apigen.security.enabled=true"
            );
        }

        int secretLengthBytes = jwt.getSecret().getBytes(StandardCharsets.UTF_8).length;
        if (secretLengthBytes < 32) {  // 256 bits minimo para HS256
            throw new IllegalStateException(
                "JWT_SECRET debe tener al menos 32 bytes (256 bits)"
            );
        }
    }

    public static class JwtProperties {
        private String secret;
        private int expirationMinutes = 15;
        private int refreshExpirationMinutes = 10080;  // 7 dias
        private String issuer = "apigen";
    }
}
```

#### Configuracion en application.yml

```yaml
apigen:
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}  # Variable de entorno REQUERIDA
      expiration-minutes: 15
      refresh-expiration-minutes: 10080  # 7 dias
      issuer: apigen

app:
  security:
    auth-rate-limit:
      max-attempts: 5
      lockout-minutes: 15
```

#### Generar JWT_SECRET Seguro

```bash
# Generar secret de 64 bytes (512 bits)
openssl rand -base64 64

# Ejemplo de output:
# Ym9KcXJxdXJ5c3A3dHhkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjJkMjI=
```

---

## Validacion y Auditoria

### 1. StrongPassword Validator

**Ubicacion:** `apigen-security/src/main/java/com/jnzader/apigen/security/application/validation/StrongPasswordValidator.java`

```java
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "La contrasena debe tener al menos 12 caracteres, " +
                            "incluyendo mayuscula, minuscula, numero y caracter especial";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int minLength() default 12;
    int maxLength() default 128;
}
```

#### Implementacion

```java
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private int minLength;
    private int maxLength;

    @Override
    public void initialize(StrongPassword annotation) {
        this.minLength = annotation.minLength();
        this.maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return false;
        }

        StringBuilder errors = new StringBuilder();

        if (password.length() < minLength) {
            errors.append(String.format("Minimo %d caracteres. ", minLength));
        }

        if (password.length() > maxLength) {
            errors.append(String.format("Maximo %d caracteres. ", maxLength));
        }

        if (!UPPERCASE.matcher(password).find()) {
            errors.append("Requiere mayuscula. ");
        }

        if (!LOWERCASE.matcher(password).find()) {
            errors.append("Requiere minuscula. ");
        }

        if (!DIGIT.matcher(password).find()) {
            errors.append("Requiere numero. ");
        }

        if (!SPECIAL.matcher(password).find()) {
            errors.append("Requiere caracter especial. ");
        }

        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Contrasena invalida: " + errors.toString().trim()
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
```

#### Uso

```java
public record RegisterRequestDTO(
    @StrongPassword
    String password
) {}
```

---

### 2. Security Audit System

#### SecurityAuditEvent

```java
public record SecurityAuditEvent(
    Instant timestamp,
    SecurityEventType eventType,
    String username,
    String ipAddress,
    String userAgent,
    String resource,
    String action,
    SecurityOutcome outcome,
    Map<String, Object> details
) {
    public enum SecurityEventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_FAILURE,
        ACCESS_DENIED,
        SESSION_CREATED,
        PASSWORD_CHANGE,
        ACCOUNT_LOCKED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED,
        RESOURCE_ACCESS,
        DATA_EXPORT,
        ADMIN_ACTION
    }

    public enum SecurityOutcome {
        SUCCESS,
        FAILURE,
        DENIED,
        BLOCKED
    }
}
```

#### SecurityAuditService

```java
@Service
public class SecurityAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    private final ApplicationEventPublisher eventPublisher;

    public void logAuthenticationSuccess(String username) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(AUTHENTICATION_SUCCESS)
            .username(username)
            .ipAddress(getClientIp())
            .userAgent(getUserAgent())
            .action("LOGIN")
            .outcome(SUCCESS)
            .build();

        logEvent(event);
    }

    public void logAuthenticationFailure(String username, String reason) {
        SecurityAuditEvent event = SecurityAuditEvent.builder()
            .eventType(AUTHENTICATION_FAILURE)
            .username(username)
            .ipAddress(getClientIp())
            .action("LOGIN")
            .outcome(FAILURE)
            .details(Map.of("reason", reason))
            .build();

        logEvent(event);
    }

    private void logEvent(SecurityAuditEvent event) {
        // Log estructurado
        auditLog.info("SECURITY_EVENT type={} user={} ip={} outcome={}",
            event.eventType(),
            event.username(),
            event.ipAddress(),
            event.outcome()
        );

        // Publicar evento
        eventPublisher.publishEvent(event);
    }
}
```

#### SecurityAuditListener

```java
@Component
public class SecurityAuditListener {

    private final SecurityAuditService auditService;

    @EventListener
    @Async("domainEventExecutor")
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        auditService.logAuthenticationSuccess(username);
    }

    @EventListener
    @Async("domainEventExecutor")
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        auditService.logAuthenticationFailure(username, event.getException().getMessage());
    }

    @EventListener
    @Async("domainEventExecutor")
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        String username = event.getAuthentication().get().getName();
        auditService.logAccessDenied(event.getSource().toString(), "ACCESS_DENIED");
    }
}
```

**Procesamiento Asincrono:** Los eventos se procesan en background para no impactar rendimiento.

---

## Flujos de Autenticacion

### 1. Flujo de Login

```
┌──────────┐                                    ┌──────────────┐
│  Client  │                                    │   Backend    │
└────┬─────┘                                    └──────┬───────┘
     │                                                 │
     │  POST /api/v1/auth/login                       │
     │  { username, password }                        │
     ├────────────────────────────────────────────────>│
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthRateLimitFilter    │
     │                        │ - Verifica intentos IP │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthController.login() │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthService.login()    │
     │                        │ 1. Autentica creds     │
     │                        │ 2. Carga usuario       │
     │                        │ 3. Genera tokens       │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ JwtService             │
     │                        │ - Access token (15min) │
     │                        │ - Refresh token (7d)   │
     │                        └────────────────────────┤
     │                                                 │
     │  200 OK                                         │
     │  {                                              │
     │    "accessToken": "eyJ...",                     │
     │    "refreshToken": "eyJ...",                    │
     │    "expiresAt": "...",                          │
     │    "user": { ... }                              │
     │  }                                              │
     │<────────────────────────────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ SecurityAuditListener  │
     │                        │ - Log evento LOGIN     │
     │                        └────────────────────────┤
     │                                                 │
```

### 2. Flujo de Request Autenticado

```
┌──────────┐                                    ┌──────────────┐
│  Client  │                                    │   Backend    │
└────┬─────┘                                    └──────┬───────┘
     │                                                 │
     │  GET /api/v1/products                          │
     │  Authorization: Bearer eyJ...                  │
     ├────────────────────────────────────────────────>│
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ JwtAuthenticationFilter│
     │                        │ 1. Extrae JWT          │
     │                        │ 2. Valida estructura   │
     │                        │ 3. Verifica blacklist  │
     │                        │ 4. Carga UserDetails   │
     │                        │ 5. Puebla SecurityCtx  │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ Spring Security        │
     │                        │ - Autoriza acceso      │
     │                        │ - Ejecuta @PreAuthorize│
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ ProductController      │
     │                        │ - Procesa request      │
     │                        └────────────────────────┤
     │                                                 │
     │  200 OK                                         │
     │  [ { "id": 1, ... } ]                           │
     │<────────────────────────────────────────────────┤
     │                                                 │
```

### 3. Flujo de Refresh Token

```
┌──────────┐                                    ┌──────────────┐
│  Client  │                                    │   Backend    │
└────┬─────┘                                    └──────┬───────┘
     │                                                 │
     │  POST /api/v1/auth/refresh                     │
     │  { "refreshToken": "eyJ..." }                  │
     ├────────────────────────────────────────────────>│
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthService            │
     │                        │ 1. Valida tipo refresh │
     │                        │ 2. Verifica blacklist  │
     │                        │ 3. Extrae username     │
     │                        │ 4. INVALIDA token usado│
     │                        │ 5. Genera NUEVOS tokens│
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ TokenBlacklistService  │
     │                        │ - Blacklist old refresh│
     │                        │   (TOKEN_ROTATED)      │
     │                        └────────────────────────┤
     │                                                 │
     │  200 OK                                         │
     │  {                                              │
     │    "accessToken": "NEW_eyJ...",                 │
     │    "refreshToken": "NEW_eyJ...",                │
     │    ...                                          │
     │  }                                              │
     │<────────────────────────────────────────────────┤
     │                                                 │
```

**Refresh Token Rotation:**
- Cada refresh token es de **un solo uso**
- Al usarse, se invalida y se genera uno nuevo
- Previene reuso malicioso de tokens robados

### 4. Flujo de Logout

```
┌──────────┐                                    ┌──────────────┐
│  Client  │                                    │   Backend    │
└────┬─────┘                                    └──────┬───────┘
     │                                                 │
     │  POST /api/v1/auth/logout                      │
     │  Authorization: Bearer eyJ...                  │
     ├────────────────────────────────────────────────>│
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ JwtAuthenticationFilter│
     │                        │ - Autentica request    │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthController.logout()│
     │                        │ - Extrae token         │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ AuthService.logout()   │
     │                        └────────────────────────┤
     │                                                 │
     │                        ┌────────────────────────┤
     │                        │ TokenBlacklistService  │
     │                        │ - Blacklist token      │
     │                        │   (LOGOUT)             │
     │                        └────────────────────────┤
     │                                                 │
     │  204 No Content                                 │
     │<────────────────────────────────────────────────┤
     │                                                 │
```

---

## Mejores Practicas

### 1. Seguridad de Tokens

#### Duracion de Tokens

```yaml
# RECOMENDADO
jwt:
  expiration-minutes: 15        # Access token: corto
  refresh-expiration-minutes: 10080  # Refresh token: 7 dias
```

**Rationale:**
- **Access tokens cortos**: Limitan ventana de exposicion si son robados
- **Refresh tokens largos**: Evitan logins frecuentes, pero con rotacion

#### Almacenamiento en Cliente

**NUNCA hacer:**
```javascript
// MAL: localStorage es vulnerable a XSS
localStorage.setItem('accessToken', token);
```

**HACER:**
```javascript
// BIEN: httpOnly cookie (no accesible desde JS)
// Backend setea:
response.cookie('accessToken', token, {
  httpOnly: true,
  secure: true,  // Solo HTTPS
  sameSite: 'strict',
  maxAge: 15 * 60 * 1000  // 15 minutos
});

// O memoria del cliente (se pierde al refresh, pero mas seguro)
const authContext = createContext();
```

### 2. Rate Limiting

```yaml
app:
  security:
    auth-rate-limit:
      max-attempts: 5      # Maximo 5 intentos
      lockout-minutes: 15  # Bloqueo de 15 minutos
```

**Proteccion contra:**
- Ataques de fuerza bruta
- Credential stuffing
- Enumeracion de usuarios

### 3. Password Policy

```java
@StrongPassword(minLength = 12, maxLength = 128)
String password;
```

**Requisitos:**
- Minimo 12 caracteres (NIST recomienda 8+, nosotros 12 para mayor seguridad)
- Mayuscula + minuscula + digito + especial
- Maximo 128 caracteres (prevenir DoS)

### 4. Auditoria

```java
// SIEMPRE auditar:
- Login exitoso/fallido
- Cambios de contrasena
- Accesos denegados
- Acciones administrativas
- Exportacion de datos

// Log estructurado para SIEM
auditLog.info("SECURITY_EVENT type={} user={} ip={} outcome={}");
```

### 5. Headers de Seguridad

```java
// XSS Protection
.xssProtection(xss -> xss.headerValue(ENABLED_MODE_BLOCK))

// Prevenir Clickjacking
.frameOptions(frame -> frame.deny())

// Content Security Policy
.contentSecurityPolicy(csp -> csp.policyDirectives(
    "default-src 'self'; " +
    "script-src 'self'; " +
    "frame-ancestors 'none'"
))

// HSTS (fuerza HTTPS)
.httpStrictTransportSecurity(hsts -> hsts
    .includeSubDomains(true)
    .maxAgeInSeconds(31536000)  // 1 ano
)
```

### 6. CORS Seguro

```yaml
app:
  cors:
    allowed-origins:
      - https://app.example.com  # Especificar dominios exactos
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
    allow-credentials: true
```

**NUNCA hacer:**
```yaml
# MAL: Permite cualquier origen
allowed-origins: ["*"]
```

### 7. Manejo de Errores

```java
// NO revelar informacion sensible en errores
// MAL:
throw new RuntimeException("Usuario 'admin' no encontrado");

// BIEN:
throw new RuntimeException("Credenciales invalidas");
```

**Previene:**
- Enumeracion de usuarios
- Informacion de estructura de BD

### 8. HTTPS Obligatorio

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

**Produccion:**
- SIEMPRE usar HTTPS
- Certificado valido (Let's Encrypt, etc.)
- Redirigir HTTP -> HTTPS

### 9. Rotacion de Secrets

```bash
# Rotar JWT_SECRET periodicamente (ej: cada 90 dias)
openssl rand -base64 64 > new_secret.txt

# Implementar periodo de transicion:
# - Aceptar tokens con ambos secrets por X dias
# - Luego eliminar secret antiguo
```

### 10. Monitoreo

```java
// Alertas automaticas para:
- Multiples logins fallidos
- Acceso desde IPs/paises inusuales
- Intentos de escalacion de privilegios
- Rate limit excedido repetidamente

// Metricas a trackear:
- Tiempo de respuesta de autenticacion
- Tasa de exito/fallo de login
- Tokens activos vs blacklisted
- Tamano de blacklist
```

---

## Checklist de Seguridad

- [ ] JWT_SECRET generado con `openssl rand -base64 64`
- [ ] JWT_SECRET almacenado en variable de entorno (NO en codigo)
- [ ] Access tokens <= 15 minutos
- [ ] Refresh tokens con rotacion habilitada
- [ ] BCrypt con factor 12+
- [ ] Password policy: minimo 12 caracteres
- [ ] Rate limiting en endpoints de auth (5 intentos / 15 min)
- [ ] HTTPS habilitado en produccion
- [ ] CORS configurado con origenes especificos
- [ ] Headers de seguridad (XSS, HSTS, CSP, Frame Options)
- [ ] Auditoria de eventos de seguridad
- [ ] Logs estructurados para SIEM
- [ ] Blacklist de tokens implementada
- [ ] Cleanup automatico de blacklist
- [ ] Manejo de errores sin revelar info sensible
- [ ] Indices en tablas de usuarios y tokens
- [ ] Tests de seguridad (ver SecurityConfigTest)
- [ ] Documentacion de Swagger con seguridad
- [ ] Monitoreo y alertas configuradas

---

## Recursos Adicionales

### Documentacion

- [JWT.io](https://jwt.io) - Debugger de JWT
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

### Herramientas

```bash
# Generar JWT secret
openssl rand -base64 64

# Testear endpoints con httpie
http POST :8080/api/v1/auth/login username=admin password=pass

# Decodificar JWT
echo "eyJ..." | base64 -d
```

### Tests

Ver archivos de test en `src/test/java/security/`:
- `JwtServiceTest.java` - Tests de generacion/validacion JWT
- `AuthServiceTest.java` - Tests de login/register/refresh
- `SecurityConfigTest.java` - Tests de configuracion
- `AuthControllerTest.java` - Tests de endpoints

---

**Anterior:** [04-INFRAESTRUCTURA-BASE.md](./04-INFRAESTRUCTURA-BASE.md)
**Siguiente:** [06-CACHE-RESILIENCIA.md](./06-CACHE-RESILIENCIA.md)
