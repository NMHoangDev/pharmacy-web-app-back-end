CREATE TABLE IF NOT EXISTS pharmacists (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(32) UNIQUE,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    avatar_url TEXT,
    specialty VARCHAR(64),
    experience_years INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    verified TINYINT(1) NOT NULL DEFAULT 0,
    availability VARCHAR(128),
    rating DECIMAL(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    bio TEXT,
    education TEXT,
    languages_json JSON,
    working_days_json JSON,
    working_hours VARCHAR(64),
    consultation_modes_json JSON,
    license_number VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pharmacist_shifts (
    id CHAR(36) PRIMARY KEY,
    pharmacist_id CHAR(36) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pharmacist_shift FOREIGN KEY (pharmacist_id) REFERENCES pharmacists(id) ON DELETE CASCADE
);

CREATE INDEX idx_pharmacists_specialty ON pharmacists(specialty);
CREATE INDEX idx_pharmacists_status ON pharmacists(status);
CREATE INDEX idx_pharmacist_shifts_pharmacist ON pharmacist_shifts(pharmacist_id);

CREATE TABLE IF NOT EXISTS offline_orders (
    id CHAR(36) PRIMARY KEY,
    order_code VARCHAR(32) NOT NULL UNIQUE,
    order_type VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    branch_id CHAR(36) NOT NULL,
    pharmacist_id CHAR(36) NOT NULL,
    customer_name VARCHAR(128),
    customer_phone VARCHAR(32),
    consultation_id VARCHAR(64),
    note VARCHAR(512),
    subtotal BIGINT NOT NULL,
    discount BIGINT NOT NULL DEFAULT 0,
    tax_fee BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
    payment_method VARCHAR(24) NULL,
    amount_received BIGINT NULL,
    change_amount BIGINT NULL,
    transfer_reference VARCHAR(128) NULL,
    payment_proof_url TEXT NULL,
    inventory_reservation_id CHAR(36) NULL,
    paid_at TIMESTAMP NULL,
    refunded_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS offline_order_items (
    id CHAR(36) PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    sku VARCHAR(64) NULL,
    product_name VARCHAR(256) NULL,
    batch_no VARCHAR(64) NULL,
    expiry_date DATE NULL,
    quantity INT NOT NULL,
    unit_price BIGINT NOT NULL,
    line_total BIGINT NOT NULL,
    CONSTRAINT fk_offline_order_item_order FOREIGN KEY (order_id) REFERENCES offline_orders(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS offline_order_payments (
    id CHAR(36) PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    event_type VARCHAR(16) NOT NULL,
    method VARCHAR(24) NOT NULL,
    amount BIGINT NOT NULL,
    amount_received BIGINT NULL,
    change_amount BIGINT NULL,
    transfer_reference VARCHAR(128) NULL,
    proof_url TEXT NULL,
    note VARCHAR(512) NULL,
    created_by CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offline_order_payment_order FOREIGN KEY (order_id) REFERENCES offline_orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_offline_order_branch ON offline_orders(branch_id);
CREATE INDEX idx_offline_order_pharmacist ON offline_orders(pharmacist_id);
CREATE INDEX idx_offline_order_status ON offline_orders(status);
CREATE INDEX idx_offline_order_created ON offline_orders(created_at);
CREATE INDEX idx_offline_item_order ON offline_order_items(order_id);
CREATE INDEX idx_offline_item_product ON offline_order_items(product_id);
CREATE INDEX idx_offline_payment_order ON offline_order_payments(order_id);
CREATE INDEX idx_offline_payment_created ON offline_order_payments(created_at);
