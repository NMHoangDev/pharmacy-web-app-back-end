-- Database setup for payment-service (MySQL)
CREATE DATABASE IF NOT EXISTS payment_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE payment_db;

CREATE TABLE IF NOT EXISTS payment_transactions (
  id CHAR(36) NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  provider VARCHAR(16) NOT NULL,
  txn_ref VARCHAR(64) NOT NULL,
  amount BIGINT NOT NULL,
  currency VARCHAR(8) NOT NULL DEFAULT 'VND',
  status VARCHAR(16) NOT NULL,
  response_code VARCHAR(32) DEFAULT NULL,
  transaction_status VARCHAR(32) DEFAULT NULL,
  gateway_transaction_no VARCHAR(64) DEFAULT NULL,
  pay_date VARCHAR(32) DEFAULT NULL,
  raw_callback TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_payment_txn_ref (txn_ref),
  KEY idx_payment_order_id (order_id),
  KEY idx_payment_provider (provider)
) ENGINE=InnoDB;
