-- inventory_db schema (MySQL)
CREATE TABLE IF NOT EXISTS inventory_items (
    product_id CHAR(36) PRIMARY KEY,
    on_hand INT NOT NULL DEFAULT 0,
    reserved INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservations (
    id CHAR(36) PRIMARY KEY,
    order_id CHAR(36) NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    
);

CREATE TABLE IF NOT EXISTS reservation_lines (
    reservation_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (reservation_id, product_id),
    CONSTRAINT fk_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS inventory_activities (
    id CHAR(36) PRIMARY KEY,
    product_id CHAR(36) NOT NULL,
    type VARCHAR(16) NOT NULL,
    delta INT NOT NULL DEFAULT 0,
    on_hand_after INT NOT NULL DEFAULT 0,
    reserved_after INT NOT NULL DEFAULT 0,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_inventory_activities_product (product_id),
    INDEX idx_inventory_activities_created (created_at)
);

-- Migration helper: ensure UUID columns are CHAR(36)
SET @schema = DATABASE();

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'inventory_items' AND column_name = 'product_id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE inventory_items MODIFY COLUMN product_id CHAR(36)',
    'SELECT "inventory_items.product_id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'reservations' AND column_name = 'id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE reservations MODIFY COLUMN id CHAR(36)',
    'SELECT "reservations.id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'reservations' AND column_name = 'order_id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE reservations MODIFY COLUMN order_id CHAR(36)',
    'SELECT "reservations.order_id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'reservation_lines' AND column_name = 'reservation_id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE reservation_lines MODIFY COLUMN reservation_id CHAR(36)',
    'SELECT "reservation_lines.reservation_id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'reservation_lines' AND column_name = 'product_id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE reservation_lines MODIFY COLUMN product_id CHAR(36)',
    'SELECT "reservation_lines.product_id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'inventory_activities' AND column_name = 'id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE inventory_activities MODIFY COLUMN id CHAR(36)',
    'SELECT "inventory_activities.id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
    FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'inventory_activities' AND column_name = 'product_id';
SET @sql = IF(@exists = 1,
    'ALTER TABLE inventory_activities MODIFY COLUMN product_id CHAR(36)',
    'SELECT "inventory_activities.product_id not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

