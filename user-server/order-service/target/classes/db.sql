-- order_db schema (MySQL)
CREATE TABLE IF NOT EXISTS carts (
    user_id CHAR(36) PRIMARY KEY,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cart_items (
    cart_user_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (cart_user_id, product_id),
    CONSTRAINT fk_cart FOREIGN KEY (cart_user_id) REFERENCES carts(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS orders (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    status VARCHAR(24) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_outbox_status ON outbox_events(status);
