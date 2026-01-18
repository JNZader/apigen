<#
.SYNOPSIS
    Generador de entidades para APiGen

.DESCRIPTION
    Crea automaticamente la estructura completa para una nueva entidad:
    - Entity, DTO, Mapper, Repository, Service, Controller
    - Migracion SQL
    - Test unitario

.PARAMETER EntityName
    Nombre de la entidad en PascalCase (ej: Product, Customer, Order)

.PARAMETER ModuleName
    Nombre del modulo en minusculas (ej: products, customers, orders)

.PARAMETER Fields
    Campos adicionales en formato "nombre:tipo" separados por coma
    Tipos soportados: string, int, long, decimal, boolean, date, datetime
    Ejemplo: "name:string,price:decimal,stock:int"

.EXAMPLE
    .\generate-entity.ps1 Product products "name:string,price:decimal,stock:int,sku:string"

.EXAMPLE
    .\generate-entity.ps1 Customer customers "firstName:string,lastName:string,email:string"
#>

param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$EntityName,

    [Parameter(Mandatory=$true, Position=1)]
    [string]$ModuleName,

    [Parameter(Mandatory=$false, Position=2)]
    [string]$Fields = ""
)

# Configuracion
$BasePackage = "com.jnzader.apigen"
$BasePath = "src/main/java/com/jnzader/apigen"
$TestBasePath = "src/test/java/com/jnzader/apigen"
$MigrationPath = "src/main/resources/db/migration"

# Funciones de utilidad
function To-CamelCase($str) {
    return $str.Substring(0,1).ToLower() + $str.Substring(1)
}

function To-SnakeCase($str) {
    return ($str -creplace '([A-Z])', '_$1').ToLower().TrimStart('_')
}

function To-KebabCase($str) {
    return ($str -creplace '([A-Z])', '-$1').ToLower().TrimStart('-')
}

function Get-JavaType($type) {
    switch ($type.ToLower()) {
        "string"   { return "String" }
        "int"      { return "Integer" }
        "integer"  { return "Integer" }
        "long"     { return "Long" }
        "decimal"  { return "BigDecimal" }
        "double"   { return "Double" }
        "float"    { return "Float" }
        "boolean"  { return "Boolean" }
        "bool"     { return "Boolean" }
        "date"     { return "LocalDate" }
        "datetime" { return "LocalDateTime" }
        "instant"  { return "Instant" }
        default    { return $type }
    }
}

function Get-SqlType($type) {
    switch ($type.ToLower()) {
        "string"   { return "VARCHAR(255)" }
        "int"      { return "INTEGER" }
        "integer"  { return "INTEGER" }
        "long"     { return "BIGINT" }
        "decimal"  { return "DECIMAL(10, 2)" }
        "double"   { return "DOUBLE PRECISION" }
        "float"    { return "REAL" }
        "boolean"  { return "BOOLEAN" }
        "bool"     { return "BOOLEAN" }
        "date"     { return "DATE" }
        "datetime" { return "TIMESTAMP" }
        "instant"  { return "TIMESTAMP" }
        default    { return "VARCHAR(255)" }
    }
}

# Parsear campos
$ParsedFields = @()
if ($Fields -ne "") {
    $Fields.Split(",") | ForEach-Object {
        $parts = $_.Trim().Split(":")
        if ($parts.Length -eq 2) {
            $ParsedFields += @{
                Name = $parts[0].Trim()
                Type = $parts[1].Trim()
                JavaType = Get-JavaType $parts[1].Trim()
                SqlType = Get-SqlType $parts[1].Trim()
                SnakeName = To-SnakeCase $parts[0].Trim()
            }
        }
    }
}

# Variables derivadas
$entityNameLower = To-CamelCase $EntityName
$entityNameSnake = To-SnakeCase $EntityName
$tableName = "${entityNameSnake}s"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  APiGen - Generador de Entidades" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Entidad: $EntityName" -ForegroundColor Yellow
Write-Host "Modulo:  $ModuleName" -ForegroundColor Yellow
Write-Host "Tabla:   $tableName" -ForegroundColor Yellow
Write-Host ""

# Crear directorios
$directories = @(
    "$BasePath/$ModuleName/domain/entity",
    "$BasePath/$ModuleName/domain/event",
    "$BasePath/$ModuleName/domain/exception",
    "$BasePath/$ModuleName/application/dto",
    "$BasePath/$ModuleName/application/mapper",
    "$BasePath/$ModuleName/application/service",
    "$BasePath/$ModuleName/infrastructure/repository",
    "$BasePath/$ModuleName/infrastructure/controller",
    "$TestBasePath/$ModuleName/application/service",
    "$TestBasePath/$ModuleName/infrastructure/controller"
)

