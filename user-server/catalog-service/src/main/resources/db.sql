-- catalog_db schema (UUID as CHAR(36) for MySQL compatibility)

CREATE TABLE IF NOT EXISTS categories (
        id CHAR(36) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        slug VARCHAR(255) UNIQUE NOT NULL,
        parent_id CHAR(36),
        description TEXT,
        is_active BOOLEAN DEFAULT TRUE,
        sort_order INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS drugs (
        id CHAR(36) PRIMARY KEY,
        sku VARCHAR(255) UNIQUE NOT NULL,
        name VARCHAR(255) NOT NULL,
        slug VARCHAR(255) UNIQUE NOT NULL,
        category_id CHAR(36),
        price DECIMAL(12,2) NOT NULL,
        status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
        prescription_required BOOLEAN DEFAULT FALSE,
        description TEXT,
        image_url MEDIUMTEXT,
        attributes JSON,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
);

-- Migration helper (run once if your existing DB was created with BIGINT ids)
-- Converts numeric ids to CHAR(36) so UUID inserts won't fail.
SET @schema = DATABASE();
SELECT COUNT(*) INTO @exists
        FROM information_schema.tables
 WHERE table_schema = @schema AND table_name = 'categories';
SET @sql = IF(@exists = 1,
        'ALTER TABLE categories MODIFY COLUMN id CHAR(36)',
        'SELECT "categories table not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
        FROM information_schema.tables
 WHERE table_schema = @schema AND table_name = 'drugs';
SET @sql = IF(@exists = 1,
        'ALTER TABLE drugs MODIFY COLUMN id CHAR(36)',
        'SELECT "drugs table not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
        FROM information_schema.tables
 WHERE table_schema = @schema AND table_name = 'drugs';
SET @sql = IF(@exists = 1,
        'ALTER TABLE drugs MODIFY COLUMN category_id CHAR(36)',
        'SELECT "drugs table not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
        FROM information_schema.tables
 WHERE table_schema = @schema AND table_name = 'drugs';
SET @sql = IF(@exists = 1,
        'ALTER TABLE drugs MODIFY COLUMN image_url MEDIUMTEXT',
        'SELECT "drugs table not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
        FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'categories' AND column_name = 'parent_id';
SET @sql = IF(@exists = 0,
        'ALTER TABLE categories ADD COLUMN parent_id CHAR(36)',
        'SELECT "parent_id already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
 WHERE table_schema = @schema AND table_name = 'drugs' AND index_name = 'idx_drugs_category';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_drugs_category ON drugs(category_id)',
    'SELECT "idx_drugs_category already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
 WHERE table_schema = @schema AND table_name = 'drugs' AND index_name = 'idx_drugs_slug';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_drugs_slug ON drugs(slug)',
    'SELECT "idx_drugs_slug already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
 WHERE table_schema = @schema AND table_name = 'categories' AND index_name = 'idx_categories_parent';
SET @sql = IF(@exists = 0,
    'CREATE INDEX idx_categories_parent ON categories(parent_id)',
    'SELECT "idx_categories_parent already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
