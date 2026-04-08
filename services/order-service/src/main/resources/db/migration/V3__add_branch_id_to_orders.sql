SET @db := DATABASE();

SELECT @db AS using_db;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'branch_id'
);

SET @sql := IF(
  @col_exists = 0,
  CONCAT('ALTER TABLE `', @db, '`.`orders` ADD COLUMN `branch_id` CHAR(36) NULL'),
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