foreach ($dir in $directories) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "[+] Directorio: $dir" -ForegroundColor Green
    }
}

# Generar imports para campos
$fieldImports = @()
$ParsedFields | ForEach-Object {
    switch ($_.JavaType) {
        "BigDecimal"    { $fieldImports += "import java.math.BigDecimal;" }
        "LocalDate"     { $fieldImports += "import java.time.LocalDate;" }
        "LocalDateTime" { $fieldImports += "import java.time.LocalDateTime;" }
        "Instant"       { $fieldImports += "import java.time.Instant;" }
    }
}
$fieldImports = $fieldImports | Select-Object -Unique

# ============================================================
# 1. ENTITY
# ============================================================
$entityFields = ""
$ParsedFields | ForEach-Object {
    $entityFields += @"

    @Column(name = "$($_.SnakeName)")
    private $($_.JavaType) $($_.Name);
"@
}

$entityContent = @"
package ${BasePackage}.${ModuleName}.domain.entity;

import ${BasePackage}.core.domain.entity.Base;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.envers.Audited;
$($fieldImports -join "`n")

@Entity
@Table(name = "$tableName")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class $EntityName extends Base {
$entityFields
}
"@

$entityPath = "$BasePath/$ModuleName/domain/entity/${EntityName}.java"
Set-Content -Path $entityPath -Value $entityContent -Encoding UTF8
Write-Host "[+] Entity: $entityPath" -ForegroundColor Green

# ============================================================
# 2. DTO
# ============================================================
$dtoFields = ""
$ParsedFields | ForEach-Object {
    $dtoFields += @"

    private $($_.JavaType) $($_.Name);
"@
}

$dtoContent = @"
package ${BasePackage}.${ModuleName}.application.dto;

import ${BasePackage}.core.application.dto.BaseDTO;
import ${BasePackage}.core.application.dto.validation.OnCreate;
import ${BasePackage}.core.application.dto.validation.OnUpdate;
import jakarta.validation.constraints.*;
import lombok.*;
$($fieldImports -join "`n")

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ${EntityName}DTO extends BaseDTO {
$dtoFields
}
"@

$dtoPath = "$BasePath/$ModuleName/application/dto/${EntityName}DTO.java"
Set-Content -Path $dtoPath -Value $dtoContent -Encoding UTF8
Write-Host "[+] DTO: $dtoPath" -ForegroundColor Green

# ============================================================
# 3. MAPPER
# ============================================================
$mapperContent = @"
package ${BasePackage}.${ModuleName}.application.mapper;

import ${BasePackage}.core.application.mapper.BaseMapper;
import ${BasePackage}.${ModuleName}.application.dto.${EntityName}DTO;
import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ${EntityName}Mapper extends BaseMapper<${EntityName}, ${EntityName}DTO> {

    @Override
    ${EntityName}DTO toDto(${EntityName} entity);

    @Override
    ${EntityName} toEntity(${EntityName}DTO dto);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(${EntityName}DTO dto, @MappingTarget ${EntityName} entity);
}
"@

$mapperPath = "$BasePath/$ModuleName/application/mapper/${EntityName}Mapper.java"
Set-Content -Path $mapperPath -Value $mapperContent -Encoding UTF8
Write-Host "[+] Mapper: $mapperPath" -ForegroundColor Green

# ============================================================
# 4. REPOSITORY
# ============================================================
$repositoryContent = @"
package ${BasePackage}.${ModuleName}.infrastructure.repository;

import ${BasePackage}.core.infrastructure.repository.BaseRepository;
import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};
import org.springframework.stereotype.Repository;

@Repository
public interface ${EntityName}Repository extends BaseRepository<${EntityName}, Long> {

    // Agrega metodos de consulta personalizados aqui
    // Ejemplo: Optional<${EntityName}> findByName(String name);
}
"@

$repositoryPath = "$BasePath/$ModuleName/infrastructure/repository/${EntityName}Repository.java"
Set-Content -Path $repositoryPath -Value $repositoryContent -Encoding UTF8
Write-Host "[+] Repository: $repositoryPath" -ForegroundColor Green

# ============================================================
# 5. SERVICE INTERFACE
# ============================================================
$serviceContent = @"
package ${BasePackage}.${ModuleName}.application.service;

import ${BasePackage}.core.application.service.BaseService;
import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};

public interface ${EntityName}Service extends BaseService<${EntityName}, Long> {

    // Agrega metodos de negocio personalizados aqui
}
"@

