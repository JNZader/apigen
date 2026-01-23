# ============================================
# my-api - Dockerfile
# ============================================
# Runtime-only container optimized for Java 25
#
# Prerequisites:
#   ./gradlew bootJar -x test
#
# Build:
#   docker build -t my-api:latest .
#
# Run (standalone):
#   docker run -p 8080:8080 my-api:latest
#
# Run (with docker-compose):
#   docker-compose up -d
# ============================================

# Runtime Stage (JAR must be pre-built)
# Build locally first with: ./gradlew bootJar -x test
FROM eclipse-temurin:25-jre-alpine

# Metadata
LABEL maintainer="com.example"
LABEL description="my-api API Service"
LABEL version="0.0.1-SNAPSHOT"

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

# Copy pre-built JAR (build locally first with: ./gradlew bootJar -x test)
COPY --chown=appuser:appgroup build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Environment variables
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
