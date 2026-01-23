# ============================================
# APiGen Server - Docker Build
# ============================================
# Multi-stage build for the API Generator service
#
# Build:
#   docker build -f apigen-server/Dockerfile -t apigen-server:latest .
#
# Run:
#   docker run -p 8080:8080 apigen-server:latest
# ============================================

# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy ALL module source code (required by multi-module build)
COPY apigen-bom apigen-bom
COPY apigen-core apigen-core
COPY apigen-security apigen-security
COPY apigen-codegen apigen-codegen
COPY apigen-graphql apigen-graphql
COPY apigen-grpc apigen-grpc
COPY apigen-gateway apigen-gateway
COPY apigen-server apigen-server

# Make gradlew executable and build
RUN chmod +x gradlew
RUN ./gradlew :apigen-server:bootJar --no-daemon -x test

# ============================================
# Runtime stage
# ============================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the built JAR from build stage
COPY --from=build /app/apigen-server/build/libs/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Default port (can be overridden with SERVER_PORT env var)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT:-8080}/api/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
