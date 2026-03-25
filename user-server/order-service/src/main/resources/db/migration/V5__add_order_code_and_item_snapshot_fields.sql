SET @db := DATABASE();

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'order_code'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `order_code` VARCHAR(64) NULL AFTER `user_id`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'image_url'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `image_url` TEXT NULL AFTER `product_name`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'sku'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `sku` VARCHAR(128) NULL AFTER `image_url`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'unit'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `unit` VARCHAR(64) NULL AFTER `sku`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'category'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `category` VARCHAR(128) NULL AFTER `unit`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'type'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `type` VARCHAR(128) NULL AFTER `category`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'order_items'
    AND COLUMN_NAME = 'short_description'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`order_items` ADD COLUMN `short_description` TEXT NULL AFTER `type`'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
