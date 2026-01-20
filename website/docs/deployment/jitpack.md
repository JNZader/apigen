---
sidebar_position: 3
---

# Publishing to JitPack

JitPack is a package repository for JVM and Android projects that builds directly from GitHub repositories. It's the easiest way to publish your library without setting up a Maven repository.

## How JitPack Works

1. User requests your library (e.g., `com.github.jnzader:apigen-core:1.0.0`)
2. JitPack clones your repository
3. JitPack builds the project using Gradle
4. JitPack serves the built artifacts

## APiGen on JitPack

APiGen is available on JitPack at:

**https://jitpack.io/#jnzader/apigen**

### Available Modules (Library Artifacts)

These are the **library modules** published to JitPack for consumption:

| Module | JitPack Artifact | Description |
|--------|------------------|-------------|
| BOM | `com.github.jnzader.apigen:apigen-bom` | Bill of Materials for version management |
| Core | `com.github.jnzader.apigen:apigen-core` | Core CRUD functionality, entities, DTOs |
| Security | `com.github.jnzader.apigen:apigen-security` | JWT, OAuth2, RBAC authentication |
| CodeGen | `com.github.jnzader.apigen:apigen-codegen` | Code generation for APIs |
| GraphQL | `com.github.jnzader.apigen:apigen-graphql` | GraphQL API support |
| gRPC | `com.github.jnzader.apigen:apigen-grpc` | gRPC protocol support |
| Gateway | `com.github.jnzader.apigen:apigen-gateway` | API Gateway with Spring Cloud Gateway |

### Modules NOT Published

These modules are **not** published as they are applications, not libraries:

| Module | Purpose |
|--------|---------|
| `apigen-server` | HTTP server application (not a library) |
| `apigen-example` | Demo/example application |

## Using APiGen from JitPack

### Step 1: Add JitPack Repository

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

**Gradle (Groovy):**
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

**Maven:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step 2: Add Dependencies

**Using a Release Tag (Recommended):**
```groovy
dependencies {
    // Using the BOM for version management
    implementation platform('com.github.jnzader.apigen:apigen-bom:1.0.0')
    implementation 'com.github.jnzader.apigen:apigen-core'
    implementation 'com.github.jnzader.apigen:apigen-security' // Optional
}
```

**Using a Commit Hash:**
```groovy
dependencies {
    implementation 'com.github.jnzader.apigen:apigen-core:abc1234'
}
```

**Using a Branch (SNAPSHOT):**
```groovy
dependencies {
    implementation 'com.github.jnzader.apigen:apigen-core:main-SNAPSHOT'
}
```

### Maven Example

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.github.jnzader.apigen</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.github.jnzader.apigen</groupId>
        <artifactId>apigen-core</artifactId>
    </dependency>
</dependencies>
```

## Version Options

| Version Format | Example | Description |
|----------------|---------|-------------|
| Release tag | `1.0.0` | Stable release |
| Commit hash | `abc1234` | Specific commit |
| Branch-SNAPSHOT | `main-SNAPSHOT` | Latest from branch |

## Triggering a Build

JitPack builds are triggered when someone requests a dependency:

1. **First request:** JitPack clones and builds (may take a few minutes)
2. **Subsequent requests:** Served from cache (instant)

### Pre-building Releases

To pre-build a release, visit:
```
https://jitpack.io/#jnzader/apigen/1.0.0
```

Click "Get it" to trigger the build before users request it.

## Build Configuration

APiGen's `jitpack.yml` configures the build:

```yaml
jdk:
  - openjdk25

install:
  - ./gradlew clean publishToMavenLocal -x test -x pitest --no-daemon --parallel
```

### Notes:
- Uses Java 25 (same as the project requirement)
- Tests are skipped on JitPack (already run in CI)
- Multi-module builds are supported

## Checking Build Status

View build logs at:
```
https://jitpack.io/#jnzader/apigen/VERSION/build.log
```

## Troubleshooting

### Build Failed
1. Check the build log on JitPack
2. Ensure `./gradlew publishToMavenLocal` works locally
3. Check that JitPack supports your JDK version

### Dependency Not Found
1. Verify the repository URL includes JitPack
2. Check the group ID format: `com.github.{user}.{repo}`
3. Verify the version exists (tag or commit)

### Cache Issues
JitPack caches artifacts. To force a rebuild:
- Delete and recreate the Git tag
- Use a new commit hash

## Comparison: JitPack vs Maven Central

| Feature | JitPack | Maven Central |
|---------|---------|---------------|
| Setup | Zero | Requires signing, approval |
| Build | Automatic from Git | Manual publishing |
| Snapshots | Branch-based | Manual upload |
| Speed | First build slow | Always fast |
| Trust | Git-based | Signed artifacts |
| Cost | Free | Free |

**Recommendation:**
- Use JitPack for development and testing
- Consider Maven Central for production releases (requires more setup)

## Related Links

- [JitPack Documentation](https://jitpack.io/docs/)
- [JitPack Multi-Module Projects](https://jitpack.io/docs/BUILDING/#multi-module-projects)
- [APiGen on JitPack](https://jitpack.io/#jnzader/apigen)
