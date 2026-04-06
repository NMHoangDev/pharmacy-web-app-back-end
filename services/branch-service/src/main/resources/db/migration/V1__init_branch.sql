CREATE TABLE IF NOT EXISTS branches (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    address_line VARCHAR(255),
    ward VARCHAR(128),
    district VARCHAR(128),
    city VARCHAR(128),
    province VARCHAR(128),
    country VARCHAR(128),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    phone VARCHAR(64),
    email VARCHAR(128),
    timezone VARCHAR(64),
    notes TEXT,
    cover_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS branch_settings (
    branch_id CHAR(36) PRIMARY KEY,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    buffer_before_minutes INT NOT NULL DEFAULT 0,
    buffer_after_minutes INT NOT NULL DEFAULT 0,
    lead_time_minutes INT NOT NULL DEFAULT 0,
    cutoff_time TIME NULL,
    max_bookings_per_pharmacist_per_day INT DEFAULT 0,
    max_bookings_per_customer_per_week INT DEFAULT 0,
    channels_json JSON,
    pricing_json JSON,
    pickup_enabled BOOLEAN DEFAULT TRUE,
    delivery_enabled BOOLEAN DEFAULT FALSE,
    delivery_zones_json JSON,
    shipping_fee_rules_json JSON,
    default_warehouse_code VARCHAR(64),
    allow_negative_stock BOOLEAN DEFAULT FALSE,
    default_reorder_point INT DEFAULT 20,
    enable_fefo BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_branch_settings_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS branch_hours (
    branch_id CHAR(36) PRIMARY KEY,
    weekly_hours_json JSON NOT NULL,
    lunch_break_json JSON,
    CONSTRAINT fk_branch_hours_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS branch_holidays (
    id CHAR(36) PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    date DATE NOT NULL,
    type VARCHAR(32) NOT NULL,
    open_time TIME NULL,
    close_time TIME NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_branch_holidays_branch (branch_id),
    INDEX idx_branch_holidays_date (date),
    CONSTRAINT fk_branch_holidays_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS branch_staff (
    branch_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role VARCHAR(32) NOT NULL,
    skills_json JSON,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id, user_id),
    INDEX idx_branch_staff_user (user_id),
    INDEX idx_branch_staff_branch (branch_id),
    CONSTRAINT fk_branch_staff_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS branch_audit_log (
    id CHAR(36) PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    actor VARCHAR(128),
    action VARCHAR(64) NOT NULL,
    entity VARCHAR(64) NOT NULL,
    before_json JSON,
    after_json JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_branch_audit_branch (branch_id),
    INDEX idx_branch_audit_created (created_at),
    CONSTRAINT fk_branch_audit_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE CASCADE
);
