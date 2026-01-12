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
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_reviews_product (product_id),
  INDEX idx_reviews_user (user_id)
);
