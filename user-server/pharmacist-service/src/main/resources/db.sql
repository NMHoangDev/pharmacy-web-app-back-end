-- pharmacist_db schema (MySQL)
CREATE DATABASE IF NOT EXISTS pharmacist_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE pharmacist_db;

CREATE TABLE IF NOT EXISTS pharmacists (
    id CHAR(36) PRIMARY KEY,
    code VARCHAR(32) UNIQUE,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    avatar_url TEXT,
    specialty VARCHAR(64),
    experience_years INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'OFFLINE',
    verified TINYINT(1) NOT NULL DEFAULT 0,
    availability VARCHAR(128),
    rating DECIMAL(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    bio TEXT,
    education TEXT,
    languages_json JSON,
    working_days_json JSON,
    working_hours VARCHAR(64),
    consultation_modes_json JSON,
    license_number VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pharmacist_shifts (
    id CHAR(36) PRIMARY KEY,
    pharmacist_id CHAR(36) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pharmacist_shift FOREIGN KEY (pharmacist_id) REFERENCES pharmacists(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pharmacists_specialty ON pharmacists(specialty);
CREATE INDEX idx_pharmacists_status ON pharmacists(status);
CREATE INDEX idx_pharmacist_shifts_pharmacist ON pharmacist_shifts(pharmacist_id);
