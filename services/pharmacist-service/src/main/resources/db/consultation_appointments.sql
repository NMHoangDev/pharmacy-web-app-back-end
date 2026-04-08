-- Consultation appointments table
CREATE TABLE IF NOT EXISTS consultation_appointments (
    id CHAR(36) PRIMARY KEY,
    pharmacist_id CHAR(36) NOT NULL,
    user_id CHAR(36),
    full_name VARCHAR(255) NOT NULL,
    contact VARCHAR(255) NOT NULL,
    mode VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cancel_reason VARCHAR(255),
    UNIQUE KEY uq_consultation_pharmacist_start (pharmacist_id, start_at),
    INDEX idx_consultation_pharmacist_start (pharmacist_id, start_at),
    INDEX idx_consultation_user_created (user_id, created_at)
);
