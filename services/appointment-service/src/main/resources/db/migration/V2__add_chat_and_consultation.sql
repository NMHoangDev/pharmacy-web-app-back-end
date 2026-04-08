CREATE TABLE IF NOT EXISTS consultation_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    appointment_id CHAR(36) NOT NULL,
    room_id VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_by CHAR(36) NOT NULL,
    started_at DATETIME NULL,
    ended_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_consultation_room (room_id),
    INDEX idx_consultation_appointment (appointment_id),
    INDEX idx_consultation_status (status),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS chat_messages (
    id CHAR(36) NOT NULL PRIMARY KEY,
    appointment_id CHAR(36) NOT NULL,
    sender_id CHAR(36) NOT NULL,
    sender_role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(16) NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_appointment_created (appointment_id, created_at),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB;
