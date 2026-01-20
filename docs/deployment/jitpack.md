# Publishing to JitPack

JitPack is a package repository for JVM projects that builds directly from GitHub.

## APiGen on JitPack

**URL:** https://jitpack.io/#jnzader/apigen

## Available Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| BOM | `com.github.jnzader.apigen:apigen-bom` | Version management |
| Core | `com.github.jnzader.apigen:apigen-core` | Core CRUD functionality |
| Security | `com.github.jnzader.apigen:apigen-security` | JWT, OAuth2, RBAC |
| CodeGen | `com.github.jnzader.apigen:apigen-codegen` | Code generation |
| GraphQL | `com.github.jnzader.apigen:apigen-graphql` | GraphQL support |
| gRPC | `com.github.jnzader.apigen:apigen-grpc` | gRPC support |
| Gateway | `com.github.jnzader.apigen:apigen-gateway` | API Gateway |

## Usage

### Step 1: Add JitPack Repository

**Gradle:**
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

**Gradle:**
```groovy
dependencies {
    implementation platform('com.github.jnzader.apigen:apigen-bom:1.0.0')
    implementation 'com.github.jnzader.apigen:apigen-core'
}
```

**Maven:**
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

| Format | Example | Description |
|--------|---------|-------------|
| Release tag | `1.0.0` | Stable release |
| Commit hash | `abc1234` | Specific commit |
| Branch-SNAPSHOT | `main-SNAPSHOT` | Latest from branch |

## Build Status

Check build logs at:
```
https://jitpack.io/#jnzader/apigen/VERSION/build.log
```
