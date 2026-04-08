CREATE DATABASE IF NOT EXISTS review_db;
USE review_db;

CREATE TABLE IF NOT EXISTS reviews (
  id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  rating INT NOT NULL,
  title VARCHAR(128),
  content TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  reply_content TEXT,
  replied_at TIMESTAMP NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_reviews_product (product_id),
  INDEX idx_reviews_user (user_id),
  INDEX idx_reviews_status (status)
);

-- Idempotent schema updates for existing databases
SET @reply_content_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reviews'
    AND COLUMN_NAME = 'reply_content'
);
SET @reply_content_sql := IF(@reply_content_exists = 0,
  'ALTER TABLE reviews ADD COLUMN reply_content TEXT',
  'SELECT 1'
);
PREPARE stmt FROM @reply_content_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @replied_at_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reviews'
    AND COLUMN_NAME = 'replied_at'
);
SET @replied_at_sql := IF(@replied_at_exists = 0,
  'ALTER TABLE reviews ADD COLUMN replied_at TIMESTAMP NULL',
  'SELECT 1'
);
PREPARE stmt FROM @replied_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @status_index_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'reviews'
    AND INDEX_NAME = 'idx_reviews_status'
);
SET @status_index_sql := IF(@status_index_exists = 0,
  'CREATE INDEX idx_reviews_status ON reviews(status)',
  'SELECT 1'
);
PREPARE stmt FROM @status_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS review_images (
  id CHAR(36) NOT NULL,
  review_id CHAR(36) NOT NULL,
  image_url TEXT,
  bucket VARCHAR(64),
  object_key VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_review_images_review (review_id)
);
