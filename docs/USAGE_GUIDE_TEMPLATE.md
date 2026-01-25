# APiGen - Guía de Uso: GitHub Template

Esta guía explica cómo usar APiGen como **plantilla de GitHub** para crear un nuevo repositorio con toda la estructura del proyecto.

## Índice

1. [Cuándo Usar Esta Opción](#cuándo-usar-esta-opción)
2. [Crear Repositorio desde Template](#crear-repositorio-desde-template)
3. [Estructura del Proyecto](#estructura-del-proyecto)
4. [Configuración Inicial](#configuración-inicial)
5. [Personalizar el Proyecto](#personalizar-el-proyecto)
6. [Ejecutar por Primera Vez](#ejecutar-por-primera-vez)
7. [CI/CD y GitHub Actions](#cicd-y-github-actions)
8. [Mantener Actualizado](#mantener-actualizado)

---

## Cuándo Usar Esta Opción

**Ideal para:**

- Crear un **proyecto nuevo** con infraestructura completa
- Equipos que quieren **CI/CD preconfigurado**
- Proyectos que necesitan **Docker y monitoring**
- Contribuidores que quieren **modificar el core** de APiGen
- Empresas que quieren **fork privado** para personalizar

**No usar si:**

- Ya tienes un proyecto existente → usa [Dependencia de Librería](USAGE_GUIDE_LIBRARY.md)
- Solo necesitas una API simple → usa [Clonar Ejemplo](USAGE_GUIDE_EXAMPLE.md)
- Tienes SQL y quieres generar código → usa [Codegen](USAGE_GUIDE_CODEGEN.md)

---

## Crear Repositorio desde Template

### Paso 1: Usar el Template

1. Ve a **https://github.com/jnzader/apigen**
2. Click en el botón verde **"Use this template"**
3. Selecciona **"Create a new repository"**

![Use this template button](https://docs.github.com/assets/images/help/repository/use-this-template-button.png)

### Paso 2: Configurar el Nuevo Repositorio

En la página de creación:

| Campo | Recomendación | Ejemplo |
|-------|---------------|---------|
| **Owner** | Tu usuario u organización | `mi-empresa` |
| **Repository name** | Nombre descriptivo | `mi-api-backend` |
| **Description** | Descripción del proyecto | `API REST para gestión de inventario` |
| **Visibility** | Public o Private | `Private` |
| **Include all branches** | ❌ No marcar | Solo necesitas `main` |

Click en **"Create repository"**.

### Paso 3: Clonar el Nuevo Repositorio

```bash
# Clonar tu nuevo repositorio
git clone https://github.com/tu-usuario/mi-api-backend.git
cd mi-api-backend

# Verificar la estructura
ls -la
```

---

## Estructura del Proyecto

```
mi-api-backend/
├── .github/
│   ├── workflows/
│   │   └── ci.yml                    # GitHub Actions CI/CD
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md             # Template para bugs
│   │   ├── feature_request.md        # Template para features
│   │   └── config.yml                # Configuración de templates
│   └── PULL_REQUEST_TEMPLATE.md      # Template para PRs
│
├── apigen-core/                      # 📦 Librería principal
│   ├── src/main/java/
│   │   └── com/jnzader/apigen/core/
│   │       ├── domain/
│   │       │   ├── entity/Base.java
│   │       │   ├── repository/BaseRepository.java
│   │       │   └── specification/FilterSpecificationBuilder.java
│   │       ├── application/
│   │       │   ├── dto/BaseDTO.java
│   │       │   ├── mapper/BaseMapper.java
│   │       │   └── service/BaseServiceImpl.java
│   │       └── infrastructure/
│   │           ├── controller/BaseControllerImpl.java
│   │           └── hateoas/BaseResourceAssembler.java
│   └── build.gradle
│
├── apigen-security/                  # 🔐 Módulo de seguridad
│   ├── src/main/java/
│   │   └── com/jnzader/apigen/security/
│   │       ├── domain/entity/
│   │       │   ├── User.java
│   │       │   ├── Role.java
│   │       │   └── RefreshToken.java
│   │       ├── application/service/
│   │       │   ├── AuthService.java
│   │       │   └── JwtService.java
│   │       └── infrastructure/controller/
│   │           └── AuthController.java
│   └── build.gradle
│
├── apigen-codegen/                   # 🔧 Generador de código
│   ├── src/main/java/
│   │   └── com/jnzader/apigen/codegen/
│   │       ├── SqlParser.java
│   │       └── CodeGenerator.java
│   └── build.gradle
│
├── apigen-bom/                       # 📋 Bill of Materials
│   └── build.gradle
│
├── apigen-example/                   # 📖 Aplicación de ejemplo
│   ├── src/main/java/
│   │   └── com/jnzader/example/
│   │       └── ... (estructura completa)
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   └── db/migration/
│   └── build.gradle
│
├── docs/                             # 📚 Documentación
│   ├── USAGE_GUIDE.md
│   ├── USAGE_GUIDE_LIBRARY.md
│   ├── USAGE_GUIDE_EXAMPLE.md
│   ├── USAGE_GUIDE_CODEGEN.md
│   └── USAGE_GUIDE_TEMPLATE.md
│
├── docker-compose.yml                # 🐳 PostgreSQL + monitoring
├── build.gradle                      # Configuración raíz multi-módulo
├── settings.gradle                   # Definición de módulos
├── gradle.properties                 # Propiedades globales
├── CONTRIBUTING.md                   # Guía de contribución
├── LICENSE                           # MIT License
└── README.md                         # Documentación principal
```

---

## Configuración Inicial

### Paso 1: Actualizar Información del Proyecto

**`build.gradle` (raíz):**

```groovy
allprojects {
    group = 'com.tu-empresa'  // Cambiar
    version = '0.1.0-SNAPSHOT'
}
```

**`settings.gradle`:**

```groovy
rootProject.name = 'mi-api-backend'  // Cambiar nombre del proyecto
```

**`gradle.properties`:**

```properties
org.gradle.jvmargs=-Xmx2g
systemProp.file.encoding=UTF-8
```

### Paso 2: Configurar Variables de Entorno

Crea un archivo `.env` (no commitear):

```bash
# .env
DB_URL=jdbc:postgresql://localhost:5432/mi_db
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=mi-clave-secreta-super-segura-de-al-menos-32-caracteres
```

### Paso 3: Configurar GitHub Secrets

Para CI/CD, ve a tu repositorio → **Settings** → **Secrets and variables** → **Actions**:

| Secret | Descripción | Ejemplo |
|--------|-------------|---------|
| `DOCKER_USERNAME` | Usuario Docker Hub | `miusuario` |
| `DOCKER_PASSWORD` | Token Docker Hub | `dckr_pat_xxx` |
| `SONAR_TOKEN` | Token SonarCloud (opcional) | `sqp_xxx` |

---

## Personalizar el Proyecto

### Opción A: Usar Todo el Proyecto Multi-Módulo

Si quieres mantener la estructura multi-módulo y potencialmente contribuir cambios al core:

1. **Renombrar paquetes** en todos los módulos:
   - `com.jnzader.apigen` → `com.tu-empresa.api`
   - `com.jnzader.example` → `com.tu-empresa.myapp`

2. **Actualizar imports** en todos los archivos Java

3. **Actualizar `@ComponentScan`** en las clases de configuración

### Opción B: Solo Usar apigen-example

Si solo necesitas la aplicación de ejemplo:

```bash
# Copiar solo el ejemplo a la raíz
mv apigen-example/* .
rm -rf apigen-core apigen-security apigen-codegen apigen-bom

# Actualizar build.gradle para usar dependencias de Maven
# Cambiar:
#   implementation project(':apigen-core')
# A:
#   implementation 'com.github.jnzader.apigen:apigen-core:v2.18.0'
```

### Opción C: Mantener como Fork para Contribuir

Si planeas contribuir al proyecto original:

```bash
# Agregar el repositorio original como remote
git remote add upstream https://github.com/jnzader/apigen.git

# Verificar remotes
git remote -v
# origin    https://github.com/tu-usuario/mi-api-backend.git (fetch)
# origin    https://github.com/tu-usuario/mi-api-backend.git (push)
# upstream  https://github.com/jnzader/apigen.git (fetch)
# upstream  https://github.com/jnzader/apigen.git (push)
```

---

## Ejecutar por Primera Vez

### Opción 1: Con Docker Compose (Recomendado)

```bash
# Levantar PostgreSQL
docker-compose up -d postgres

# Verificar que está corriendo
docker-compose ps

# Ejecutar la aplicación de ejemplo
./gradlew :apigen-example:bootRun
```

### Opción 2: Con H2 (Sin Docker)

```bash
# Ejecutar con perfil de desarrollo
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=dev'
```

### Verificar que Funciona

```bash
# Health check
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Swagger UI
open http://localhost:8080/swagger-ui.html

# Listar productos de ejemplo
curl http://localhost:8080/api/products
```

---

## CI/CD y GitHub Actions

El proyecto incluye un workflow de CI/CD preconfigurado.

### Workflow: `.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Build with Gradle
        run: ./gradlew build

      - name: Run tests
        run: ./gradlew test

      - name: Upload test results
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/reports/tests/'
```

### Qué hace el CI

| Evento | Acción |
|--------|--------|
| Push a `main` | Build + Tests + (Deploy) |
| Push a `develop` | Build + Tests |
| Pull Request | Build + Tests + Code Review |

### Personalizar CI/CD

Para agregar deployment:

```yaml
# En .github/workflows/ci.yml, agregar:
  deploy:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to production
        run: |
          # Tu lógica de deployment
```

---

## Mantener Actualizado

### Sincronizar con el Repositorio Original

Si quieres recibir actualizaciones de APiGen:

```bash
# Obtener cambios del upstream
git fetch upstream

# Crear rama para merge
git checkout -b sync-upstream main

# Merge cambios
git merge upstream/main

# Resolver conflictos si los hay
# ... editar archivos ...
git add .
git commit -m "Sync with upstream APiGen"

# Push a tu repositorio
git push origin sync-upstream

# Crear PR para revisar los cambios
```

### Actualizar Dependencias

```bash
# Ver dependencias desactualizadas
./gradlew dependencyUpdates

# Actualizar Gradle wrapper
./gradlew wrapper --gradle-version=8.12
```

---

## Estructura de Ramas Recomendada

```
main                    # Producción
├── develop             # Desarrollo activo
│   ├── feature/xxx     # Nuevas características
│   └── bugfix/xxx      # Correcciones
├── release/x.x.x       # Preparación de releases
└── hotfix/xxx          # Correcciones urgentes
```

### Flujo de Trabajo

1. **Nueva feature:** `git checkout -b feature/nueva-entidad develop`
2. **Desarrollo:** commits en la rama
3. **PR:** Pull Request a `develop`
4. **Review:** Code review + CI
5. **Merge:** Merge a `develop`
6. **Release:** `develop` → `release/1.0.0` → `main`

---

## Checklist de Setup

- [ ] Crear repositorio desde template
- [ ] Clonar repositorio localmente
- [ ] Actualizar `group` en `build.gradle`
- [ ] Actualizar `rootProject.name` en `settings.gradle`
- [ ] Crear archivo `.env` con variables
- [ ] Configurar GitHub Secrets
- [ ] Verificar CI pasa correctamente
- [ ] Levantar con Docker Compose
- [ ] Acceder a Swagger UI
- [ ] Renombrar paquetes (si es necesario)
- [ ] Eliminar módulos que no necesitas
- [ ] Actualizar README.md del proyecto

---

## Próximos Pasos

Una vez que tengas el proyecto configurado:

1. **Crear tus entidades** - Sigue la estructura de `Product` en `apigen-example`
2. **Agregar lógica de negocio** - Extiende los servicios base
3. **Configurar seguridad** - Personaliza roles y permisos
4. **Agregar tests** - Usa los tests existentes como referencia
5. **Configurar deployment** - Agrega pasos de deploy al CI

---

## Solución de Problemas

### Error: "Could not find project :apigen-core"

Asegúrate de que `settings.gradle` incluye todos los módulos:

```groovy
include 'apigen-core', 'apigen-security', 'apigen-codegen', 'apigen-bom', 'apigen-example'
```

### Error: "Port 8080 already in use"

```bash
# Encontrar el proceso
lsof -i :8080

# Matar el proceso
kill -9 <PID>

# O cambiar el puerto
./gradlew :apigen-example:bootRun --args='--server.port=8081'
```

### Error: "Cannot connect to database"

```bash
# Verificar que PostgreSQL está corriendo
docker-compose ps

# Ver logs
docker-compose logs postgres

# Reiniciar
docker-compose restart postgres
```

---

## Ver También

- [USAGE_GUIDE.md](USAGE_GUIDE.md) - Resumen de todas las opciones
- [USAGE_GUIDE_LIBRARY.md](USAGE_GUIDE_LIBRARY.md) - Usar como dependencia
- [USAGE_GUIDE_EXAMPLE.md](USAGE_GUIDE_EXAMPLE.md) - Usar solo el ejemplo
- [USAGE_GUIDE_CODEGEN.md](USAGE_GUIDE_CODEGEN.md) - Generar desde SQL
- [CONTRIBUTING.md](../CONTRIBUTING.md) - Guía de contribución