$servicePath = "$BasePath/$ModuleName/application/service/${EntityName}Service.java"
Set-Content -Path $servicePath -Value $serviceContent -Encoding UTF8
Write-Host "[+] Service Interface: $servicePath" -ForegroundColor Green

# ============================================================
# 6. SERVICE IMPLEMENTATION
# ============================================================
$serviceImplContent = @"
package ${BasePackage}.${ModuleName}.application.service;

import ${BasePackage}.core.application.service.BaseServiceImpl;
import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};
import ${BasePackage}.${ModuleName}.infrastructure.repository.${EntityName}Repository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class ${EntityName}ServiceImpl
        extends BaseServiceImpl<${EntityName}, Long>
        implements ${EntityName}Service {

    private final ${EntityName}Repository ${entityNameLower}Repository;

    public ${EntityName}ServiceImpl(${EntityName}Repository repository) {
        super(repository);
        this.${entityNameLower}Repository = repository;
    }

    @Override
    protected String getEntityName() {
        return "${EntityName}";
    }

    // Implementa metodos de negocio personalizados aqui
}
"@

$serviceImplPath = "$BasePath/$ModuleName/application/service/${EntityName}ServiceImpl.java"
Set-Content -Path $serviceImplPath -Value $serviceImplContent -Encoding UTF8
Write-Host "[+] Service Impl: $serviceImplPath" -ForegroundColor Green

# ============================================================
# 7. CONTROLLER INTERFACE
# ============================================================
$controllerContent = @"
package ${BasePackage}.${ModuleName}.infrastructure.controller;

import ${BasePackage}.core.infrastructure.controller.BaseController;
import ${BasePackage}.${ModuleName}.application.dto.${EntityName}DTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "${EntityName}s", description = "API de gestion de ${entityNameLower}s")
@RequestMapping("/api/v1/${ModuleName}")
public interface ${EntityName}Controller extends BaseController<${EntityName}DTO, Long> {

    // Agrega endpoints personalizados aqui
}
"@

$controllerPath = "$BasePath/$ModuleName/infrastructure/controller/${EntityName}Controller.java"
Set-Content -Path $controllerPath -Value $controllerContent -Encoding UTF8
Write-Host "[+] Controller Interface: $controllerPath" -ForegroundColor Green

# ============================================================
# 8. CONTROLLER IMPLEMENTATION
# ============================================================
$controllerImplContent = @"
package ${BasePackage}.${ModuleName}.infrastructure.controller;

import ${BasePackage}.core.infrastructure.controller.BaseControllerImpl;
import ${BasePackage}.${ModuleName}.application.dto.${EntityName}DTO;
import ${BasePackage}.${ModuleName}.application.mapper.${EntityName}Mapper;
import ${BasePackage}.${ModuleName}.application.service.${EntityName}Service;
import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ${EntityName}ControllerImpl
        extends BaseControllerImpl<${EntityName}, ${EntityName}DTO, Long>
        implements ${EntityName}Controller {

    private final ${EntityName}Service ${entityNameLower}Service;
    private final ${EntityName}Mapper ${entityNameLower}Mapper;

    public ${EntityName}ControllerImpl(${EntityName}Service service, ${EntityName}Mapper mapper) {
        super(service, mapper);
        this.${entityNameLower}Service = service;
        this.${entityNameLower}Mapper = mapper;
    }

    // Implementa endpoints personalizados aqui
}
"@

$controllerImplPath = "$BasePath/$ModuleName/infrastructure/controller/${EntityName}ControllerImpl.java"
Set-Content -Path $controllerImplPath -Value $controllerImplContent -Encoding UTF8
Write-Host "[+] Controller Impl: $controllerImplPath" -ForegroundColor Green

# ============================================================
# 9. SQL MIGRATION
# ============================================================
# Obtener el siguiente numero de migracion
$existingMigrations = Get-ChildItem -Path $MigrationPath -Filter "V*.sql" -ErrorAction SilentlyContinue
$nextVersion = 2
if ($existingMigrations) {
    $versions = $existingMigrations | ForEach-Object {
        if ($_.Name -match "^V(\d+)") { [int]$Matches[1] }
    }
    $nextVersion = ($versions | Measure-Object -Maximum).Maximum + 1
}

$sqlFields = ""
$ParsedFields | ForEach-Object {
    $sqlFields += "    $($_.SnakeName) $($_.SqlType),`n"
}

$migrationContent = @"
-- V${nextVersion}__create_${tableName}_table.sql
-- Migracion generada automaticamente para: ${EntityName}

