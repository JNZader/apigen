-- Auto-generated migration for: Order

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    order_number VARCHAR (255) NOT NULL,
    total DECIMAL (19, 2) NOT NULL,
    status VARCHAR (255) NOT NULL,
    shipping_address VARCHAR (255) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    customer_id BIGINT,
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
CREATE TABLE orders_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    order_number VARCHAR (255),
    total DECIMAL (19, 2),
    status VARCHAR (255),
    shipping_address VARCHAR (255),
    order_date TIMESTAMP,
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_orders_estado ON orders(estado);
CREATE INDEX idx_orders_fecha_creacion ON orders(fecha_creacion DESC);
CREATE INDEX idx_orders_customer ON orders(customer_id);

-- Foreign Key Constraints
ALTER TABLE orders ADD CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
