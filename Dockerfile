# ==========================================
# APiGen - Multi-stage Dockerfile
# ==========================================
# Optimizado para Java 25 con Virtual Threads
#
# Build:
#   docker build -t apigen:latest .
#
# Run:
#   docker run -p 8080:8080 apigen:latest
# ==========================================

# ------------------------------
# Stage 1: Build
# ------------------------------
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Instalar herramientas necesarias
RUN apk add --no-cache bash

# Copiar archivos de Gradle primero (para cache de dependencias)
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Dar permisos de ejecución a gradlew
RUN chmod +x gradlew

# Descargar dependencias (cacheado si no cambian)
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente
COPY src/ src/

# Build de la aplicación (sin tests para velocidad)
RUN ./gradlew bootJar --no-daemon -x test

# Extraer layers del JAR para optimizar la imagen final
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination extracted

# ------------------------------
# Stage 2: Runtime
# ------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# Metadata
LABEL maintainer="APiGen <dev@example.com>"
LABEL description="APiGen REST API Template"
LABEL version="0.0.1-SNAPSHOT"

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S apigen && \
    adduser -u 1001 -S apigen -G apigen

WORKDIR /app

# Crear directorio de logs
RUN mkdir -p /app/logs && chown -R apigen:apigen /app

# Copiar layers extraídos (orden optimizado para cache)
COPY --from=builder --chown=apigen:apigen /app/extracted/dependencies/ ./
COPY --from=builder --chown=apigen:apigen /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=apigen:apigen /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=apigen:apigen /app/extracted/application/ ./

# Cambiar a usuario no-root
USER apigen

# Variables de entorno por defecto
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod
ENV LOG_PATH=/app/logs
ENV SERVER_PORT=8080

# Exponer puerto
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Punto de entrada optimizado para containers
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
