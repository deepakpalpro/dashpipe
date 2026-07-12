-- Petstore schema (OpenAPI 3.0 Petstore)
CREATE DATABASE IF NOT EXISTS petstore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE petstore;

CREATE TABLE IF NOT EXISTS categories (
    id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tags (
    id   BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pets (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    category_id BIGINT NULL,
    status      ENUM('available', 'pending', 'sold') NOT NULL DEFAULT 'available',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pets_status (status),
    CONSTRAINT fk_pets_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pet_photos (
    id       BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_id   BIGINT NOT NULL,
    url      VARCHAR(1024) NOT NULL,
    KEY idx_pet_photos_pet (pet_id),
    CONSTRAINT fk_pet_photos_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS pet_tags (
    pet_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (pet_id, tag_id),
    CONSTRAINT fk_pet_tags_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE CASCADE,
    CONSTRAINT fk_pet_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pet_id             BIGINT NOT NULL,
    customer_username  VARCHAR(255) NULL,
    quantity           INT NOT NULL DEFAULT 1,
    unit_price         DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    currency           CHAR(3) NOT NULL DEFAULT 'USD',
    ship_date          DATETIME(3) NULL,
    status             ENUM('placed', 'approved', 'delivered') NOT NULL DEFAULT 'placed',
    complete           TINYINT(1) NOT NULL DEFAULT 0,
    notes              VARCHAR(512) NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_orders_status (status),
    KEY idx_orders_pet_id (pet_id),
    KEY idx_orders_customer (customer_username),
    CONSTRAINT fk_orders_pet FOREIGN KEY (pet_id) REFERENCES pets (id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(255) NULL,
    last_name   VARCHAR(255) NULL,
    email       VARCHAR(255) NULL,
    password    VARCHAR(255) NULL,
    phone       VARCHAR(64) NULL,
    user_status INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB;
