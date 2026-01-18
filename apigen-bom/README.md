# APiGen BOM (Bill of Materials)

Centralized dependency version management for APiGen modules.

## Purpose

The BOM ensures all APiGen modules and their dependencies use compatible versions, preventing version conflicts in your project.

## Usage

**Gradle:**
```groovy
dependencies {
    implementation platform('com.jnzader:apigen-bom:1.0.0-SNAPSHOT')
    implementation 'com.jnzader:apigen-core'
    implementation 'com.jnzader:apigen-security'  // Optional
}
```

**Maven:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.jnzader</groupId>
            <artifactId>apigen-bom</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Managed Dependencies

| Dependency | Version |
|------------|---------|
| Spring Boot | 4.0.0 |
| Spring Cloud | 2024.0.1 |
| MapStruct | 1.6.3 |
| JJWT | 0.13.0 |
| Resilience4j | 2.3.0 |
| Caffeine | 3.2.3 |
| SpringDoc OpenAPI | 3.0.0 |

See [main README](../README.md) for complete documentation.
