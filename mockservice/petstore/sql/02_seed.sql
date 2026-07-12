USE petstore;

INSERT INTO categories (id, name) VALUES
    (1, 'Dogs'),
    (2, 'Cats'),
    (3, 'Birds')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO tags (id, name) VALUES
    (1, 'friendly'),
    (2, 'trained'),
    (3, 'vaccinated')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO pets (id, name, category_id, status) VALUES
    (1, 'doggie', 1, 'available'),
    (2, 'kitty', 2, 'pending'),
    (3, 'polly', 3, 'sold')
ON DUPLICATE KEY UPDATE name = VALUES(name), category_id = VALUES(category_id), status = VALUES(status);

INSERT INTO pet_photos (pet_id, url)
SELECT 1, 'https://example.com/photos/doggie.jpg'
WHERE NOT EXISTS (SELECT 1 FROM pet_photos WHERE pet_id = 1 AND url = 'https://example.com/photos/doggie.jpg');

INSERT INTO pet_photos (pet_id, url)
SELECT 2, 'https://example.com/photos/kitty.jpg'
WHERE NOT EXISTS (SELECT 1 FROM pet_photos WHERE pet_id = 2 AND url = 'https://example.com/photos/kitty.jpg');

INSERT INTO pet_photos (pet_id, url)
SELECT 3, 'https://example.com/photos/polly.jpg'
WHERE NOT EXISTS (SELECT 1 FROM pet_photos WHERE pet_id = 3 AND url = 'https://example.com/photos/polly.jpg');

INSERT IGNORE INTO pet_tags (pet_id, tag_id) VALUES
    (1, 1), (1, 3),
    (2, 1),
    (3, 2);

INSERT INTO users (id, username, first_name, last_name, email, password, phone, user_status) VALUES
    (1, 'user1', 'John', 'James', 'john@email.com', '12345', '12345', 1),
    (2, 'theUser', 'Jane', 'Doe', 'jane@email.com', 'password', '555-0100', 1)
ON DUPLICATE KEY UPDATE
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    email = VALUES(email),
    password = VALUES(password),
    phone = VALUES(phone),
    user_status = VALUES(user_status);

INSERT INTO orders (id, pet_id, quantity, ship_date, status, complete) VALUES
    (1, 1, 1, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY), 'placed', 0),
    (2, 3, 2, DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 5 DAY), 'approved', 0)
ON DUPLICATE KEY UPDATE
    pet_id = VALUES(pet_id),
    quantity = VALUES(quantity),
    status = VALUES(status),
    complete = VALUES(complete);

-- Keep auto-increment ahead of seeded IDs
ALTER TABLE categories AUTO_INCREMENT = 100;
ALTER TABLE tags AUTO_INCREMENT = 100;
ALTER TABLE pets AUTO_INCREMENT = 100;
ALTER TABLE orders AUTO_INCREMENT = 100;
ALTER TABLE users AUTO_INCREMENT = 100;

-- Invoice-ready order details are applied in 06_orders_invoice_seed.sql
