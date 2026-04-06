CREATE TABLE IF NOT EXISTS post_images (
    id CHAR(36) PRIMARY KEY,
    post_id CHAR(36) NOT NULL,
    image_url TEXT NOT NULL,
    alt_text TEXT NULL,
    position INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_post_images_post (post_id, position),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);
