SET @db := DATABASE();

SELECT @db AS using_db;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'fulfillment_branch_id'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `fulfillment_branch_id` CHAR(36) NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'fulfillment_assigned_by'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `fulfillment_assigned_by` CHAR(36) NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'fulfillment_assigned_at'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `fulfillment_assigned_at` DATETIME NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'fulfillment_status'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `fulfillment_status` VARCHAR(32) NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'inventory_reservation_id'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `inventory_reservation_id` CHAR(36) NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND INDEX_NAME = 'idx_orders_fulfillment_branch_id'
);

SET @sql := IF(
  @idx_exists = 0,
  CONCAT('CREATE INDEX `idx_orders_fulfillment_branch_id` ON `', @db, '`.`orders` (`fulfillment_branch_id`)'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
