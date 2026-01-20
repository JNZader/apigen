# gRPC Module

The `apigen-grpc` module adds gRPC protocol support.

## Features

- **Proto Generation** - Auto-generated from entities
- **Streaming** - Server/client/bidirectional
- **High Performance** - Binary protocol

## Installation

```groovy
implementation 'com.github.jnzader.apigen:apigen-grpc'
```

## Configuration

```yaml
grpc:
  server:
    port: 9090
```

## Proto Definition

APiGen auto-generates proto files:

```protobuf
service ProductService {
  rpc GetProduct(GetProductRequest) returns (Product);
  rpc ListProducts(ListProductsRequest) returns (ProductList);
  rpc CreateProduct(CreateProductRequest) returns (Product);
  rpc UpdateProduct(UpdateProductRequest) returns (Product);
  rpc DeleteProduct(DeleteProductRequest) returns (Empty);
}
```

## Client Usage

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 9090)
    .usePlaintext()
    .build();

ProductServiceGrpc.ProductServiceBlockingStub stub =
    ProductServiceGrpc.newBlockingStub(channel);

Product product = stub.getProduct(
    GetProductRequest.newBuilder()
        .setId("123")
        .build()
);
```
