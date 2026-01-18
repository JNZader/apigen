# Documentacion Didactica de APiGen

Bienvenido a la documentacion didactica de APiGen. Esta guia te ayudara a entender el proyecto y recrearlo desde cero, comprendiendo cada decision arquitectonica y tecnica.

## Estado Actual del Proyecto

APiGen ha evolucionado de una **aplicacion monolitica** a una **libreria multi-modulo reutilizable**:

```
apigen/
├── apigen-core/        # Libreria base (entidades, servicios, controllers)
├── apigen-security/    # Modulo de seguridad JWT/OAuth2
├── apigen-codegen/     # Generador de codigo desde SQL
├── apigen-bom/         # Bill of Materials para versiones
└── apigen-example/     # Aplicacion de ejemplo
```

## Formas de Usar APiGen

| Metodo | Descripcion | Documentacion |
|--------|-------------|---------------|
| **GitHub Template** | Crear nuevo repo desde template | [USAGE_GUIDE_TEMPLATE.md](../docs/USAGE_GUIDE_TEMPLATE.md) |
| **Clonar Ejemplo** | Copiar apigen-example | [USAGE_GUIDE_EXAMPLE.md](../docs/USAGE_GUIDE_EXAMPLE.md) |
| **Libreria** | Agregar como dependencia | [USAGE_GUIDE_LIBRARY.md](../docs/USAGE_GUIDE_LIBRARY.md) |
| **Codegen** | Generar desde SQL | [USAGE_GUIDE_CODEGEN.md](../docs/USAGE_GUIDE_CODEGEN.md) |

## Filosofia de Aprendizaje Progresivo

Esta documentacion sigue el principio de **"aprender cuando lo necesitas, no antes"**. Cada documento:

1. **Explica el "por que" antes del "como"**
2. **Construye sobre conceptos anteriores**
3. **Incluye ejemplos practicos completos**
4. **Tiene ejercicios para reforzar el aprendizaje**

## Roadmap de Documentos

### Documentos Disponibles

- **[00-OVERVIEW.md](./00-OVERVIEW.md)** - Vision general del proyecto
  - Que es APiGen y por que existe
  - Arquitectura multi-modulo
  - Stack tecnologico (Spring Boot 4.0, Java 25)
  - Casos de uso ideales

- **[01-SETUP-INICIAL.md](./01-SETUP-INICIAL.md)** - Configurar el proyecto
  - Clonar y ejecutar apigen-example
  - Estructura multi-modulo explicada
  - Docker Compose
  - Perfiles de configuracion (dev/prod)

- **[02-DOMINIO-BASE.md](./02-DOMINIO-BASE.md)** - Capa de dominio
  - Entidad Base con auditoria
  - BaseRepository con queries optimizadas
  - Sistema de soft delete
  - Domain events para pub/sub

- **[03-APLICACION-BASE.md](./03-APLICACION-BASE.md)** - Capa de aplicacion
  - BaseDTO y validaciones
  - BaseMapper con MapStruct
  - BaseServiceImpl generico
  - Result Pattern (Vavr)

- **[04-INFRAESTRUCTURA-BASE.md](./04-INFRAESTRUCTURA-BASE.md)** - Capa de infraestructura
  - BaseControllerImpl con HATEOAS
  - GlobalExceptionHandler (RFC 7807)
  - Filtrado dinamico con JPA Specifications
  - ETag y Cache condicional

- **[05-SEGURIDAD-JWT.md](./05-SEGURIDAD-JWT.md)** - Sistema de seguridad
  - Modulo apigen-security
  - JWT Service con refresh tokens
  - AuthController
  - Auto-configuracion de seguridad

- **[06-CACHE-RESILIENCIA.md](./06-CACHE-RESILIENCIA.md)** - Cache y resiliencia
  - Caffeine multi-nivel
  - Rate limiting
  - Configuracion via application.yaml

- **[07-OBSERVABILIDAD.md](./07-OBSERVABILIDAD.md)** - Monitoreo
  - Logging estructurado
  - Metricas Prometheus
  - Actuator endpoints

- **[08-TESTING.md](./08-TESTING.md)** - Testing completo
  - Tests unitarios con Mockito
  - Tests de integracion
  - Tests de arquitectura con ArchUnit

- **[09-GENERADORES.md](./09-GENERADORES.md)** - Generadores de codigo
  - Modulo apigen-codegen
  - Generar desde SQL
  - Estructura de archivos generados

- **[10-CHEATSHEET.md](./10-CHEATSHEET.md)** - Referencia rapida
  - Comandos Gradle
  - Docker commands
  - URLs importantes
  - Patrones de codigo

- **[11-NUEVAS-FUNCIONALIDADES.md](./11-NUEVAS-FUNCIONALIDADES.md)** - Funcionalidades avanzadas
  - Cursor-based Pagination
  - Auto-configuracion Spring Boot Starter

### Documentos de Referencia

- **[ARQUITECTURA-DDD.md](./ARQUITECTURA-DDD.md)** - Arquitectura DDD explicada
- **[PATRONES-DISENO.md](./PATRONES-DISENO.md)** - Patrones de diseno usados
- **[RECURSOS-EXTERNOS.md](./RECURSOS-EXTERNOS.md)** - Links a recursos externos

## Como Usar Esta Documentacion

### Opcion 1: Aprendizaje Completo (Recomendado)

