-- Auto-generated migration for: Review

CREATE TABLE reviews (
    id BIGINT PRIMARY KEY,
    rating INTEGER NOT NULL,
    comment VARCHAR (255),
    review_date TIMESTAMP NOT NULL,
    product_id BIGINT,
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
CREATE TABLE reviews_aud (
    id BIGINT NOT NULL,
    rev INTEGER NOT NULL REFERENCES revision_info(id),
    revtype SMALLINT,
    rating INTEGER,
    comment VARCHAR (255),
    review_date TIMESTAMP,
    estado BOOLEAN,
    fecha_eliminacion TIMESTAMP,
    modificado_por VARCHAR(100),
    eliminado_por VARCHAR(100),
    PRIMARY KEY (id, rev)
);

-- Indexes
CREATE INDEX idx_reviews_estado ON reviews(estado);
CREATE INDEX idx_reviews_fecha_creacion ON reviews(fecha_creacion DESC);
CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);

-- Foreign Key Constraints
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id);
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
