-- Auto-generated migration for: Product

CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR (255) NOT NULL,
    description VARCHAR (255),
    price DECIMAL (19, 2) NOT NULL,
    stock INTEGER NOT NULL,
    sku VARCHAR (255) NOT NULL,
    image_url VARCHAR (255),
    category_id BIGINT,
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
CREATE TABLE products_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    name VARCHAR (255),
    description VARCHAR (255),
    price DECIMAL (19, 2),
    stock INTEGER,
    sku VARCHAR (255),
    image_url VARCHAR (255),
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_products_estado ON products(estado);
CREATE INDEX idx_products_fecha_creacion ON products(fecha_creacion DESC);
CREATE INDEX idx_products_category ON products(category_id);

-- Foreign Key Constraints
ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id);
