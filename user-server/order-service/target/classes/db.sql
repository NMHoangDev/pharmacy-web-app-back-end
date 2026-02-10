-- order_db schema (MySQL)
CREATE TABLE IF NOT EXISTS carts (
    user_id CHAR(36) PRIMARY KEY,
    branch_id CHAR(36) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    cart_user_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (cart_user_id, product_id),
    CONSTRAINT fk_cart FOREIGN KEY (cart_user_id) REFERENCES carts(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_shipping_address (
    id CHAR(36) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    province_name VARCHAR(255),
    province_code VARCHAR(64),
    district_name VARCHAR(255),
    district_code VARCHAR(64),
    ward_name VARCHAR(255),
    ward_code VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS order_shipping (
    id CHAR(36) PRIMARY KEY,
    method VARCHAR(64) NOT NULL,
    fee DECIMAL(12,2) NOT NULL,
    eta_range VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS order_discounts (
    id CHAR(36) PRIMARY KEY,
    promo_code VARCHAR(128),
    discount_amount DECIMAL(12,2) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    branch_id CHAR(36) NULL,
    status VARCHAR(24) NOT NULL,
    subtotal DECIMAL(12,2) NOT NULL,
    shipping_fee DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method VARCHAR(32),
    payment_status VARCHAR(32),
    note TEXT,
    shipping_address_id CHAR(36),
    shipping_id CHAR(36),
    discount_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES order_shipping_address(id),
    CONSTRAINT fk_orders_shipping FOREIGN KEY (shipping_id) REFERENCES order_shipping(id),
    CONSTRAINT fk_orders_discount FOREIGN KEY (discount_id) REFERENCES order_discounts(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    id CHAR(36) PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    transaction_ref VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id CHAR(36) PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(8) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_user_id);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_branch ON orders(branch_id);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_outbox_status ON outbox_events(status);
