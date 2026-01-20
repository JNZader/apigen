# GraphQL Module

The `apigen-graphql` module adds GraphQL API support.

## Features

- **Auto-generated Schema** - From entities
- **Queries** - Read operations
- **Mutations** - Write operations
- **Subscriptions** - Real-time updates

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-graphql'
```

## Configuration

```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
```

## Usage

GraphQL endpoint: `/graphql`
GraphiQL UI: `/graphiql`

### Query Example

```graphql
query {
  products(page: 0, size: 10) {
    content {
      id
      name
      price
    }
    totalElements
  }
}
```

### Mutation Example

```graphql
mutation {
  createProduct(input: {
    name: "Laptop"
    price: 999.99
  }) {
    id
    name
  }
}
```