Lee los documentos en orden del 00 al 11, implementando cada concepto.

```bash
# Clona el repositorio
git clone https://github.com/jnzader/apigen.git
cd apigen

# Explora apigen-core para entender las clases base
ls apigen-core/src/main/java/com/jnzader/apigen/core/

# Ejecuta el ejemplo para ver todo funcionando
docker-compose up -d postgres
./gradlew :apigen-example:bootRun
```

### Opcion 2: Referencia Rapida

Si ya conoces Spring Boot y solo quieres entender decisiones especificas:

1. Lee el `00-OVERVIEW.md` para contexto
2. Salta a los documentos que te interesen
3. Usa el `10-CHEATSHEET.md` para referencia rapida

### Opcion 3: Usar Directamente

Si solo quieres usar APiGen sin entender los internos:

1. Ve a [docs/USAGE_GUIDE.md](../docs/USAGE_GUIDE.md)
2. Elige el metodo que prefieras
3. Sigue las instrucciones

## Estructura del Proyecto Multi-Modulo

```
apigen/
├── apigen-core/                      # LIBRERIA PRINCIPAL
│   └── src/main/java/com/jnzader/apigen/core/
│       ├── domain/
│       │   ├── entity/Base.java      # Entidad base
│       │   ├── repository/BaseRepository.java
│       │   └── specification/FilterSpecificationBuilder.java
│       ├── application/
│       │   ├── dto/BaseDTO.java
│       │   ├── mapper/BaseMapper.java
│       │   └── service/BaseServiceImpl.java
│       └── infrastructure/
│           ├── controller/BaseControllerImpl.java
│           └── hateoas/BaseResourceAssembler.java
│
├── apigen-security/                  # MODULO DE SEGURIDAD
│   └── src/main/java/com/jnzader/apigen/security/
│       ├── domain/entity/{User,Role,RefreshToken}.java
│       ├── application/service/{AuthService,JwtService}.java
│       └── infrastructure/controller/AuthController.java
│
├── apigen-codegen/                   # GENERADOR DE CODIGO
│   └── src/main/java/com/jnzader/apigen/codegen/
│       ├── SqlParser.java
│       └── CodeGenerator.java
│
├── apigen-bom/                       # BILL OF MATERIALS
│   └── build.gradle                  # Define versiones de todos los modulos
│
├── apigen-example/                   # APLICACION DE EJEMPLO
│   └── src/main/java/com/jnzader/example/
│       ├── domain/entity/Product.java
│       ├── application/{dto,mapper,service}/
│       └── infrastructure/controller/ProductController.java
│
├── docs/                             # Documentacion de uso
│   ├── USAGE_GUIDE.md
│   ├── USAGE_GUIDE_*.md
│   └── FEATURES.md
│
├── docs-didacticos/                  # ESTA DOCUMENTACION
│
├── docker-compose.yml
├── build.gradle                      # Config multi-modulo
└── settings.gradle                   # Define modulos
```

## Preguntas Frecuentes

### Como uso APiGen en mi proyecto?

```groovy
// build.gradle
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // opcional
}
```

Ver [USAGE_GUIDE_LIBRARY.md](../docs/USAGE_GUIDE_LIBRARY.md) para detalles.

### Como genero una entidad nueva?

En apigen-example, crea los 7 archivos manualmente siguiendo el patron de Product:
1. Entity (extiende Base)
2. DTO (record implementando BaseDTO)
3. Mapper (extiende BaseMapper)
4. Repository (extiende BaseRepository)
5. Service (extiende BaseServiceImpl)
6. ResourceAssembler (extiende BaseResourceAssembler)
7. Controller (extiende BaseControllerImpl)

O usa codegen desde SQL:
```bash
java -jar apigen-codegen.jar schema.sql ./output com.mycompany
```

### Como activo/desactivo la seguridad?

```yaml
# application.yaml
apigen:
  security:
    enabled: false  # true para activar
```

### Cual es la diferencia entre docs/ y docs-didacticos/?

- **docs/** - Documentacion de USO (como usar APiGen)
- **docs-didacticos/** - Documentacion de APRENDIZAJE (como funciona internamente)

## Stack Tecnologico

| Categoria | Tecnologia | Version |
|-----------|------------|---------|
| **Runtime** | Java | 25 |
| **Framework** | Spring Boot | 4.0.0 |
| **Database** | PostgreSQL | 17 |
| **ORM** | Hibernate | 7.x |
| **Mapping** | MapStruct | 1.6.3 |
| **Security** | JWT (jjwt) | 0.13.0 |
| **Cache** | Caffeine | 3.2.x |
| **Resilience** | Resilience4j | 2.3.x |
| **Docs** | SpringDoc OpenAPI | 3.0.0 |
| **Testing** | JUnit 5 + TestContainers | - |

## Contribuciones

Si encuentras errores o quieres mejorar la documentacion:

1. Abre un issue en GitHub
2. Propone cambios via Pull Request
3. Discute en GitHub Discussions

## Licencia

Esta documentacion esta bajo la misma licencia MIT que el proyecto APiGen.

---

**Proximo Paso:** Lee [00-OVERVIEW.md](./00-OVERVIEW.md) para entender la vision general del proyecto.

**Ultima actualizacion:** Enero 2025
**Version:** 3.0.0 (Multi-modulo)
