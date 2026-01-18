# 01 - Setup Inicial del Proyecto

## Objetivo

En este documento aprenderas a configurar y ejecutar APiGen, entendiendo la estructura multi-modulo y como usar cada componente.

## Prerequisitos

```bash
# Java 25 (o Java 21+)
java -version
# openjdk version "25" o superior

# Docker y Docker Compose
docker --version
docker-compose --version

# Git
git --version
```

## Opcion 1: Clonar y Explorar (Recomendado para Aprender)

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/jnzader/apigen.git
cd apigen
```

### Paso 2: Entender la Estructura

```
apigen/
├── apigen-core/        # Libreria base - EXPLORAR PRIMERO
├── apigen-security/    # Modulo de seguridad
├── apigen-codegen/     # Generador de codigo
├── apigen-bom/         # Bill of Materials
├── apigen-example/     # Aplicacion de ejemplo - EJECUTAR ESTO
├── docs/               # Documentacion de uso
├── docs-didacticos/    # Esta documentacion
├── build.gradle        # Config multi-modulo
├── settings.gradle     # Define los modulos
└── docker-compose.yml  # PostgreSQL + servicios
```

### Paso 3: Levantar PostgreSQL

```bash
docker-compose up -d postgres

# Verificar que esta corriendo
docker-compose ps
# NAME              STATUS
# apigen-postgres   Up (healthy)
```

### Paso 4: Ejecutar el Ejemplo

```bash
# Ejecutar apigen-example
./gradlew :apigen-example:bootRun

# O con perfil dev (usa H2 en memoria, no requiere Docker)
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=dev'
```

### Paso 5: Verificar que Funciona

```bash
# Health check
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Swagger UI
# Abrir en navegador: http://localhost:8080/swagger-ui.html

# Listar productos
curl http://localhost:8080/api/products

# Crear un producto
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":999.99,"stock":10}'
```

## Opcion 2: Usar como Dependencia (Para Proyectos Nuevos)

### Paso 1: Crear Proyecto con Spring Initializr

1. Visita https://start.spring.io
2. Configura:
   - **Project:** Gradle - Groovy
   - **Language:** Java
   - **Spring Boot:** 4.0.0
   - **Java:** 25
   - **Dependencies:**
     - Spring Web
     - Spring Data JPA
     - PostgreSQL Driver
     - Validation

3. Click "Generate" y descomprime

### Paso 2: Agregar Dependencias de APiGen

```groovy
// build.gradle

plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.tuempresa'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    mavenLocal()  // Para snapshots locales
}

dependencies {
    // APiGen - BOM para gestion de versiones
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')

    // APiGen - Core (obligatorio)
    implementation 'com.jnzader:apigen-core'

    // APiGen - Security (opcional)
    implementation 'com.jnzader:apigen-security'

    // Base de datos
    runtimeOnly 'org.postgresql:postgresql'

    // Lombok y MapStruct
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += [
        '-Amapstruct.defaultComponentModel=spring',
        '-Amapstruct.unmappedTargetPolicy=IGNORE'
    ]
}
```

### Paso 3: Configurar Application

```java
// src/main/java/com/tuempresa/myapi/MyApiApplication.java
package com.tuempresa.myapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
    "com.tuempresa.myapi",
    "com.jnzader.apigen.core.domain.entity"
})
public class MyApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApiApplication.class, args);
    }
}
```

### Paso 4: Configurar application.yaml

```yaml
# src/main/resources/application.yaml
spring:
  application:
    name: my-api

  threads:
    virtual:
      enabled: true

  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

# APiGen configuration
apigen:
  core:
    enabled: true
  security:
    enabled: false  # Cambiar a true cuando necesites seguridad
    jwt:
      secret: ${JWT_SECRET:clave-secreta-de-al-menos-32-caracteres}

# Swagger
springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

### Paso 5: Crear tu Primera Entidad

Sigue el patron de `apigen-example`. Necesitas crear 7 archivos:

```
src/main/java/com/tuempresa/myapi/
├── domain/
│   ├── entity/Product.java
│   └── repository/ProductRepository.java
├── application/
│   ├── dto/ProductDTO.java
│   ├── mapper/ProductMapper.java
│   └── service/ProductService.java
└── infrastructure/
    ├── controller/ProductController.java
    └── hateoas/ProductResourceAssembler.java
```

Ver [02-DOMINIO-BASE.md](./02-DOMINIO-BASE.md) para detalles de cada archivo.

## Estructura Multi-Modulo Explicada

### settings.gradle

```groovy
rootProject.name = 'apigen'

include 'apigen-core'
include 'apigen-security'
include 'apigen-codegen'
include 'apigen-bom'
include 'apigen-example'
```

### build.gradle (Raiz)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0' apply false
    id 'io.spring.dependency-management' version '1.1.7'
}

