-- ==========================================
-- APiGen Example - Products Table
-- ==========================================
-- Example domain entity demonstrating APiGen usage.
-- ==========================================

CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY DEFAULT nextval('base_sequence'),

    -- Domain fields
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    category VARCHAR(100),
    sku VARCHAR(50) UNIQUE,

    -- Base entity fields (soft delete & auditing)
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_eliminacion TIMESTAMP,
    eliminado_por VARCHAR(100),
    creado_por VARCHAR(100),
    modificado_por VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

-- ==========================================
-- INDEXES
-- ==========================================

-- Apply standard base entity indexes
SELECT create_base_indexes('products');

-- Domain-specific indexes
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category) WHERE category IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku) WHERE sku IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);

-- ==========================================
-- SAMPLE DATA (optional)
-- ==========================================

INSERT INTO products (name, description, price, stock, category, sku) VALUES
    ('Laptop Pro 15', 'High-performance laptop with 16GB RAM', 1299.99, 50, 'Electronics', 'LP15-001'),
    ('Wireless Mouse', 'Ergonomic wireless mouse', 29.99, 200, 'Accessories', 'WM-002'),
    ('USB-C Hub', '7-in-1 USB-C hub with HDMI', 49.99, 150, 'Accessories', 'UCH-003'),
    ('Mechanical Keyboard', 'RGB mechanical keyboard', 89.99, 100, 'Accessories', 'MK-004'),
    ('Monitor 27"', '4K IPS monitor', 449.99, 30, 'Electronics', 'M27-005')
ON CONFLICT DO NOTHING;

SELECT 'Products table created successfully' AS status;
