# 08 - TESTING EN APIGEN

## Tabla de Contenidos
- [Introducción](#introducción)
- [Configuración de Testing](#configuración-de-testing)
- [Tests Unitarios](#tests-unitarios)
- [Tests de Integración](#tests-de-integración)
- [Tests de Arquitectura](#tests-de-arquitectura)
- [Fixtures y Helpers](#fixtures-y-helpers)
- [Mejores Prácticas](#mejores-prácticas)
- [Ejecución de Tests](#ejecución-de-tests)

---

## Introducción

APiGen implementa una estrategia de testing completa que incluye:

### Tipos de Tests
- **Tests Unitarios**: Verifican componentes aislados (servicios, mappers, utils)
- **Tests de Integración**: Validan el flujo completo con base de datos real
- **Tests de Arquitectura**: Aseguran el cumplimiento de reglas arquitectónicas
- **Tests de Controladores**: Validan endpoints HTTP con MockMvc
- **Tests de Seguridad**: Verifican autenticación y autorización

### Tecnologías Utilizadas
- **JUnit 5**: Framework de testing
- **Mockito**: Mocking framework
- **AssertJ**: Assertions fluidas
- **TestContainers**: Contenedores Docker para tests
- **ArchUnit**: Tests de arquitectura
- **MockMvc**: Tests de controladores
- **WebTestClient**: Tests de integración HTTP

---

## Configuración de Testing

### Dependencias en build.gradle

```gradle
dependencies {
    // Testing Core
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webflux'
    testImplementation 'org.springframework.security:spring-security-test'

    // TestContainers
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation "org.testcontainers:testcontainers-junit-jupiter:${testcontainersVersion}"
    testImplementation "org.testcontainers:testcontainers-postgresql:${testcontainersVersion}"

    // Architecture Testing
    testImplementation "com.tngtech.archunit:archunit-junit5:${archunitVersion}"

    // Awaitility for async testing
    testImplementation "org.awaitility:awaitility:${awaitilityVersion}"

    // Lombok for tests
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'
    testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
}
```

### TestContainers Configuration

**TestcontainersConfiguration.java**
```java
@TestConfiguration
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection  // Spring Boot 3.1+ automatic connection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine")
        );
    }
}
```

**Ventajas de @ServiceConnection**
- Configura automáticamente datasource, URL, username y password
- No requiere properties manuales
- Compatible con Spring Boot 3.1+

### Configuración de Tests en build.gradle

```gradle
tasks.named('test') {
    // Cargar variables de entorno desde .env
    def envFile = file('.env')
    if (envFile.exists()) {
        envFile.eachLine { line ->
            if (line && !line.startsWith('#') && line.contains('=')) {
                def (key, value) = line.split('=', 2)
                environment key.trim(), value.trim()
            }
        }
    }

    useJUnitPlatform {
        // Por defecto, excluye tests que requieren configuración especial
        if (!project.hasProperty('test.all')) {
            if (!project.hasProperty('test.security')) {
                excludeTags 'security'
            }
            if (!project.hasProperty('test.integration')) {
                excludeTags 'integration'
            }
            if (!project.hasProperty('test.architecture')) {
                excludeTags 'architecture'
            }
        }
    }

    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = false
        exceptionFormat = 'full'
    }
}
```

### application-test.yaml

```yaml
# Test configuration
apigen:
  security:
    enabled: false  # Deshabilita seguridad en tests unitarios
    jwt:
      secret: dGVzdC1zZWNyZXQta2V5LWZvci1pbnRlZ3JhdGlvbi10ZXN0cw==
      expiration-minutes: 60

spring:
  jpa:
    hibernate:
      ddl-auto: update  # Permite crear TestEntity desde test sources
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  flyway:
    enabled: true  # Crea base_sequence y tablas base

logging:
  level:
    root: WARN
    com.jnzader.apigen: INFO
    org.springframework.security: DEBUG
```

---

## Tests Unitarios

### Tests de Servicios con Mockito

**BaseServiceImplTest.java** - Ejemplo completo

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BaseServiceImpl Unit Tests")
class BaseServiceImplTest {

    @Mock
    private TestEntityRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CacheEvictionService cacheEvictionService;

    private TestEntityServiceImpl service;

    @BeforeEach
    void setUp() {
        // Crear el servicio manualmente para control total
        service = new TestEntityServiceImpl(repository, cacheEvictionService);

        // Inyectar mocks en campos heredados
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);

        TestEntityBuilder.resetIdCounter();
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return entity when found")
        void shouldReturnEntityWhenFound() {
            // Given
            TestEntity entity = TestEntityBuilder.aTestEntityWithId()
                    .withName("Test Entity")
                    .build();
            given(repository.findById(1L)).willReturn(Optional.of(entity));

            // When
            Result<TestEntity, Exception> result = service.findById(1L);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getName()).isEqualTo("Test Entity");
            then(repository).should().findById(1L);
        }

        @Test
        @DisplayName("should return failure when entity not found")
        void shouldReturnFailureWhenNotFound() {
            // Given
            given(repository.findById(999L)).willReturn(Optional.empty());

            // When
            Result<TestEntity, Exception> result = service.findById(999L);

            // Then
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should save new entity and register created event")
        void shouldSaveNewEntityAndRegisterCreatedEvent() {
            // Given
            TestEntity newEntity = TestEntityBuilder.aTestEntity()
                    .withName("New Entity")
                    .build();
            TestEntity savedEntity = TestEntityBuilder.aTestEntityWithId()
                    .withName("New Entity")
                    .build();
            given(repository.save(any(TestEntity.class))).willReturn(savedEntity);

            // When
            Result<TestEntity, Exception> result = service.save(newEntity);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getId()).isNotNull();
            then(repository).should().save(any(TestEntity.class));
        }
    }

    @Nested
    @DisplayName("softDelete()")
    class SoftDeleteTests {

        @Test
        @DisplayName("should soft delete entity")
        void shouldSoftDeleteEntity() {
            // Given
            TestEntity entity = TestEntityBuilder.aTestEntityWithId()
                    .withName("Test")
                    .build();
            given(repository.findById(1L)).willReturn(Optional.of(entity));
            given(repository.save(any(TestEntity.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // When
            Result<Void, Exception> result = service.softDelete(1L, "test-user");

            // Then
            assertThat(result.isSuccess()).isTrue();

            ArgumentCaptor<TestEntity> captor =
                    ArgumentCaptor.forClass(TestEntity.class);
            then(repository).should().save(captor.capture());

            assertThat(captor.getValue().getEstado()).isFalse();
            assertThat(captor.getValue().getEliminadoPor()).isEqualTo("test-user");
        }
    }
}
```

**Patrones Clave:**
- ✅ `@ExtendWith(MockitoExtension.class)` - Habilita Mockito
- ✅ `@Mock` - Crea mocks automáticamente
- ✅ `@BeforeEach` - Setup antes de cada test
- ✅ `@Nested` - Organiza tests relacionados
- ✅ `given().willReturn()` - BDD style mocking
- ✅ `then().should()` - BDD style verification
- ✅ `ArgumentCaptor` - Captura argumentos para verificación

### Tests del Result Pattern

**ResultTest.java**

```java
@DisplayName("Result Pattern Tests")
class ResultTest {

    @Nested
    @DisplayName("Creation")
    class CreationTests {

        @Test
        @DisplayName("success() should create successful result")
        void successShouldCreateSuccessfulResult() {
            // When
            Result<String, Exception> result = Result.success("value");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.isFailure()).isFalse();
            assertThat(result.orElseThrow()).isEqualTo("value");
        }

        @Test
        @DisplayName("failure() should create failed result")
        void failureShouldCreateFailedResult() {
            // When
            Exception error = new RuntimeException("error");
            Result<String, Exception> result = Result.failure(error);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isFailure()).isTrue();
            assertThatThrownBy(result::orElseThrow)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("error");
        }

        @Test
        @DisplayName("of() should capture successful computation")
        void ofShouldCaptureSuccessfulComputation() {
            // When
            Result<Integer, Exception> result = Result.of(() -> 1 + 1);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }

        @Test
        @DisplayName("of() should capture exception")
        void ofShouldCaptureException() {
            // When
            Result<Integer, Exception> result = Result.of(() -> {
                throw new IllegalArgumentException("test error");
            });

            // Then
            assertThat(result.isFailure()).isTrue();
        }
    }

    @Nested
    @DisplayName("Transformations")
    class TransformationTests {

        @Test
        @DisplayName("map() should transform success value")
        void mapShouldTransformSuccessValue() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<String, Exception> mapped =
                    result.map(n -> "Number: " + n);

            // Then
            assertThat(mapped.isSuccess()).isTrue();
            assertThat(mapped.orElseThrow()).isEqualTo("Number: 5");
        }

        @Test
        @DisplayName("flatMap() should chain successful results")
        void flatMapShouldChainSuccessfulResults() {
            // Given
            Result<Integer, Exception> result = Result.success(5);

            // When
            Result<Integer, Exception> chained =
                    result.flatMap(n -> Result.success(n * 2));

            // Then
            assertThat(chained.isSuccess()).isTrue();
            assertThat(chained.orElseThrow()).isEqualTo(10);
        }

        @Test
        @DisplayName("flatMap() should short-circuit on failure")
        void flatMapShouldShortCircuitOnFailure() {
            // Given
            Result<Integer, Exception> result =
                    Result.failure(new RuntimeException("error"));
            AtomicBoolean called = new AtomicBoolean(false);

            // When
            Result<Integer, Exception> chained = result.flatMap(n -> {
                called.set(true);
                return Result.success(n * 2);
            });

            // Then
            assertThat(chained.isFailure()).isTrue();
            assertThat(called.get()).isFalse(); // No se ejecutó
        }
    }
}
```

**Características Verificadas:**
- ✅ Creación de Success y Failure
- ✅ Transformaciones con `map()` y `flatMap()`
- ✅ Extracción de valores con `orElse()`, `orElseGet()`, `orElseThrow()`
- ✅ Side effects con `onSuccess()` y `onFailure()`
- ✅ Chaining operations
- ✅ Short-circuit en operaciones

---

## Tests de Integración

### Tests con @SpringBootTest

**ApigenApplicationTests.java** - Test de integración completo

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Tag("integration")
public class ApigenApplicationTests {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private static Long createdEntityId;

    @Test
    @Order(1)
    void shouldCreateEntity() {
        TestEntityDTO newDto = new TestEntityDTO(null, true, "Test Name");

        webTestClient.post().uri("/test-entities")
                .bodyValue(newDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TestEntityDTO.class)
                .value(responseDto -> {
                    assertThat(responseDto.id()).isNotNull();
                    assertThat(responseDto.name()).isEqualTo("Test Name");
                    createdEntityId = responseDto.id();
                });
    }

    @Test
    @Order(2)
    void shouldFindEntityById() {
        webTestClient.get().uri("/test-entities/" + createdEntityId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TestEntityDTO.class)
                .value(responseDto -> {
                    assertThat(responseDto.id()).isEqualTo(createdEntityId);
                });
    }

    @Test
    @Order(3)
    void shouldUpdateEntity() {
        TestEntityDTO updatedDto =
                new TestEntityDTO(null, true, "Updated Name");

        webTestClient.put().uri("/test-entities/" + createdEntityId)
                .bodyValue(updatedDto)
                .exchange()
                .expectStatus().isOk();

        // Verify the update
        webTestClient.get().uri("/test-entities/" + createdEntityId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TestEntityDTO.class)
                .value(responseDto -> {
                    assertThat(responseDto.name()).isEqualTo("Updated Name");
                });
    }

    @Test
    @Order(4)
    void shouldDeleteEntity() {
        webTestClient.delete().uri("/test-entities/" + createdEntityId)
                .exchange()
                .expectStatus().isNoContent();

        // Verify the deletion
        webTestClient.get().uri("/test-entities/" + createdEntityId)
                .exchange()
                .expectStatus().isNotFound();
    }
}
```

**Características:**
- ✅ `@SpringBootTest` con puerto aleatorio
- ✅ `@Import(TestcontainersConfiguration.class)` - PostgreSQL real
- ✅ `@TestMethodOrder` - Tests ordenados (CRUD completo)
- ✅ `WebTestClient` - Cliente HTTP reactivo
- ✅ Verificación de códigos HTTP (201, 200, 204, 404)
- ✅ Tests end-to-end con base de datos real

### Tests con MockMvc

**BaseControllerTest.java**

```java
@WebMvcTest(TestEntityControllerImpl.class)
@Import({TestEntityMapper.class})
@ActiveProfiles("test")
@DisplayName("BaseController Tests")
@Tag("security")
class BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TestEntityService testEntityService;

    @MockitoBean
    private TestEntityMapper testEntityMapper;

    // Mocks para seguridad
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /test-entities")
    class GetAllTests {

        @Test
        @WithMockUser
        @DisplayName("should return list of entities")
        void shouldReturnListOfEntities() throws Exception {
            // Given
            Page<TestEntity> page = new PageImpl<>(
                List.of(testEntity),
                PageRequest.of(0, 20),
                1
            );
            given(testEntityService.findAll(
                any(Specification.class),
                any(Pageable.class)
            )).willReturn(Result.success(page));
            given(testEntityMapper.toDTO(testEntity))
                    .willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(get("/test-entities")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id",
                            is(testEntity.getId().intValue())))
                    .andExpect(jsonPath("$.content[0].name",
                            is("Test Entity")));
        }
    }

    @Nested
    @DisplayName("POST /test-entities")
    class CreateTests {

        @Test
        @WithMockUser
        @DisplayName("should create entity and return 201")
        void shouldCreateEntityAndReturn201() throws Exception {
            // Given
            TestEntityDTO newDto = new TestEntityDTO(null, true, "New");
            given(testEntityMapper.toEntity(any(TestEntityDTO.class)))
                    .willReturn(testEntity);
            given(testEntityService.save(any(TestEntity.class)))
                    .willReturn(Result.success(testEntity));
            given(testEntityMapper.toDTO(testEntity))
                    .willReturn(testEntityDTO);

            // When/Then
            mockMvc.perform(post("/test-entities")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()));
        }

        @Test
        @WithMockUser
        @DisplayName("should return 400 for invalid input")
        void shouldReturn400ForInvalidInput() throws Exception {
            // Given
            TestEntityDTO invalidDto = new TestEntityDTO(null, true, null);

            // When/Then - Validation should fail
            mockMvc.perform(post("/test-entities")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(status().isBadRequest());
        }
    }
}
```

**Características:**
- ✅ `@WebMvcTest` - Solo carga el controller layer
- ✅ `@MockitoBean` - Mockea las dependencias
- ✅ `@WithMockUser` - Simula usuario autenticado
- ✅ `.with(csrf())` - Incluye token CSRF
- ✅ `jsonPath()` - Valida respuestas JSON
- ✅ Tests rápidos sin levantar servidor completo

---

## Tests de Arquitectura

### Tests con ArchUnit

**ArchitectureTest.java**

```java
@DisplayName("Architecture Tests")
@Tag("architecture")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.jnzader.apigen";
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Nested
    @DisplayName("Layered Architecture")
    class LayeredArchitectureTests {

        @Test
        @DisplayName("should respect hexagonal layer dependencies")
        void shouldRespectLayerDependencies() {
            ArchRule rule = layeredArchitecture()
                    .consideringAllDependencies()

                    // Domain layer - innermost, no external dependencies
                    .layer("Domain").definedBy(
                            "..domain.entity..",
                            "..domain.event..",
                            "..domain.exception..",
                            "..domain.repository..",
                            "..domain.specification.."
                    )

                    // Application layer - orchestrates domain
                    .layer("Application").definedBy(
                            "..application.dto..",
                            "..application.mapper..",
                            "..application.service..",
                            "..application.validation..",
                            "..application.util.."
                    )

                    // Infrastructure layer - external concerns
                    .layer("Infrastructure").definedBy(
                            "..infrastructure.config..",
                            "..infrastructure.controller..",
                            "..infrastructure.filter..",
                            "..infrastructure.hateoas..",
                            "..infrastructure.aspect..",
                            "..infrastructure.event..",
                            "..infrastructure.exception..",
                            "..infrastructure.util..",
                            "..infrastructure.jwt..",
                            "..infrastructure.audit.."
                    )

                    // Domain should not depend on Application or Infrastructure
                    .whereLayer("Domain")
                            .mayOnlyBeAccessedByLayers("Application", "Infrastructure")

                    // Application should not depend on Infrastructure
                    .whereLayer("Application")
                            .mayOnlyBeAccessedByLayers("Infrastructure")

                    // Infrastructure is the outermost layer
                    .whereLayer("Infrastructure")
                            .mayNotBeAccessedByAnyLayer();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionTests {

        @Test
        @DisplayName("controllers should be suffixed with Controller or ControllerImpl")
        void controllersShouldBeSuffixedWithController() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.controller..")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Controller")
                    .orShould().haveSimpleNameEndingWith("ControllerImpl");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("services should be suffixed with Service or ServiceImpl")
        void servicesShouldBeSuffixedWithService() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("ServiceImpl");

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("DTOs should be suffixed with DTO")
        void dtosShouldBeSuffixedWithDTO() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.dto..")
                    .and().areNotInterfaces()
                    .and().areNotEnums()
                    .and().doNotHaveSimpleName("ValidationGroups")
                    .should().haveSimpleNameEndingWith("DTO")
                    .orShould().haveSimpleNameEndingWith("Validated");

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Required Annotations")
    class AnnotationTests {

        @Test
        @DisplayName("controllers should be annotated with @RestController")
        void controllersShouldBeAnnotatedWithRestController() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..infrastructure.controller..")
                    .and().haveSimpleNameEndingWith("ControllerImpl")
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should().beAnnotatedWith(RestController.class)
                    .allowEmptyShould(true);

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("services should be annotated with @Service")
        void servicesShouldBeAnnotatedWithService() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..application.service..")
                    .and().haveSimpleNameEndingWith("ServiceImpl")
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should().beAnnotatedWith(Service.class)
                    .orShould().beAnnotatedWith(Component.class)
                    .allowEmptyShould(true);

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("entities should be annotated with @Entity")
        void entitiesShouldBeAnnotatedWithEntity() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..domain.entity..")
                    .and().areNotInterfaces()
                    .and().areNotEnums()
                    .and().areNotMemberClasses()
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .should().beAnnotatedWith(Entity.class)
                    .orShould().beAnnotatedWith(MappedSuperclass.class);

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Dependencies")
    class DependencyTests {

        @Test
        @DisplayName("domain should not depend on application or infrastructure")
        void domainShouldNotDependOnOuterLayers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..application.service..",
                            "..infrastructure.controller..",
                            "..infrastructure.config.."
                    );

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("should not have cyclic dependencies between packages")
        void shouldNotHaveCyclicDependencies() {
            ArchRule rule = slices()
                    .matching(BASE_PACKAGE + ".(*)..")
                    .should().beFreeOfCycles();

            rule.check(importedClasses);
        }
    }

    @Nested
    @DisplayName("Code Quality")
    class CodeQualityTests {

        @Test
        @DisplayName("interfaces should not have Hungarian notation 'I' prefix")
        void interfacesShouldNotHaveIPrefix() {
            ArchRule rule = classes()
                    .that().areInterfaces()
                    .and().haveSimpleNameStartingWith("I")
                    .and().doNotHaveSimpleName("Import")
                    .should().haveSimpleNameNotStartingWith("I")
                    .because("Hungarian notation is discouraged")
                    .allowEmptyShould(true);

            rule.check(importedClasses);
        }

        @Test
        @DisplayName("utility classes should be final")
        void utilityClassesShouldBeFinal() {
            ArchRule rule = classes()
                    .that().resideInAnyPackage("..application.util..",
                                               "..infrastructure.util..")
                    .and().haveSimpleNameEndingWith("Utils")
                    .should().haveModifier(JavaModifier.FINAL);

            rule.check(importedClasses);
        }
    }
}
```

**Reglas Arquitectónicas Validadas:**
- ✅ Respeto de capas hexagonales (Domain → Application → Infrastructure)
- ✅ Nomenclatura consistente (Controller, Service, Repository, DTO)
- ✅ Anotaciones requeridas (@RestController, @Service, @Entity)
- ✅ Domain no depende de Application/Infrastructure
- ✅ No hay dependencias cíclicas
- ✅ No Hungarian notation (IService, IRepository)
- ✅ Utility classes son final

---

## Fixtures y Helpers

### TestEntityBuilder

**TestEntityBuilder.java** - Builder Pattern para tests

```java
public class TestEntityBuilder {

    private static final AtomicLong ID_COUNTER = new AtomicLong(1);

    private Long id;
    private String name = "Default Name";
    private Boolean estado = true;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private String creadoPor = "test-user";
    private Long version = 0L;

    private TestEntityBuilder() {}

    public static TestEntityBuilder aTestEntity() {
        return new TestEntityBuilder();
    }

    public static TestEntityBuilder aTestEntityWithId() {
        return new TestEntityBuilder()
                .withId(ID_COUNTER.getAndIncrement());
    }

    public TestEntityBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestEntityBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public TestEntityBuilder withEstado(Boolean estado) {
        this.estado = estado;
        return this;
    }

    public TestEntityBuilder deleted() {
        this.estado = false;
        return this;
    }

    public TestEntityBuilder active() {
        this.estado = true;
        return this;
    }

    public TestEntity build() {
        TestEntity entity = new TestEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setEstado(estado);
        entity.setFechaCreacion(fechaCreacion);
        entity.setCreadoPor(creadoPor);
        entity.setVersion(version);
        return entity;
    }

    public static void resetIdCounter() {
        ID_COUNTER.set(1);
    }
}
```

**Uso:**
```java
// Entidad sin ID (para crear)
TestEntity newEntity = TestEntityBuilder.aTestEntity()
        .withName("New Product")
        .build();

// Entidad con ID autoincrementado
TestEntity existing = TestEntityBuilder.aTestEntityWithId()
        .withName("Existing Product")
        .build();

// Entidad eliminada
TestEntity deleted = TestEntityBuilder.aTestEntityWithId()
        .withName("Deleted Product")
        .deleted()
        .build();

// Reset del contador entre tests
@BeforeEach
void setUp() {
    TestEntityBuilder.resetIdCounter();
}
```

### TestConstants

**TestConstants.java** - Constantes compartidas

```java
public final class TestConstants {

    private TestConstants() {}

    // IDs
    public static final Long VALID_ID = 1L;
    public static final Long INVALID_ID = 999999L;
    public static final Long NEGATIVE_ID = -1L;

    // Strings
    public static final String VALID_NAME = "Test Entity";
    public static final String UPDATED_NAME = "Updated Entity";
    public static final String EMPTY_STRING = "";
    public static final String BLANK_STRING = "   ";

    // Users
    public static final String TEST_USER = "test-user";
    public static final String ADMIN_USER = "admin";

    // Pagination
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    // Batch
    public static final int SMALL_BATCH_SIZE = 5;
    public static final int LARGE_BATCH_SIZE = 500;
    public static final int EXCEEDING_BATCH_SIZE = 15000;

    // Timeouts
    public static final long SHORT_TIMEOUT_MS = 100;
    public static final long MEDIUM_TIMEOUT_MS = 500;
    public static final long LONG_TIMEOUT_MS = 2000;

    // HTTP
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}
```

**Uso:**
```java
import static com.jnzader.apigen.support.TestConstants.*;

@Test
void shouldFindById() {
    given(repository.findById(VALID_ID)).willReturn(Optional.of(entity));

    Result<TestEntity, Exception> result = service.findById(VALID_ID);

    assertThat(result.isSuccess()).isTrue();
}
```

---

## Mejores Prácticas

### Patrón Given-When-Then (BDD)

```java
@Test
@DisplayName("should save new entity and return success")
void shouldSaveNewEntityAndReturnSuccess() {
    // Given - Preparar el contexto
    TestEntity newEntity = TestEntityBuilder.aTestEntity()
            .withName("New Product")
            .build();
    given(repository.save(any(TestEntity.class)))
            .willReturn(newEntity);

    // When - Ejecutar la acción
    Result<TestEntity, Exception> result = service.save(newEntity);

    // Then - Verificar el resultado
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.orElseThrow().getName()).isEqualTo("New Product");
    then(repository).should().save(any(TestEntity.class));
}
```

### Patrón AAA (Arrange-Act-Assert)

```java
@Test
void testSoftDelete() {
    // Arrange
    TestEntity entity = TestEntityBuilder.aTestEntityWithId().build();
    when(repository.findById(1L)).thenReturn(Optional.of(entity));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    Result<Void, Exception> result = service.softDelete(1L, "admin");

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(repository).save(argThat(e -> !e.getEstado()));
}
```

### Organización de Tests

```
src/test/java/
├── com/jnzader/apigen/
│   ├── ApigenApplicationTests.java          # Integration tests
│   ├── TestcontainersConfiguration.java     # TestContainers config
│   │
│   ├── architecture/
│   │   └── ArchitectureTest.java            # ArchUnit tests
│   │
│   ├── core/
│   │   ├── application/
│   │   │   ├── dto/
│   │   │   │   └── ValidationTest.java      # Validation tests
│   │   │   ├── service/
│   │   │   │   └── BaseServiceImplTest.java # Service unit tests
│   │   │   └── util/
│   │   │       └── ResultTest.java          # Result pattern tests
│   │   │
│   │   └── infrastructure/
│   │       └── controller/
│   │           └── BaseControllerTest.java  # Controller tests
│   │
│   ├── fixtures/                            # Test entities
│   │   ├── TestEntity.java
│   │   ├── TestEntityDTO.java
│   │   ├── TestEntityService.java
│   │   └── ...
│   │
│   └── support/                             # Test utilities
│       ├── TestEntityBuilder.java           # Builders
│       └── TestConstants.java               # Constants
│
src/test/resources/
├── application-test.yaml                    # Test configuration
├── application-test-security.yaml           # Security test config
└── logback-test.xml                         # Test logging
```

### Nomenclatura de Tests

**Convenciones:**
- ✅ `should[Expected]When[Condition]` - Describe el comportamiento esperado
- ✅ `@DisplayName("descripción legible")` - Nombres legibles
- ✅ `@Nested class [Feature]Tests` - Agrupa tests relacionados

**Ejemplos:**
```java
@Nested
@DisplayName("findById()")
class FindByIdTests {

    @Test
    @DisplayName("should return entity when found")
    void shouldReturnEntityWhenFound() { }

    @Test
    @DisplayName("should return failure when entity not found")
    void shouldReturnFailureWhenNotFound() { }

    @Test
    @DisplayName("should throw exception for null ID")
    void shouldThrowExceptionForNullId() { }
}
```

### Uso de AssertJ

**Assertions Fluidas:**
```java
// Basic assertions
assertThat(result.isSuccess()).isTrue();
assertThat(entity.getName()).isEqualTo("Test");
assertThat(entity.getId()).isNotNull();

// Collections
assertThat(entities).hasSize(5);
assertThat(entities).isEmpty();
assertThat(entities).contains(entity1, entity2);
assertThat(entities).extracting("name")
        .containsExactly("A", "B", "C");

// Exceptions
assertThatThrownBy(() -> service.findById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ID cannot be null");

// Predicates
assertThat(entities).allMatch(e -> e.getEstado());
assertThat(entities).anyMatch(e -> e.getName().equals("Test"));
assertThat(entities).noneMatch(e -> e.getId() == null);

// Object properties
assertThat(entity)
        .extracting("id", "name", "estado")
        .containsExactly(1L, "Test", true);
```

### Mockito - BDD Style

**Given-When-Then con BDDMockito:**
```java
import static org.mockito.BDDMockito.*;

// Given
given(repository.findById(1L)).willReturn(Optional.of(entity));
given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

// When
Result<TestEntity, Exception> result = service.findById(1L);

// Then
then(repository).should().findById(1L);
then(repository).should(never()).deleteById(anyLong());
then(repository).shouldHaveNoMoreInteractions();
```

### ArgumentCaptor

**Capturar y verificar argumentos:**
```java
@Captor
private ArgumentCaptor<TestEntity> entityCaptor;

@Test
void shouldSetCorrectDeletedBy() {
    // Given
    given(repository.findById(1L)).willReturn(Optional.of(entity));
    given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

    // When
    service.softDelete(1L, "admin");

    // Then
    then(repository).should().save(entityCaptor.capture());
    TestEntity captured = entityCaptor.getValue();

    assertThat(captured.getEstado()).isFalse();
    assertThat(captured.getEliminadoPor()).isEqualTo("admin");
    assertThat(captured.getFechaEliminacion()).isNotNull();
}
```

---

## Ejecución de Tests

### Comandos Gradle

```bash
# Solo tests unitarios (por defecto)
./gradlew test

# Incluir tests de seguridad
./gradlew test -Ptest.security

# Incluir tests de integración (TestContainers)
./gradlew test -Ptest.integration

# Incluir tests de arquitectura (ArchUnit)
./gradlew test -Ptest.architecture

# Ejecutar TODOS los tests
./gradlew test -Ptest.all

# Test específico
./gradlew test --tests BaseServiceImplTest

# Test con logging detallado
./gradlew test --info

# Tests con generación de reportes
./gradlew test jacocoTestReport  # Cuando JaCoCo soporte Java 25
```

### Tags de JUnit 5

**Etiquetado de tests:**
```java
@Tag("integration")
class ApigenApplicationTests { }

@Tag("architecture")
class ArchitectureTest { }

@Tag("security")
class BaseControllerTest { }

@Tag("unit")
class BaseServiceImplTest { }
```

**Ejecución por tags:**
```gradle
test {
    useJUnitPlatform {
        includeTags 'unit'
        excludeTags 'integration', 'architecture'
    }
}
```

### Reporte de Tests

**Configuración en build.gradle:**
```gradle
test {
    testLogging {
        events "passed", "skipped", "failed"
        showStandardStreams = false
        exceptionFormat = 'full'
    }

    afterSuite { desc, result ->
        if (!desc.parent) {
            println "\n=========================================="
            println "Test Results: ${result.resultType}"
            println "=========================================="
            println "Total:   ${result.testCount}"
            println "Passed:  ${result.successfulTestCount}"
            println "Failed:  ${result.failedTestCount}"
            println "Skipped: ${result.skippedTestCount}"
            println "=========================================="
        }
    }
}
```

**Output del reporte:**
```
==========================================
Test Results: SUCCESS
==========================================
Total:   157
Passed:  157
Failed:  0
Skipped: 0
==========================================
```

### CI/CD Integration

**GitHub Actions - .github/workflows/test.yml:**
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 25
        uses: actions/setup-java@v3
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Run unit tests
        run: ./gradlew test

      - name: Run integration tests
        run: ./gradlew test -Ptest.integration

      - name: Run architecture tests
        run: ./gradlew test -Ptest.architecture

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/reports/tests/
```

---

## Resumen

### Cobertura de Tests en APiGen

| Tipo | Framework | Cobertura | Propósito |
|------|-----------|-----------|-----------|
| Unitarios | JUnit 5 + Mockito | Servicios, Mappers, Utils | Lógica de negocio aislada |
| Integración | TestContainers | CRUD completo | Flujo end-to-end con DB real |
| Controladores | MockMvc | Endpoints HTTP | Validación de API REST |
| Arquitectura | ArchUnit | Reglas arquitectónicas | Calidad de código |
| Seguridad | Spring Security Test | Auth/Authz | Validación de permisos |

### Estadísticas del Proyecto

- **Total de archivos de test**: 19+
- **Frameworks utilizados**: 6 (JUnit, Mockito, AssertJ, TestContainers, ArchUnit, MockMvc)
- **Tipos de tests**: 5 (Unit, Integration, Controller, Architecture, Security)
- **Cobertura**: Servicios, Controllers, Mappers, Utils, Result Pattern

### Ventajas de la Estrategia de Testing

✅ **Confianza**: Tests completos desde unitarios hasta end-to-end
✅ **Velocidad**: Tests unitarios rápidos, integración con tags
✅ **Realismo**: TestContainers con PostgreSQL real
✅ **Arquitectura**: ArchUnit valida reglas hexagonales
✅ **Mantenibilidad**: Builders y fixtures reutilizables
✅ **CI/CD Ready**: Tags para ejecución selectiva

### Próximos Pasos

1. ✅ **Cobertura**: Habilitar JaCoCo cuando soporte Java 25
2. ✅ **Performance**: Agregar tests de carga con Gatling
3. ✅ **Contract Testing**: Implementar Spring Cloud Contract
4. ✅ **Mutation Testing**: Agregar PIT para mutation testing
5. ✅ **E2E**: Tests con Selenium/Playwright para UI (si aplica)

---

**¡Testing completo = Código confiable!** 🧪✅
