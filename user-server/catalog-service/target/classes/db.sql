-- catalog_db schema (NO UUID)

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS drugs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    price DECIMAL(12,2) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
    prescription_required BOOLEAN DEFAULT FALSE,
    description TEXT,
    image_url TEXT,
    attributes JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_drugs_category ON drugs(category_id);
CREATE INDEX IF NOT EXISTS idx_drugs_slug ON drugs(slug);
