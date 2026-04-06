CREATE DATABASE IF NOT EXISTS appointment_db;
USE appointment_db;

CREATE TABLE IF NOT EXISTS appointments (
  id CHAR(36) NOT NULL,
  user_id CHAR(36) NOT NULL,
  pharmacist_id CHAR(36) NOT NULL,
  start_at DATETIME NOT NULL,
  end_at DATETIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
  channel VARCHAR(32) NOT NULL DEFAULT 'VIDEO',
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_appointments_user (user_id),
  INDEX idx_appointments_pharmacist (pharmacist_id)
);
