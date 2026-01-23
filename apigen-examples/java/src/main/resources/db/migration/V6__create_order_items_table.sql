-- Auto-generated migration for: OrderItem

CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL (19, 2) NOT NULL,
    order_id BIGINT,
    product_id BIGINT,
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
CREATE TABLE order_items_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    quantity INTEGER,
    unit_price DECIMAL (19, 2),
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_order_items_estado ON order_items(estado);
CREATE INDEX idx_order_items_fecha_creacion ON order_items(fecha_creacion DESC);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- Foreign Key Constraints
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id);
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id);
