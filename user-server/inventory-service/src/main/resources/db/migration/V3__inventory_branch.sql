-- Branch-aware inventory migration

SET @default_branch_id = '00000000-0000-0000-0000-000000000001';

CREATE TABLE IF NOT EXISTS inventory_items_new (
    branch_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    on_hand INT NOT NULL DEFAULT 0,
    reserved INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (branch_id, product_id),
    INDEX idx_inventory_items_branch (branch_id)
);

INSERT INTO inventory_items_new (branch_id, product_id, on_hand, reserved, updated_at)
SELECT @default_branch_id, product_id, on_hand, reserved, updated_at
FROM inventory_items;

DROP TABLE inventory_items;
RENAME TABLE inventory_items_new TO inventory_items;

ALTER TABLE reservations
    ADD COLUMN branch_id CHAR(36) NULL AFTER order_id;

UPDATE reservations SET branch_id = @default_branch_id WHERE branch_id IS NULL;
ALTER TABLE reservations MODIFY branch_id CHAR(36) NOT NULL;

UPDATE inventory_activities SET branch_id = @default_branch_id WHERE branch_id IS NULL;
ALTER TABLE inventory_activities MODIFY branch_id CHAR(36) NOT NULL;

UPDATE stock_documents SET branch_id = @default_branch_id WHERE branch_id IS NULL;
ALTER TABLE stock_documents MODIFY branch_id CHAR(36) NOT NULL;
