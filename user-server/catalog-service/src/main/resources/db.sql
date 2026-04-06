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
        cost_price DECIMAL(12,2),
        sale_price DECIMAL(12,2) NOT NULL,
        status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
        prescription_required BOOLEAN DEFAULT FALSE,
        description TEXT,
        dosage_form TEXT,
        packaging TEXT,
        active_ingredient TEXT,
        indications TEXT,
        usage_dosage TEXT,
        contraindications_warning TEXT,
        other_information TEXT,
        image_url MEDIUMTEXT,
        attributes JSON,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS drug_branch_settings (
        id CHAR(36) PRIMARY KEY,
        drug_id CHAR(36) NOT NULL,
        branch_id CHAR(36) NOT NULL,
        price_override DECIMAL(12,2),
        status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
        note TEXT,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        UNIQUE KEY uk_drug_branch (drug_id, branch_id)
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
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'cost_price';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN cost_price DECIMAL(12,2)',
        'SELECT "cost_price already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'dosage_form';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN dosage_form TEXT',
        'SELECT "dosage_form already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'packaging';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN packaging TEXT',
        'SELECT "packaging already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'active_ingredient';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN active_ingredient TEXT',
        'SELECT "active_ingredient already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'indications';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN indications TEXT',
        'SELECT "indications already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'usage_dosage';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN usage_dosage TEXT',
        'SELECT "usage_dosage already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'contraindications_warning';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN contraindications_warning TEXT',
        'SELECT "contraindications_warning already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'other_information';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN other_information TEXT',
        'SELECT "other_information already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE drugs
   SET dosage_form = COALESCE(
        dosage_form,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.dosageForm')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.form'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET packaging = COALESCE(
        packaging,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.packaging')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.packing'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET active_ingredient = COALESCE(
        active_ingredient,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.activeIngredient')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.ingredient'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET indications = COALESCE(
        indications,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.indications')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.usage'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET usage_dosage = COALESCE(
        usage_dosage,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.usageDosage')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.dosage'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET contraindications_warning = COALESCE(
        contraindications_warning,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.contraindicationsWarning')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.contraindications')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.warning'))
   )
 WHERE attributes IS NOT NULL;

UPDATE drugs
   SET other_information = COALESCE(
        other_information,
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.otherInformation')),
        JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.extraInfo'))
   )
 WHERE attributes IS NOT NULL;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'sale_price';
SET @sql = IF(@exists = 0,
        'ALTER TABLE drugs ADD COLUMN sale_price DECIMAL(12,2)',
        'SELECT "sale_price already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'price';
SET @sql = IF(@exists = 1,
        'UPDATE drugs SET sale_price = COALESCE(sale_price, price) WHERE sale_price IS NULL',
        'SELECT "price column not found"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop legacy price column now that data is in sale_price
SELECT COUNT(*) INTO @exists
 FROM information_schema.columns
 WHERE table_schema = @schema AND table_name = 'drugs' AND column_name = 'price';
SET @sql = IF(@exists = 1,
        'ALTER TABLE drugs DROP COLUMN price',
        'SELECT "price column already removed"');
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

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
 WHERE table_schema = @schema AND table_name = 'drug_branch_settings' AND index_name = 'idx_drug_branch_drug';
SET @sql = IF(@exists = 0,
                'CREATE INDEX idx_drug_branch_drug ON drug_branch_settings(drug_id)',
                'SELECT "idx_drug_branch_drug already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists FROM information_schema.statistics
 WHERE table_schema = @schema AND table_name = 'drug_branch_settings' AND index_name = 'idx_drug_branch_branch';
SET @sql = IF(@exists = 0,
                'CREATE INDEX idx_drug_branch_branch ON drug_branch_settings(branch_id)',
                'SELECT "idx_drug_branch_branch already exists"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO drug_branch_settings (id, drug_id, branch_id, price_override, status, note, created_at, updated_at)
SELECT UUID(), d.id, '00000000-0000-0000-0000-000000000000', NULL, d.status, NULL, NOW(), NOW()
        FROM drugs d
 WHERE NOT EXISTS (
                                SELECT 1 FROM drug_branch_settings s
                                 WHERE s.drug_id = d.id AND s.branch_id = '00000000-0000-0000-0000-000000000000'
 );
