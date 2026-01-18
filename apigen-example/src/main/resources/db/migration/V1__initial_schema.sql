-- ==========================================
-- APiGen - Migración Inicial Unificada
-- ==========================================
-- Esta migración crea todo el esquema necesario:
-- 1. Secuencia para IDs
-- 2. Funciones auxiliares
-- 3. Tablas de seguridad (usuarios, roles, permisos)
-- 4. Índices optimizados
-- 5. Tabla de auditoría (Hibernate Envers)
-- ==========================================

-- ==========================================
-- 1. SECUENCIA PARA GENERACIÓN DE IDS
-- ==========================================
CREATE SEQUENCE IF NOT EXISTS base_sequence
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 50;

-- ==========================================
-- 2. FUNCIONES AUXILIARES
-- ==========================================

-- Función para crear índices estándar en tablas que heredan de Base
CREATE OR REPLACE FUNCTION create_base_indexes(table_name TEXT)
RETURNS VOID AS $$
BEGIN
    -- Índice parcial para entidades activas (más eficiente)
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_estado ON %I(estado) WHERE estado = true',
                   table_name, table_name);

    -- Índice para ordenamiento por fecha de creación
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_fecha_creacion ON %I(fecha_creacion DESC)',
                   table_name, table_name);

    -- Índice compuesto para consultas frecuentes: activos ordenados por fecha
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_estado_fecha ON %I(estado, fecha_creacion DESC)',
                   table_name, table_name);

    -- Índice para auditoría por usuario
    EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_creado_por ON %I(creado_por) WHERE creado_por IS NOT NULL',
                   table_name, table_name);

    RAISE NOTICE 'Índices base creados para tabla: %', table_name;
END;
$$ LANGUAGE plpgsql;

-- Función para mantenimiento de tablas
CREATE OR REPLACE FUNCTION maintenance_analyze_tables()
RETURNS VOID AS $$
DECLARE
    tbl RECORD;
BEGIN
    FOR tbl IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format('ANALYZE %I.%I', tbl.schemaname, tbl.tablename);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ==========================================
-- 3. TABLAS DE SEGURIDAD
-- ==========================================

-- Tabla de permisos
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    category VARCHAR(50),

    -- Campos de Base entity
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Tabla de roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),

    -- Campos de Base entity
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Tabla de relación role-permissions (muchos a muchos)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role_id BIGINT NOT NULL,

    -- Campos específicos de Spring Security
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),

    -- Campos de Base entity
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Tabla para blacklist de tokens JWT
CREATE TABLE IF NOT EXISTS token_blacklist (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),
    token_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL,
    expiration TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(50) DEFAULT 'LOGOUT'
);

-- ==========================================
-- 4. TABLA DE AUDITORÍA (HIBERNATE ENVERS)
-- ==========================================
CREATE TABLE IF NOT EXISTS revision_info (
    id SERIAL PRIMARY KEY,
    revision_date TIMESTAMP NOT NULL,
    username VARCHAR(100)
);

-- ==========================================
-- 5. ÍNDICES OPTIMIZADOS
-- ==========================================

-- Índices para usuarios
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_auth ON users(username, estado, enabled)
    WHERE estado = true AND enabled = true;
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role_id);

-- Índices para roles
CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name);

-- Índices para permisos
CREATE INDEX IF NOT EXISTS idx_permissions_name ON permissions(name);
CREATE INDEX IF NOT EXISTS idx_permissions_category ON permissions(category) WHERE category IS NOT NULL;

-- Índices para role_permissions (ya tiene PK compuesto, agregar inverso)
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions(permission_id);

-- Índices para token blacklist
CREATE INDEX IF NOT EXISTS idx_token_blacklist_expiration ON token_blacklist(expiration);
CREATE INDEX IF NOT EXISTS idx_token_blacklist_username ON token_blacklist(username);

-- Índices para revision_info
CREATE INDEX IF NOT EXISTS idx_revision_info_date ON revision_info(revision_date DESC);
CREATE INDEX IF NOT EXISTS idx_revision_info_username ON revision_info(username);

-- Aplicar índices base a tablas de seguridad
SELECT create_base_indexes('users');
SELECT create_base_indexes('roles');
SELECT create_base_indexes('permissions');

-- ==========================================
-- 6. DATOS INICIALES
-- ==========================================

-- Permisos CRUD básicos
INSERT INTO permissions (name, description, category) VALUES
    ('READ', 'Permiso de lectura', 'CRUD'),
    ('CREATE', 'Permiso de creación', 'CRUD'),
    ('UPDATE', 'Permiso de actualización', 'CRUD'),
    ('DELETE', 'Permiso de eliminación', 'CRUD')
ON CONFLICT (name) DO NOTHING;

-- Roles básicos
INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Administrador con acceso total'),
    ('USER', 'Usuario estándar con acceso limitado'),
    ('GUEST', 'Usuario invitado con acceso de solo lectura')
ON CONFLICT (name) DO NOTHING;

-- Asignar permisos a roles
-- ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- USER: READ, CREATE, UPDATE (no DELETE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN ('READ', 'CREATE', 'UPDATE')
ON CONFLICT DO NOTHING;

-- GUEST: solo READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'GUEST' AND p.name = 'READ'
ON CONFLICT DO NOTHING;

-- ==========================================
-- NOTA: No se crea usuario admin por defecto
-- El primer usuario admin debe crearse manualmente
-- o mediante un script de inicialización seguro
-- ==========================================

-- Limpieza automática de tokens expirados (función)
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM token_blacklist WHERE expiration < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_tokens() IS
    'Elimina tokens expirados del blacklist. Ejecutar periódicamente: SELECT cleanup_expired_tokens();';

-- ==========================================
-- FINALIZACIÓN
-- ==========================================
SELECT 'APiGen initial schema created successfully' AS status;