allprojects {
    group = 'com.jnzader'
    version = '1.0.0-SNAPSHOT'
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    repositories {
        mavenCentral()
    }
}
```

### Dependencias entre Modulos

```
apigen-example
    └── depends on → apigen-core
    └── depends on → apigen-security

apigen-security
    └── depends on → apigen-core

apigen-codegen
    └── standalone (no dependencias internas)

apigen-bom
    └── define versiones de todos los modulos
```

### build.gradle de apigen-core

```groovy
plugins {
    id 'java-library'  // Nota: java-library, no java
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

// No crear JAR ejecutable
bootJar {
    enabled = false
}

// Crear JAR de libreria
jar {
    enabled = true
}

dependencies {
    api 'org.springframework.boot:spring-boot-starter-web'
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    api 'org.springframework.boot:spring-boot-starter-validation'
    api 'org.springframework.boot:spring-boot-starter-hateoas'
    api 'org.springframework.boot:spring-boot-starter-cache'

    api 'org.mapstruct:mapstruct:1.6.3'
    api 'io.vavr:vavr:0.10.5'
    api 'com.github.ben-manes.caffeine:caffeine:3.2.0'

    // ... mas dependencias
}
```

### build.gradle de apigen-example

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

// Este SI crea JAR ejecutable
bootJar {
    enabled = true
}

dependencies {
    implementation project(':apigen-core')
    implementation project(':apigen-security')

    runtimeOnly 'org.postgresql:postgresql'

    // Para desarrollo
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

## Comandos Gradle Utiles

```bash
# Compilar todo
./gradlew build

# Compilar solo un modulo
./gradlew :apigen-core:build

# Ejecutar tests
./gradlew test

# Ejecutar tests de un modulo
./gradlew :apigen-core:test

# Ejecutar la aplicacion de ejemplo
./gradlew :apigen-example:bootRun

# Limpiar y reconstruir
./gradlew clean build

# Ver dependencias de un modulo
./gradlew :apigen-core:dependencies

# Publicar a Maven Local (para usar como dependencia)
./gradlew publishToMavenLocal
```

## Docker Compose

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:17-alpine
    container_name: apigen-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: apigen_db
      POSTGRES_USER: apigen_user
      POSTGRES_PASSWORD: apigen_password
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U apigen_user -d apigen_db"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres-data:
```

### Comandos Docker

```bash
# Levantar servicios
docker-compose up -d

# Ver logs
docker-compose logs -f postgres

# Detener
docker-compose down

# Detener y eliminar volumenes
docker-compose down -v
```

## Perfiles de Configuracion

### application.yaml (Base)

```yaml
spring:
  application:
    name: apigen-example

  threads:
    virtual:
      enabled: true

apigen:
  core:
    enabled: true
```

### application-dev.yaml (Desarrollo)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

apigen:
  security:
    enabled: false

logging:
  level:
    com.jnzader: DEBUG
```

### application-prod.yaml (Produccion)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

apigen:
  security:
    enabled: true
    jwt:
      secret: ${JWT_SECRET}

logging:
  level:
    root: WARN
    com.jnzader: INFO
```

### Ejecutar con Perfil

```bash
# Desarrollo (H2)
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=dev'

# Produccion (PostgreSQL)
./gradlew :apigen-example:bootRun --args='--spring.profiles.active=prod'
```

## Verificacion Final

### Checklist

- [ ] Java 25 instalado
- [ ] Docker corriendo
- [ ] Repositorio clonado
- [ ] PostgreSQL levantado con Docker Compose
- [ ] `./gradlew :apigen-example:bootRun` funciona
- [ ] http://localhost:8080/actuator/health retorna UP
- [ ] http://localhost:8080/swagger-ui.html muestra la documentacion
- [ ] Puedes crear/listar productos via API

### Troubleshooting

#### Error: "Port 8080 already in use"

```bash
# Encontrar proceso
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# Matar proceso o usar otro puerto
./gradlew :apigen-example:bootRun --args='--server.port=8081'
```

#### Error: "Cannot connect to database"

```bash
# Verificar Docker
docker-compose ps
docker-compose logs postgres

# Reiniciar
docker-compose restart postgres
```

#### Error: "Could not find apigen-core"

```bash
# Publicar a Maven Local primero
./gradlew :apigen-core:publishToMavenLocal
./gradlew :apigen-security:publishToMavenLocal
```

## Resumen

Has aprendido a:
1. Clonar y ejecutar el proyecto multi-modulo
2. Entender la estructura de directorios
3. Usar Docker Compose para PostgreSQL
4. Ejecutar con diferentes perfiles
5. Usar APiGen como dependencia

## Proximos Pasos

Continua con [02-DOMINIO-BASE.md](./02-DOMINIO-BASE.md) para entender:
- La clase `Base` entity
- El `BaseRepository`
- El sistema de soft delete
- Los domain events

---

**Siguiente:** [02-DOMINIO-BASE.md](./02-DOMINIO-BASE.md)
