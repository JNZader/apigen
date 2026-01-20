# APiGen GraphQL Module

GraphQL API layer alternative to REST for APiGen applications.

## Features

- **Schema Building**: Fluent API for constructing GraphQL schemas
- **DataLoader Support**: N+1 query prevention with batched loading
- **Error Handling**: RFC 7807-aligned error responses
- **Context Management**: Request-scoped context with user ID and locale
- **HTTP Endpoint**: Ready-to-use GraphQL controller

## Quick Start

### 1. Add Dependency

```gradle
implementation 'com.jnzader:apigen-graphql:1.0.0-SNAPSHOT'
```

### 2. Enable GraphQL

```yaml
apigen:
  graphql:
    enabled: true
    path: /graphql
    tracing:
      enabled: false
```

### 3. Define Schema

```java
@Configuration
public class GraphQLConfig {

    @Bean
    public GraphQLSchema graphQLSchema(ProductService productService) {
        return SchemaBuilder.newSchema()
            .type("Product")
                .field("id", GraphQLID, true)
                .field("name", GraphQLString, true)
                .field("price", GraphQLFloat)
            .endType()
            .query("product")
                .argument("id", GraphQLID, true)
                .returns("Product")
                .fetcher(env -> productService.findById(env.getArgument("id")))
            .endQuery()
            .query("products")
                .returnsList("Product")
                .fetcher(env -> productService.findAll())
            .endQuery()
            .build();
    }
}
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `apigen.graphql.enabled` | `false` | Enable GraphQL support |
| `apigen.graphql.path` | `/graphql` | GraphQL endpoint path |
| `apigen.graphql.tracing.enabled` | `false` | Enable Apollo tracing |
| `apigen.graphql.http.enabled` | `true` | Enable HTTP controller |

## Components

### SchemaBuilder

Fluent API for building GraphQL schemas:

```java
GraphQLSchema schema = SchemaBuilder.newSchema()
    .type("Product")
        .field("id", GraphQLID, true)
        .field("name", GraphQLString, true)
    .endType()
    .inputType("CreateProductInput")
        .field("name", GraphQLString, true)
        .field("price", GraphQLFloat, true)
    .endInputType()
    .query("products")
        .returnsList("Product")
        .fetcher(productsDataFetcher)
    .endQuery()
    .mutation("createProduct")
        .argument("input", "CreateProductInput", true)
        .returns("Product")
        .fetcher(createProductFetcher)
    .endMutation()
    .build();
```

### BaseDataFetcher

Base class for data fetchers with utility methods:

```java
@Component
public class ProductByIdFetcher extends BaseDataFetcher<Product> {

    private final ProductRepository repository;

    @Override
    protected Product fetch(DataFetchingEnvironment env) {
        String id = getRequiredArgument(env, "id");
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }
}
```

### DataLoaderRegistry

Prevent N+1 queries with batched loading:

```java
@Component
public class ProductDataLoaderRegistrar implements DataLoaderRegistrar {

    private final ProductRepository repository;

    @Override
    public void register(DataLoaderRegistry registry) {
        registry.<String, Product>register("products", ids -> {
            List<Product> products = repository.findAllById(ids);
            return products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        });
    }
}
```

### Error Handling

Errors are converted to RFC 7807-aligned GraphQL errors:

```json
{
  "errors": [{
    "message": "Product not found",
    "extensions": {
      "type": "NOT_FOUND",
      "status": 404,
      "detail": "Product with ID 123 does not exist"
    },
    "path": ["product"]
  }]
}
```

## Dependencies

- GraphQL Java 22.3
- Java DataLoader 3.4.0
- Spring Boot 4.0.0

## License

MIT License
