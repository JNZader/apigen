# Guía de Despliegue: APiGen Studio

## Arquitectura de Despliegue

```
┌─────────────────────┐         ┌─────────────────────┐
│   apigen-web        │  HTTPS  │   apigen-server     │
│   (Vercel)          │ ──────► │   (Koyeb)           │
│   React + Vite      │         │   Spring Boot       │
└─────────────────────┘         └─────────────────────┘
     Frontend                        Backend
     https://apigen.vercel.app       https://apigen-server.koyeb.app
```

---

## Parte 1: Backend en Koyeb (apigen-server)

### 1.1 Preparar el Proyecto

Primero, necesitamos crear un Dockerfile para el servidor.

```bash
cd apigen
```

Crear `apigen-server/Dockerfile`:

```dockerfile
# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY apigen-codegen apigen-codegen
COPY apigen-server apigen-server

# Build the application
RUN chmod +x gradlew
RUN ./gradlew :apigen-server:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/apigen-server/build/libs/apigen-server-*.jar app.jar

# Expose port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 1.2 Configurar CORS para Producción

Actualizar `apigen-server/src/main/java/.../config/CorsConfig.java`:

```java
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // En producción, usar orígenes específicos
        if ("*".equals(allowedOrigins)) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Content-Disposition", "Content-Length"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
```

### 1.3 Crear application-prod.yml

```yaml
# apigen-server/src/main/resources/application-prod.yml
server:
  port: ${PORT:8081}

spring:
  application:
    name: apigen-server

