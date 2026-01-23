-- Auto-generated migration for: Customer

CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    email VARCHAR (255) NOT NULL,
    first_name VARCHAR (255) NOT NULL,
    last_name VARCHAR (255) NOT NULL,
    phone VARCHAR (255),
    address VARCHAR (255),
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
CREATE TABLE customers_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    email VARCHAR (255),
    first_name VARCHAR (255),
    last_name VARCHAR (255),
    phone VARCHAR (255),
    address VARCHAR (255),
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_customers_estado ON customers(estado);
CREATE INDEX idx_customers_fecha_creacion ON customers(fecha_creacion DESC);

