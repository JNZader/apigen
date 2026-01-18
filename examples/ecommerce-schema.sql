-- ============================================================
-- APiGen Example Schema: E-Commerce API
-- ============================================================
-- This file demonstrates all supported SQL features:
-- - CREATE TABLE with various column types
-- - NOT NULL, UNIQUE, DEFAULT constraints
-- - PRIMARY KEY (single and composite)
-- - FOREIGN KEY relationships (inline and ALTER TABLE)
-- - Many-to-Many via junction tables
-- - Indexes (BTREE, GIN for PostgreSQL)
-- - Functions and Stored Procedures
--
-- Usage:
--   ./gradlew generateFromSql -Psql=examples/ecommerce-schema.sql
-- ============================================================

-- ====================
-- CATEGORIES
-- ====================
CREATE TABLE categories (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id BIGINT REFERENCES categories(id),
    image_url VARCHAR(500),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);

-- ====================
-- BRANDS
-- ====================
CREATE TABLE brands (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(500),
    website VARCHAR(255),
    country VARCHAR(50)
);

-- ====================
-- PRODUCTS
-- ====================
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    short_description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2),
    cost DECIMAL(10, 2),
    stock INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER DEFAULT 5,
    weight DECIMAL(8, 3),
    weight_unit VARCHAR(10) DEFAULT 'kg',
    brand_id BIGINT REFERENCES brands(id),
    category_id BIGINT REFERENCES categories(id),
    is_featured BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    meta_title VARCHAR(255),
    meta_description VARCHAR(500)
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_brand ON products(brand_id);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);

-- Full-text search index (PostgreSQL)
-- CREATE INDEX idx_products_search ON products USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '')));

-- ====================
-- PRODUCT TAGS (Many-to-Many)
-- ====================
CREATE TABLE tags (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    slug VARCHAR(50) NOT NULL UNIQUE
);

-- Junction table for Product <-> Tag (Many-to-Many)
CREATE TABLE product_tags (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, tag_id)
);

-- ====================
-- PRODUCT IMAGES
-- ====================
CREATE TABLE product_images (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_product_images_product ON product_images(product_id);

-- ====================
-- PRODUCT VARIANTS (Size, Color, etc.)
-- ====================
CREATE TABLE product_variants (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    price_adjustment DECIMAL(10, 2) DEFAULT 0,
    stock INTEGER NOT NULL DEFAULT 0,
    attributes JSONB
);

CREATE INDEX idx_product_variants_product ON product_variants(product_id);
CREATE INDEX idx_product_variants_sku ON product_variants(sku);

-- ====================
-- CUSTOMERS
-- ====================
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    date_of_birth DATE,
    accepts_marketing BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_customers_email ON customers(email);

-- ====================
-- ADDRESSES
-- ====================
CREATE TABLE addresses (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL DEFAULT 'shipping',
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    company VARCHAR(100),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    phone VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_addresses_customer ON addresses(customer_id);

-- ====================
-- ORDERS
-- ====================
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT REFERENCES customers(id),
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    subtotal DECIMAL(12, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    shipping_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    shipping_address_id BIGINT REFERENCES addresses(id),
    billing_address_id BIGINT REFERENCES addresses(id),
    notes TEXT,
    placed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_placed_at ON orders(placed_at DESC);

-- ====================
-- ORDER ITEMS
-- ====================
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id),
    variant_id BIGINT REFERENCES product_variants(id),
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- ====================
-- REVIEWS
-- ====================
CREATE TABLE reviews (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    content TEXT,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT FALSE,
    helpful_count INTEGER DEFAULT 0
);

CREATE INDEX idx_reviews_product ON reviews(product_id);
CREATE INDEX idx_reviews_customer ON reviews(customer_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- ====================
-- COUPONS
-- ====================
CREATE TABLE coupons (
    id BIGINT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    discount_type VARCHAR(20) NOT NULL DEFAULT 'percentage',
    discount_value DECIMAL(10, 2) NOT NULL,
    min_purchase_amount DECIMAL(10, 2),
    max_discount_amount DECIMAL(10, 2),
    usage_limit INTEGER,
    used_count INTEGER DEFAULT 0,
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_coupons_code ON coupons(code);

-- ====================
-- WISHLISTS (Many-to-Many)
-- ====================
CREATE TABLE wishlists (
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id, product_id)
);

-- ====================
-- POSTGRESQL FUNCTIONS
-- ====================

-- Function to calculate product average rating
CREATE OR REPLACE FUNCTION get_product_rating(p_product_id BIGINT)
RETURNS DECIMAL(3, 2)
LANGUAGE sql
AS $$
    SELECT COALESCE(AVG(rating)::DECIMAL(3, 2), 0)
    FROM reviews
    WHERE product_id = p_product_id
    AND is_approved = TRUE;
$$;

-- Function to update stock after order
CREATE OR REPLACE FUNCTION update_stock_after_order(
    p_product_id BIGINT,
    p_variant_id BIGINT,
    p_quantity INTEGER
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_variant_id IS NOT NULL THEN
        UPDATE product_variants
        SET stock = stock - p_quantity
        WHERE id = p_variant_id;
    ELSE
        UPDATE products
        SET stock = stock - p_quantity
        WHERE id = p_product_id;
    END IF;
END;
$$;

-- Function to generate order number
CREATE OR REPLACE FUNCTION generate_order_number()
RETURNS VARCHAR(50)
LANGUAGE plpgsql
AS $$
DECLARE
    v_number VARCHAR(50);
BEGIN
    v_number := 'ORD-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || '-' || LPAD(nextval('order_number_seq')::TEXT, 6, '0');
    RETURN v_number;
END;
$$;

-- Procedure to process order
CREATE OR REPLACE PROCEDURE process_order(
    IN p_order_id BIGINT,
    IN p_new_status VARCHAR(30)
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE orders
    SET status = p_new_status,
        shipped_at = CASE WHEN p_new_status = 'shipped' THEN CURRENT_TIMESTAMP ELSE shipped_at END,
        delivered_at = CASE WHEN p_new_status = 'delivered' THEN CURRENT_TIMESTAMP ELSE delivered_at END,
        cancelled_at = CASE WHEN p_new_status = 'cancelled' THEN CURRENT_TIMESTAMP ELSE cancelled_at END
    WHERE id = p_order_id;

    COMMIT;
END;
$$;

-- ====================
-- COMMENTS
-- ====================
COMMENT ON TABLE products IS 'Main product catalog table';
COMMENT ON TABLE orders IS 'Customer orders';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - unique product identifier';
COMMENT ON COLUMN orders.status IS 'Order status: pending, processing, shipped, delivered, cancelled';
