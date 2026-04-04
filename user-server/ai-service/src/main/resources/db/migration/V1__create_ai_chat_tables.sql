CREATE TABLE IF NOT EXISTS chat_conversations (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NULL,
    title VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_chat_conversations_user_id (user_id),
    INDEX idx_chat_conversations_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    id CHAR(36) NOT NULL PRIMARY KEY,
    conversation_id CHAR(36) NOT NULL,
    role_name VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    sources_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chat_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id)
        ON DELETE CASCADE,
    INDEX idx_chat_messages_conversation_created (conversation_id, created_at)
);
