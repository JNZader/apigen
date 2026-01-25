# APiGen - Guía de Uso Completa

Esta guía explica **todas las formas posibles** de usar APiGen para construir APIs REST con Spring Boot 4.0 y Java 25.

## Resumen de Opciones

| # | Método | Esfuerzo | Ideal para | Guía |
|---|--------|----------|------------|------|
| 1 | [GitHub Template](#1-github-template) | ⭐ Mínimo | Proyecto nuevo, estructura completa | [USAGE_GUIDE_TEMPLATE.md](USAGE_GUIDE_TEMPLATE.md) |
| 2 | [Clonar apigen-example](#2-clonar-apigen-example) | ⭐⭐ Bajo | Proyecto nuevo, solo API | [USAGE_GUIDE_EXAMPLE.md](USAGE_GUIDE_EXAMPLE.md) |
| 3 | [Dependencia de Librería](#3-dependencia-de-librería) | ⭐⭐⭐ Medio | Proyecto existente | [USAGE_GUIDE_LIBRARY.md](USAGE_GUIDE_LIBRARY.md) |
| 4 | [Generación de Código](#4-generación-de-código) | ⭐⭐ Bajo | Database-first, SQL existente | [USAGE_GUIDE_CODEGEN.md](USAGE_GUIDE_CODEGEN.md) |
| 5 | [BOM (Bill of Materials)](#5-bom-bill-of-materials) | ⭐ Mínimo | Multi-módulo, versiones | Esta guía |

---

## Árbol de Decisión

```
¿Tienes un proyecto Spring Boot existente?
├── SÍ → ¿Tienes esquema SQL de la BD?
│        ├── SÍ → Opción 4: Generación de Código
│        └── NO → Opción 3: Dependencia de Librería
│
└── NO → ¿Quieres toda la estructura (CI/CD, Docker, etc)?
         ├── SÍ → Opción 1: GitHub Template
         └── NO → ¿Tienes esquema SQL?
                  ├── SÍ → Opción 4: Generación de Código
                  └── NO → Opción 2: Clonar apigen-example
```

---

## 1. GitHub Template

**Crear un nuevo repositorio usando APiGen como plantilla de GitHub.**

### Cuándo usar

- Quieres un proyecto nuevo con **toda la estructura** (multi-módulo, CI/CD, Docker, docs)
- Planeas contribuir o personalizar APiGen
- Necesitas los módulos de seguridad y codegen incluidos

### Cómo funciona

1. Ve a https://github.com/jnzader/apigen
2. Click en **"Use this template"** → **"Create a new repository"**
3. Nombra tu repositorio y créalo
4. Clona tu nuevo repositorio
5. Personaliza según necesites

### Qué obtienes

```
mi-nuevo-proyecto/
├── .github/                    # Workflows CI/CD, templates de issues/PR
├── apigen-core/                # Librería base (entidades, servicios, controllers)
├── apigen-security/            # Módulo JWT/OAuth2 (opcional)
├── apigen-codegen/             # Generador de código desde SQL
├── apigen-bom/                 # Bill of Materials para versiones
├── apigen-example/             # Aplicación de ejemplo funcionando
├── docker-compose.yml          # PostgreSQL, Grafana, Prometheus
├── docs/                       # Documentación completa
├── build.gradle                # Configuración multi-módulo
└── README.md
```

### Pros y Contras

| ✅ Pros | ❌ Contras |
|---------|-----------|
| Estructura completa lista | Más código del necesario si solo quieres API |
| CI/CD configurado | Debes mantener toda la estructura |
| Puedes modificar el core | Mayor complejidad inicial |
| Incluye ejemplos y docs | - |

📖 **Guía completa:** [USAGE_GUIDE_TEMPLATE.md](USAGE_GUIDE_TEMPLATE.md)

---

## 2. Clonar apigen-example

**Copiar el módulo de ejemplo como punto de partida para una nueva API.**

### Cuándo usar

- Quieres empezar rápido con una API funcionando
- No necesitas modificar el core de APiGen
- Prefieres una estructura más simple

### Cómo funciona

```bash
# Opción A: Clonar solo el ejemplo
git clone https://github.com/jnzader/apigen.git temp
cp -r temp/apigen-example mi-nuevo-proyecto
rm -rf temp
cd mi-nuevo-proyecto

# Opción B: Descargar ZIP
curl -L https://github.com/jnzader/apigen/archive/main.zip -o apigen.zip
unzip apigen.zip
mv apigen-main/apigen-example mi-nuevo-proyecto
```

### Qué obtienes

```
mi-nuevo-proyecto/
├── src/main/java/com/jnzader/example/
│   ├── domain/
│   │   ├── entity/Product.java
│   │   └── repository/ProductRepository.java
│   ├── application/
│   │   ├── dto/ProductDTO.java
│   │   ├── mapper/ProductMapper.java
│   │   └── service/ProductService.java
│   └── infrastructure/
│       ├── controller/ProductController.java
│       └── hateoas/ProductResourceAssembler.java
├── src/main/resources/
│   ├── application.yaml
│   └── db/migration/
├── build.gradle
└── README.md
```

### Pasos después de clonar

1. Renombrar paquete `com.jnzader.example` → `com.tuempresa.tuproyecto`
2. Actualizar `build.gradle` con tus datos
3. Cambiar dependencias de `project(':apigen-core')` a Maven/Gradle

### Pros y Contras

| ✅ Pros | ❌ Contras |
|---------|-----------|
| Rápido para empezar | Sin CI/CD incluido |
| Estructura simple | Debes renombrar paquetes |
| Ejemplo funcionando | Sin Docker/monitoring |
| Fácil de entender | - |

📖 **Guía completa:** [USAGE_GUIDE_EXAMPLE.md](USAGE_GUIDE_EXAMPLE.md)

---

## 3. Dependencia de Librería

**Agregar APiGen como dependencia a un proyecto Spring Boot existente.**

### Cuándo usar

- Ya tienes un proyecto Spring Boot funcionando
- Quieres agregar funcionalidad CRUD estandarizada
- Prefieres no copiar código, solo usar la librería

### Cómo funciona

```groovy
// build.gradle
dependencies {
    // Módulo core (obligatorio)
    implementation 'com.github.jnzader.apigen:apigen-core:v2.18.0'

    // Módulo de seguridad (opcional)
    implementation 'com.github.jnzader.apigen:apigen-security:v2.18.0'
}
```

### Qué obtienes

Con `apigen-core`:
- `Base` - Entidad base con auditoría y soft delete
- `BaseDTO` - Interfaz para DTOs
- `BaseMapper` - Mapper MapStruct genérico
- `BaseRepository` - Repository con métodos adicionales
- `BaseServiceImpl` - Servicio con Result pattern y eventos
- `BaseControllerImpl` - Controller CRUD completo
- `BaseResourceAssembler` - HATEOAS links automáticos
- `FilterSpecificationBuilder` - Filtrado dinámico
- Configuración automática de caché, rate limiting, etc.

Con `apigen-security`:
- Autenticación JWT con refresh tokens
- OAuth2 configurado
- Auditoría de usuario automática
- Endpoints `/auth/login`, `/auth/refresh`, `/auth/logout`

### Configuración mínima

```yaml
# application.yaml
spring:
  application:
    name: mi-api

apigen:
  core:
    enabled: true
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET:clave-secreta-minimo-32-caracteres}
```

### Pros y Contras

| ✅ Pros | ❌ Contras |
|---------|-----------|
| Integración limpia | Requiere configuración manual |
| Sin código duplicado | Curva de aprendizaje |
| Actualizaciones fáciles | Dependencia externa |
| Máxima flexibilidad | - |

📖 **Guía completa:** [USAGE_GUIDE_LIBRARY.md](USAGE_GUIDE_LIBRARY.md)

---

## 4. Generación de Código

**Generar entidades, DTOs, servicios y controllers desde un esquema SQL.**

### Cuándo usar

- Tienes una base de datos existente con esquema SQL
- Prefieres diseño database-first
- Quieres generar múltiples entidades rápidamente
- Necesitas migrar una BD existente a una API

### Cómo funciona

```bash
# Crear schema.sql con tu esquema
cat > schema.sql << 'EOF'
CREATE SEQUENCE base_sequence START WITH 1 INCREMENT BY 50;

CREATE TABLE products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER DEFAULT 0
);

CREATE TABLE categories (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(100) NOT NULL UNIQUE
);
EOF

# Generar código
java -jar apigen-codegen.jar schema.sql ./mi-proyecto com.miempresa.api
```

### Qué genera

Para cada tabla:
```
mi-proyecto/src/main/java/com/miempresa/api/
└── products/
    ├── domain/
    │   └── entity/Product.java
    ├── application/
    │   ├── dto/ProductDTO.java
    │   ├── mapper/ProductMapper.java
    │   └── service/ProductServiceImpl.java
    └── infrastructure/
        ├── repository/ProductRepository.java
        └── controller/ProductControllerImpl.java
```

### Características

- Detecta relaciones (FK → `@ManyToOne`)
- Detecta tablas de unión (→ `@ManyToMany`)
- Genera validaciones desde constraints SQL
- Crea métodos de repository para columnas únicas
- Genera migraciones Flyway
- Soporta funciones SQL → métodos de repository

### Pros y Contras

| ✅ Pros | ❌ Contras |
|---------|-----------|
| Muy rápido | Código generado puede necesitar ajustes |
| Database-first | No detecta lógica de negocio |
| Consistente | Esquemas complejos pueden fallar |
| Migraciones incluidas | - |

📖 **Guía completa:** [USAGE_GUIDE_CODEGEN.md](USAGE_GUIDE_CODEGEN.md)

---

## 5. BOM (Bill of Materials)

**Usar el BOM para gestionar versiones de APiGen en proyectos multi-módulo.**

### Cuándo usar

- Tienes múltiples módulos que usan APiGen
- Quieres asegurar versiones consistentes
- Gestionas un proyecto empresarial grande

### Cómo funciona

```groovy
// build.gradle (proyecto raíz)
subprojects {
    dependencies {
        implementation platform('com.github.jnzader.apigen:apigen-bom:v2.18.0')
    }
}

// module-a/build.gradle
dependencies {
    implementation 'com.github.jnzader.apigen:apigen-core'  // Sin versión
}

// module-b/build.gradle
dependencies {
    implementation 'com.github.jnzader.apigen:apigen-core'      // Misma versión
    implementation 'com.github.jnzader.apigen:apigen-security'  // Misma versión
}
```

### Maven

```xml
<!-- pom.xml (parent) -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.jnzader.apigen</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>v2.18.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- module-a/pom.xml -->
<dependencies>
    <dependency>
        <groupId>com.github.jnzader.apigen</groupId>
        <artifactId>apigen-core</artifactId>
        <!-- versión heredada del BOM -->
    </dependency>
</dependencies>
```

### Versiones gestionadas

El BOM gestiona versiones de:
- `apigen-core`
- `apigen-security`
- `apigen-codegen`
- Dependencias transitivas (MapStruct, Vavr, etc.)

---

## Comparación de Características

| Característica | Template | Clone Example | Library | Codegen |
|----------------|:--------:|:-------------:|:-------:|:-------:|
| Proyecto nuevo | ✅ | ✅ | ❌ | ✅ |
| Proyecto existente | ❌ | ❌ | ✅ | ✅ |
| CI/CD incluido | ✅ | ❌ | ❌ | ❌ |
| Docker incluido | ✅ | ❌ | ❌ | ❌ |
| Ejemplo funcionando | ✅ | ✅ | ❌ | ✅ |
| Modificar core | ✅ | ❌ | ❌ | ❌ |
| Database-first | ❌ | ❌ | ❌ | ✅ |
| Sin copiar código | ❌ | ❌ | ✅ | ❌ |
| Multi-módulo | ✅ | ❌ | ✅ | ❌ |
| Curva aprendizaje | Media | Baja | Media | Baja |
| Tiempo setup | 5 min | 10 min | 30 min | 5 min |

---

## Combinaciones Recomendadas

### 1. Startup / MVP Rápido
```
Opción 2 (Clone Example) + Opción 4 (Codegen si tienes SQL)
```

### 2. Proyecto Empresarial
```
Opción 1 (Template) + Opción 5 (BOM) + Opción 3 (Library en otros módulos)
```

### 3. Migración de BD Existente
```
Opción 4 (Codegen) → Opción 3 (Library para nuevas entidades)
```

### 4. Microservicios
```
Opción 5 (BOM compartido) + Opción 3 (Library en cada servicio)
```

---

## Requisitos del Sistema

| Requisito | Versión |
|-----------|---------|
| Java | 25+ |
| Spring Boot | 4.0+ |
| Gradle | 8.x |
| Maven | 3.9+ (alternativa) |
| PostgreSQL | 17+ (recomendado) |
| Docker | 24+ (opcional) |

---

## Siguiente Paso

Elige la opción que mejor se adapte a tu caso y sigue la guía específica:

1. [USAGE_GUIDE_TEMPLATE.md](USAGE_GUIDE_TEMPLATE.md) - GitHub Template
2. [USAGE_GUIDE_EXAMPLE.md](USAGE_GUIDE_EXAMPLE.md) - Clonar ejemplo
3. [USAGE_GUIDE_LIBRARY.md](USAGE_GUIDE_LIBRARY.md) - Dependencia de librería
4. [USAGE_GUIDE_CODEGEN.md](USAGE_GUIDE_CODEGEN.md) - Generación de código

---

## FAQ

### ¿Puedo combinar varias opciones?

Sí. Por ejemplo, puedes usar el **Template** para crear tu proyecto, luego usar **Codegen** para generar entidades desde SQL, y añadir más entidades manualmente usando la **Library**.

### ¿Qué opción es más fácil de mantener?

La opción **Library** (dependencia) es la más fácil de mantener porque las actualizaciones de APiGen se obtienen simplemente cambiando la versión en el `build.gradle`.

### ¿Puedo usar APiGen sin PostgreSQL?

Sí. APiGen funciona con cualquier base de datos soportada por JPA/Hibernate. PostgreSQL es recomendado pero puedes usar H2, MySQL, Oracle, etc.

### ¿APiGen funciona con Kotlin?

Sí. Todas las clases de APiGen son compatibles con Kotlin. Puedes escribir tus entidades, DTOs y servicios en Kotlin.

### ¿Cómo actualizo APiGen?

- **Library/BOM:** Cambiar versión en `build.gradle`
- **Template/Example:** Pull desde upstream o actualizar manualmente

---

## Soporte

- **Issues:** https://github.com/jnzader/apigen/issues
- **Discussions:** https://github.com/jnzader/apigen/discussions
- **Docs:** https://github.com/jnzader/apigen/tree/main/docs