CREATE TABLE ${tableName} (
    id BIGINT PRIMARY KEY,
$sqlFields
    -- Campos heredados de Base
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Tabla de auditoria (Hibernate Envers)
CREATE TABLE ${tableName}_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
$sqlFields
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indices
CREATE INDEX idx_${tableName}_estado ON ${tableName}(estado);
CREATE INDEX idx_${tableName}_fecha_creacion ON ${tableName}(fecha_creacion DESC);

COMMENT ON TABLE ${tableName} IS 'Tabla de ${EntityName}s';
"@

$migrationPath = "$MigrationPath/V${nextVersion}__create_${tableName}_table.sql"
Set-Content -Path $migrationPath -Value $migrationContent -Encoding UTF8
Write-Host "[+] Migration: $migrationPath" -ForegroundColor Green

# ============================================================
# 10. UNIT TEST
# ============================================================
$testContent = @"
package ${BasePackage}.${ModuleName}.application.service;

import ${BasePackage}.${ModuleName}.domain.entity.${EntityName};
import ${BasePackage}.${ModuleName}.infrastructure.repository.${EntityName}Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("${EntityName}Service Tests")
class ${EntityName}ServiceImplTest {

    @Mock
    private ${EntityName}Repository repository;

    @InjectMocks
    private ${EntityName}ServiceImpl service;

    private ${EntityName} ${entityNameLower};

    @BeforeEach
    void setUp() {
        ${entityNameLower} = new ${EntityName}();
        ${entityNameLower}.setId(1L);
        // Configura campos adicionales aqui
    }

    @Test
    @DisplayName("Debe encontrar ${entityNameLower} por ID")
    void shouldFind${EntityName}ById() {
        // given
        when(repository.findById(1L)).thenReturn(Optional.of(${entityNameLower}));

        // when
        var result = service.findById(1L);

        // then
        assertThat(result.isSuccess()).isTrue();
        result.ifSuccess(found -> {
            assertThat(found.getId()).isEqualTo(1L);
        });
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Debe guardar ${entityNameLower}")
    void shouldSave${EntityName}() {
        // given
        when(repository.save(any(${EntityName}.class))).thenReturn(${entityNameLower});

        // when
        var result = service.save(${entityNameLower});

        // then
        assertThat(result.isSuccess()).isTrue();
        verify(repository).save(any(${EntityName}.class));
    }

    @Test
    @DisplayName("Debe hacer soft delete de ${entityNameLower}")
    void shouldSoftDelete${EntityName}() {
        // given
        when(repository.findById(1L)).thenReturn(Optional.of(${entityNameLower}));
        when(repository.save(any(${EntityName}.class))).thenReturn(${entityNameLower});

        // when
        var result = service.softDelete(1L);

        // then
        assertThat(result.isSuccess()).isTrue();
    }
}
"@

$testPath = "$TestBasePath/$ModuleName/application/service/${EntityName}ServiceImplTest.java"
Set-Content -Path $testPath -Value $testContent -Encoding UTF8
Write-Host "[+] Test: $testPath" -ForegroundColor Green

# ============================================================
# RESUMEN
# ============================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Generacion completada!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Archivos creados:" -ForegroundColor Yellow
Write-Host "  - Entity:          ${EntityName}.java"
Write-Host "  - DTO:             ${EntityName}DTO.java"
Write-Host "  - Mapper:          ${EntityName}Mapper.java"
Write-Host "  - Repository:      ${EntityName}Repository.java"
Write-Host "  - Service:         ${EntityName}Service.java"
Write-Host "  - Service Impl:    ${EntityName}ServiceImpl.java"
Write-Host "  - Controller:      ${EntityName}Controller.java"
Write-Host "  - Controller Impl: ${EntityName}ControllerImpl.java"
Write-Host "  - Migration:       V${nextVersion}__create_${tableName}_table.sql"
Write-Host "  - Test:            ${EntityName}ServiceImplTest.java"
Write-Host ""
Write-Host "Proximos pasos:" -ForegroundColor Yellow
Write-Host "  1. Revisa y ajusta los campos en la Entity y DTO"
Write-Host "  2. Agrega validaciones (@NotNull, @Size, etc.) en el DTO"
Write-Host "  3. Personaliza la migracion SQL si es necesario"
Write-Host "  4. Ejecuta: ./gradlew compileJava"
Write-Host "  5. Ejecuta: ./gradlew flywayMigrate (o inicia la app)"
Write-Host ""
Write-Host "Endpoint disponible en: /api/v1/${ModuleName}" -ForegroundColor Cyan
Write-Host ""
