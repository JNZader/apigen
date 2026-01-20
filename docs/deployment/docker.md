# Docker Deployment

APiGen applications can be containerized using Docker.

## Dockerfile

```dockerfile
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Build Image

```bash
# Build the application
./gradlew bootJar

# Build Docker image
docker build -t my-api:latest .
```

## Run Container

```bash
docker run -d \
  --name my-api \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://db:5432/mydb \
  my-api:latest
```

## Docker Compose

```yaml
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/mydb
    depends_on:
      - db

  db:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=mydb
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## Native Image

For GraalVM native image:

```bash
./gradlew nativeCompile
```

Then use a smaller base image:

```dockerfile
FROM alpine:3.19
COPY build/native/nativeCompile/my-api /app/my-api
ENTRYPOINT ["/app/my-api"]
```
