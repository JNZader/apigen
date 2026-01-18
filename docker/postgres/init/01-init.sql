-- ==========================================
-- PostgreSQL Initialization Script
-- ==========================================
-- Este script se ejecuta automáticamente al crear el contenedor
-- Solo se ejecuta si la base de datos no existe

-- Crear extensiones útiles
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE 'APiGen database initialized successfully';
END $$;
