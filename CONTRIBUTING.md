# Contributing to APiGen

Thank you for your interest in contributing to APiGen! This document provides guidelines for contributing to the project.

## Getting Started

### Prerequisites

- Java 25+
- Gradle 8.x (or use the included wrapper)
- Git
- PostgreSQL 17+ (for integration tests) or Docker

### Setting Up the Development Environment

1. **Fork and clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/apigen.git
   cd apigen
   ```

2. **Build the project**
   ```bash
   ./gradlew build
   ```

3. **Run tests**
   ```bash
   ./gradlew test
   ```

## Project Structure

```
apigen/
├── apigen-core/          # Core functionality (CRUD, HATEOAS, caching)
├── apigen-security/      # JWT authentication and rate limiting
├── apigen-codegen/       # Code generation from SQL/commands
├── apigen-bom/           # Bill of Materials for dependency management
└── apigen-example/       # Example application demonstrating usage
```

## Development Workflow

### 1. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-description
```

### 2. Make Your Changes

- Follow the existing code style
- Write tests for new functionality
- Update documentation as needed

### 3. Run Tests

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :apigen-core:test

# Run with coverage
./gradlew test jacocoTestReport
```

### 4. Commit Your Changes

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
git commit -m "feat(core): add support for custom filters"
git commit -m "fix(security): resolve token refresh race condition"
git commit -m "docs: update README with new configuration options"
```

**Commit Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### 5. Push and Create a Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## Code Style

### Java Style Guide

- Use meaningful variable and method names
- Keep methods short and focused (< 20 lines ideal)
- Follow Java naming conventions
- Use records for DTOs
- Prefer composition over inheritance
- Use `Optional` to avoid null

### Example Entity

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity<Long> {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Positive
    private BigDecimal price;

    // Getters and setters using Lombok or manual
}
```

### Example DTO

```java
public record ProductDTO(
    Long id,
    Boolean activo,
    @NotBlank String name,
    @Positive BigDecimal price
) implements BaseDTO<Long> {

    public static ProductDTO of(Long id, Boolean activo, String name, BigDecimal price) {
        return new ProductDTO(id, activo, name, price);
    }
}
```

## Testing Guidelines

### Unit Tests

- Test one thing per test method
- Use descriptive test names
- Use `@DisplayName` for readability

```java
@Test
@DisplayName("should throw exception when entity not found")
void shouldThrowExceptionWhenEntityNotFound() {
    assertThrows(EntityNotFoundException.class, () -> {
        service.findById(999L);
    });
}
```

### Integration Tests

- Use `@SpringBootTest` for full context
- Use `@WebMvcTest` for controller slice tests
- Use `@DataJpaTest` for repository tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isOk());
    }
}
```

## Pull Request Checklist

Before submitting a PR, ensure:

- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New code has appropriate test coverage
- [ ] Documentation is updated if needed
- [ ] Commit messages follow conventions
- [ ] PR description explains the changes

## Module-Specific Guidelines

### apigen-core

- Maintain backward compatibility
- Consider performance implications
- Document public APIs with Javadoc

### apigen-security

- Never log sensitive data (tokens, passwords)
- Follow OWASP security guidelines
- Test security configurations thoroughly

### apigen-codegen

- Validate generated code compiles
- Support multiple output formats
- Handle edge cases in SQL parsing

## Getting Help

- Open an issue for bugs or feature requests
- Use discussions for questions
- Check existing issues before creating new ones

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
