-- discount-service schema

CREATE TABLE IF NOT EXISTS discounts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  code VARCHAR(64) NOT NULL,
  type VARCHAR(16) NOT NULL,
  value DECIMAL(12,2) NOT NULL,
  max_discount DECIMAL(12,2) NULL,
  min_order_value DECIMAL(12,2) NULL,
  usage_limit INT NULL,
  usage_per_user INT NULL,
  used_count INT NOT NULL DEFAULT 0,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  version BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_discounts_code (code),
  KEY idx_discounts_status (status),
  KEY idx_discounts_date (start_date, end_date)
);

CREATE TABLE IF NOT EXISTS discount_scopes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  discount_id BIGINT NOT NULL,
  scope_type VARCHAR(16) NOT NULL,
  scope_id BIGINT NULL,
  KEY idx_discount_scopes_discount (discount_id),
  KEY idx_discount_scopes_type_id (scope_type, scope_id),
  CONSTRAINT fk_discount_scopes_discount
    FOREIGN KEY (discount_id) REFERENCES discounts(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS discount_usages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  discount_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_discount_usages_discount (discount_id),
  KEY idx_discount_usages_user (user_id),
  UNIQUE KEY uk_discount_usages_discount_user_order (discount_id, user_id, order_id),
  CONSTRAINT fk_discount_usages_discount
    FOREIGN KEY (discount_id) REFERENCES discounts(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS discount_user_targets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  discount_id BIGINT NOT NULL,
  user_id VARCHAR(128) NOT NULL,
  KEY idx_discount_user_targets_discount (discount_id),
  KEY idx_discount_user_targets_user (user_id),
  UNIQUE KEY uk_discount_user_targets_discount_user (discount_id, user_id),
  CONSTRAINT fk_discount_user_targets_discount
    FOREIGN KEY (discount_id) REFERENCES discounts(id)
    ON DELETE CASCADE
);
