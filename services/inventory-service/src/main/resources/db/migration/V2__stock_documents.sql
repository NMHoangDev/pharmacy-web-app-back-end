ALTER TABLE inventory_activities
    ADD COLUMN ref_type VARCHAR(32) NULL,
    ADD COLUMN ref_id CHAR(36) NULL,
    ADD COLUMN actor VARCHAR(128) NULL,
    ADD COLUMN branch_id CHAR(36) NULL,
    ADD COLUMN batch_no VARCHAR(128) NULL,
    ADD COLUMN expiry_date DATE NULL;

CREATE TABLE IF NOT EXISTS stock_documents (
    id CHAR(36) PRIMARY KEY,
    doc_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    supplier_name VARCHAR(255),
    supplier_id VARCHAR(255),
    invoice_no VARCHAR(128),
    reason TEXT,
    created_by VARCHAR(128),
    approved_by VARCHAR(128),
    branch_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    approved_at TIMESTAMP NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stock_documents_status (status),
    INDEX idx_stock_documents_type (doc_type),
    INDEX idx_stock_documents_created (created_at)
);

CREATE TABLE IF NOT EXISTS stock_document_lines (
    id CHAR(36) PRIMARY KEY,
    document_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    sku_snapshot VARCHAR(255),
    quantity INT NOT NULL,
    unit_cost DECIMAL(12,2) NULL,
    batch_no VARCHAR(128) NULL,
    expiry_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_doc_lines_document (document_id),
    INDEX idx_stock_doc_lines_product (product_id),
    CONSTRAINT fk_stock_doc_lines_document FOREIGN KEY (document_id) REFERENCES stock_documents(id) ON DELETE CASCADE
);
