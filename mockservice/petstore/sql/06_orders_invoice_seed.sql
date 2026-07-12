USE petstore;

-- Invoice-demo orders (idempotent upserts by id)
INSERT INTO orders (id, pet_id, customer_username, quantity, unit_price, currency, ship_date, status, complete, notes) VALUES
    (1, 1, 'user1', 1, 99.99, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 2 DAY), 'placed', 0, 'Standard pet adoption order'),
    (2, 3, 'theUser', 2, 49.50, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 5 DAY), 'approved', 0, 'Bulk bird order'),
    (3, 2, 'user1', 1, 79.00, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 DAY), 'delivered', 1, 'Cat adoption — ready to invoice'),
    (4, 1, 'theUser', 1, 99.99, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 3 DAY), 'approved', 0, 'Second dog order'),
    (5, 3, 'user1', 3, 45.00, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY), 'placed', 0, 'Multi-qty bird order'),
    (6, 2, 'theUser', 1, 79.00, 'USD', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 2 DAY), 'delivered', 1, 'Completed — invoice candidate'),
    (7, 1, 'user1', 2, 89.99, 'USD', DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 4 DAY), 'approved', 0, 'Family dog package'),
    (8, 3, 'theUser', 1, 55.00, 'USD', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY), 'delivered', 1, 'Delivered bird — invoice candidate')
ON DUPLICATE KEY UPDATE
    pet_id = VALUES(pet_id),
    customer_username = VALUES(customer_username),
    quantity = VALUES(quantity),
    unit_price = VALUES(unit_price),
    currency = VALUES(currency),
    ship_date = VALUES(ship_date),
    status = VALUES(status),
    complete = VALUES(complete),
    notes = VALUES(notes);

ALTER TABLE orders AUTO_INCREMENT = 100;
