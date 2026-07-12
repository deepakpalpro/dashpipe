USE petstore;

CREATE TABLE IF NOT EXISTS inventory_items (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku         VARCHAR(64) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    category    ENUM('food', 'accessories', 'toys') NOT NULL,
    quantity    INT NOT NULL DEFAULT 0,
    unit_price  DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    description VARCHAR(1024) NULL,
    status      ENUM('in_stock', 'low_stock', 'out_of_stock') NOT NULL DEFAULT 'in_stock',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_inventory_sku (sku),
    KEY idx_inventory_category (category),
    KEY idx_inventory_status (status)
) ENGINE=InnoDB;
