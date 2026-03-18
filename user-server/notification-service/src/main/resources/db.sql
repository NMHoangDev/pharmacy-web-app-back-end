-- notification_db schema (MySQL 8.x)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS notification_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE notification_db;

CREATE TABLE IF NOT EXISTS notifications (
    id CHAR(36) NOT NULL,
    recipient_user_id CHAR(36) NULL,
    audience VARCHAR(16) NOT NULL,
    target_role VARCHAR(32) NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    source_type VARCHAR(64) NULL,
    source_id VARCHAR(128) NULL,
    source_event_type VARCHAR(128) NULL,
    action_url VARCHAR(512) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_created (recipient_user_id, created_at),
    KEY idx_notifications_audience_role_created (audience, target_role, created_at),
    KEY idx_notifications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_receipts (
    id CHAR(36) NOT NULL,
    notification_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    read_at TIMESTAMP(6) NOT NULL,
  deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_receipt_user_notification UNIQUE (user_id, notification_id),
    CONSTRAINT fk_notification_receipts_notification
      FOREIGN KEY (notification_id) REFERENCES notifications(id)
      ON DELETE CASCADE ON UPDATE CASCADE,
    KEY idx_notification_receipts_user (user_id),
    KEY idx_notification_receipts_notification (notification_id),
    KEY idx_notification_receipts_read_at (read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