cors:
  allowed-origins: ${CORS_ORIGINS:https://apigen-studio.vercel.app}

logging:
  level:
    com.jnzader.apigen: INFO
    org.springframework.web: WARN
```

### 1.4 Desplegar en Koyeb

#### Opción A: Desde GitHub (Recomendado)

1. **Subir código a GitHub**:
```bash
git add .
git commit -m "Add deployment configuration"
git push origin main
```

2. **En Koyeb Dashboard** (https://app.koyeb.com):
   - Click "Create App"
   - Seleccionar "GitHub"
   - Conectar tu repositorio `apigen`
   - Configurar:
     - **Name**: `apigen-server`
     - **Builder**: Docker
     - **Dockerfile path**: `apigen-server/Dockerfile`
     - **Port**: `8081`

3. **Variables de Entorno**:
   ```
   SPRING_PROFILES_ACTIVE=prod
   CORS_ORIGINS=https://tu-app.vercel.app
   JAVA_OPTS=-Xmx512m
   ```

4. **Recursos**:
   - Instance type: `nano` (para empezar) o `small`
   - Regions: Elegir la más cercana

5. Click "Deploy"

#### Opción B: Desde Docker Hub

1. **Build y push local**:
```bash
cd apigen

# Build image
docker build -f apigen-server/Dockerfile -t tuusuario/apigen-server:latest .

# Push to Docker Hub
docker login
docker push tuusuario/apigen-server:latest
```

2. **En Koyeb**:
   - "Create App" → "Docker"
   - Image: `tuusuario/apigen-server:latest`
   - Configurar variables de entorno

### 1.5 Verificar Despliegue

Una vez desplegado, Koyeb te dará una URL como:
```
https://apigen-server-tuusuario.koyeb.app
```

Verificar:
```bash
curl https://apigen-server-tuusuario.koyeb.app/api/health
# Respuesta: {"status":"ok","message":"APiGen Server is running"}
```

---

## Parte 2: Frontend en Vercel (apigen-web)

### 2.1 Configurar Variable de Entorno

Crear `.env.production` en `apigen-web/`:

```env
VITE_API_URL=https://apigen-server-tuusuario.koyeb.app
```

### 2.2 Verificar vite.config.ts

```typescript
// apigen-web/vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
```

### 2.3 Crear vercel.json (Opcional)

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist",
  "framework": "vite",
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

### 2.4 Desplegar en Vercel

#### Opción A: Desde CLI (Rápido)

```bash
cd apigen-web

# Instalar Vercel CLI
npm i -g vercel

# Login
vercel login

# Deploy
vercel

# Seguir las instrucciones:
# - Set up and deploy? Yes
# - Which scope? Tu cuenta
# - Link to existing project? No
# - Project name? apigen-studio
# - Directory? ./
# - Override settings? No
```

Para producción:
```bash
vercel --prod
```

#### Opción B: Desde GitHub (Automático)

1. **Conectar repositorio**:
   - Ve a https://vercel.com/new
   - "Import Git Repository"
   - Selecciona `apigen-web` (o el monorepo)

2. **Configurar proyecto**:
   - **Framework Preset**: Vite
   - **Root Directory**: `apigen-web` (si es monorepo)
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`

3. **Variables de Entorno**:
   - Click "Environment Variables"
   - Agregar:
     ```
     VITE_API_URL = https://apigen-server-tuusuario.koyeb.app
     ```

4. Click "Deploy"

### 2.5 Configurar Dominio Personalizado (Opcional)

En Vercel Dashboard:
1. Settings → Domains
2. Add: `apigen.tudominio.com`
3. Configurar DNS en tu proveedor

---

## Parte 3: Configuración Final

### 3.1 Actualizar CORS en Koyeb

Una vez tengas la URL de Vercel, actualiza las variables en Koyeb:

```
CORS_ORIGINS=https://apigen-studio.vercel.app,https://apigen.tudominio.com
```

### 3.2 Verificar Conexión End-to-End

1. Abrir tu app en Vercel
2. Crear una entidad de prueba
3. Click en "Download" → "With APiGen Server"
4. Debería descargarse un ZIP

### 3.3 Monitoreo

**Koyeb**:
- Dashboard → Logs (ver errores en tiempo real)
- Metrics (CPU, memoria, requests)

**Vercel**:
- Analytics (si está habilitado)
- Functions logs (si usas API routes)

---

## Troubleshooting

### Error: CORS blocked

**Síntoma**: El frontend no puede conectar con el backend

**Solución**:
1. Verificar `CORS_ORIGINS` en Koyeb incluye la URL exacta de Vercel
2. No incluir trailing slash: `https://app.vercel.app` ✓, `https://app.vercel.app/` ✗

### Error: Connection refused

**Síntoma**: `Failed to fetch` en el frontend

**Solución**:
1. Verificar que el servidor Koyeb está running
2. Verificar `VITE_API_URL` en Vercel es correcto
3. Probar endpoint directamente: `curl https://tu-server.koyeb.app/api/health`

### Error: Build failed en Koyeb

**Síntoma**: Docker build falla

**Solución**:
1. Probar build local primero:
   ```bash
   docker build -f apigen-server/Dockerfile -t test .
   ```
2. Verificar que todos los archivos están commiteados
3. Revisar logs de build en Koyeb

### Error: Out of memory

**Síntoma**: App se reinicia constantemente

**Solución**:
1. Aumentar memoria en Koyeb (usar `small` en vez de `nano`)
2. Agregar `JAVA_OPTS=-Xmx256m` para limitar heap

---

## Costos Estimados

### Koyeb
- **Free tier**: 1 nano instance (256MB RAM)
- **Starter**: ~$5/mes por small instance (512MB RAM)

### Vercel
- **Hobby (Free)**: Suficiente para proyectos personales
- **Pro**: $20/mes (más bandwidth, analytics)

---

## Scripts de Despliegue

### deploy-backend.sh
```bash
#!/bin/bash
cd apigen
git add .
git commit -m "Deploy: $(date +%Y-%m-%d)"
git push origin main
echo "Koyeb detectará el push y desplegará automáticamente"
```

### deploy-frontend.sh
```bash
#!/bin/bash
cd apigen-web
vercel --prod
```

---

## Resumen de URLs

| Servicio | URL Local | URL Producción |
|----------|-----------|----------------|
| Frontend | http://localhost:5173 | https://apigen-studio.vercel.app |
| Backend | http://localhost:8081 | https://apigen-server.koyeb.app |
| Health | /api/health | /api/health |
| Generate | POST /api/generate | POST /api/generate |
