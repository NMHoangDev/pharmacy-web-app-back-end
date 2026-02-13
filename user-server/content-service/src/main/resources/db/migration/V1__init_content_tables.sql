CREATE TABLE IF NOT EXISTS users (
    id CHAR(36) PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id CHAR(36) PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(512) NOT NULL,
    excerpt TEXT,
    cover_image_url TEXT NULL,
    content_html LONGTEXT,
    content_json JSON NULL,
    reading_minutes INT NOT NULL DEFAULT 0,
    type VARCHAR(32) NULL,
    level VARCHAR(32) NULL,
    topic VARCHAR(64) NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    author_id CHAR(36) NOT NULL,
    moderation_status VARCHAR(16) NOT NULL,
    disclaimer TEXT NULL,
    published_at DATETIME NULL,
    updated_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    views BIGINT NOT NULL DEFAULT 0,
    INDEX idx_posts_status_published (moderation_status, published_at),
    FULLTEXT KEY idx_posts_fulltext (title, excerpt),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS threads (
    id CHAR(36) PRIMARY KEY,
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(512) NOT NULL,
    content TEXT NOT NULL,
    context_json JSON NULL,
    asker_id CHAR(36) NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    thread_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    moderation_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    last_activity_at DATETIME NULL,
    answer_count INT NOT NULL DEFAULT 0,
    has_pharmacist_answer BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_threads_status_activity (moderation_status, last_activity_at),
    FULLTEXT KEY idx_threads_fulltext (title, content),
    CONSTRAINT fk_threads_asker FOREIGN KEY (asker_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS answers (
    id CHAR(36) PRIMARY KEY,
    thread_id CHAR(36) NOT NULL,
    author_id CHAR(36) NOT NULL,
    content TEXT NOT NULL,
    references_json JSON NULL,
    moderation_status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_best_answer BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_answers_thread_created (thread_id, created_at),
    INDEX idx_answers_pinned_best (thread_id, is_pinned, is_best_answer),
    CONSTRAINT fk_answers_thread FOREIGN KEY (thread_id) REFERENCES threads(id),
    CONSTRAINT fk_answers_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS tags (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_tags_type_slug (type, slug)
);

CREATE TABLE IF NOT EXISTS post_tags (
    post_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS thread_tags (
    thread_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (thread_id, tag_id),
    CONSTRAINT fk_thread_tags_thread FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    CONSTRAINT fk_thread_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS votes (
    id CHAR(36) PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    value INT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_votes_target_user (target_type, target_id, user_id),
    CONSTRAINT fk_votes_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS reports (
    id CHAR(36) PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id CHAR(36) NOT NULL,
    reporter_id CHAR(36) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    note TEXT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_reports_user FOREIGN KEY (reporter_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS moderation_logs (
    id CHAR(36) PRIMARY KEY,
    target_type VARCHAR(16) NOT NULL,
    target_id CHAR(36) NOT NULL,
    action VARCHAR(32) NOT NULL,
    actor_id CHAR(36) NOT NULL,
    reason TEXT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_moderation_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);
