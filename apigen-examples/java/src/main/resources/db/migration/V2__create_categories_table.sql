-- Auto-generated migration for: Category

CREATE TABLE categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR (255) NOT NULL,
    description VARCHAR (255),
    slug VARCHAR (255) NOT NULL,
    -- Base entity fields
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- Audit table (Hibernate Envers)
CREATE TABLE categories_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    name VARCHAR (255),
    description VARCHAR (255),
    slug VARCHAR (255),
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_categories_estado ON categories(estado);
CREATE INDEX idx_categories_fecha_creacion ON categories(fecha_creacion DESC);

