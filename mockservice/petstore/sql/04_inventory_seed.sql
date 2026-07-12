USE petstore;

INSERT INTO inventory_items (sku, name, category, quantity, unit_price, description, status) VALUES
    ('FOOD-001', 'Dog Kibble 5kg', 'food', 120, 29.99, 'Adult dog dry food', 'in_stock'),
    ('FOOD-002', 'Cat Salmon Pate', 'food', 80, 2.49, 'Wet food pouch 85g', 'in_stock'),
    ('TOY-001', 'Rubber Ball', 'toys', 50, 4.99, 'Fetch toy for dogs', 'in_stock'),
    ('TOY-002', 'Catnip Mouse', 'toys', 8, 3.50, 'Soft toy with catnip', 'low_stock'),
    ('ACC-001', 'Leather Leash', 'accessories', 35, 19.50, '1.5m leather leash', 'in_stock'),
    ('ACC-002', 'Stainless Bowl', 'accessories', 0, 9.99, 'Medium pet bowl', 'out_of_stock')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    quantity = VALUES(quantity),
    unit_price = VALUES(unit_price),
    description = VALUES(description),
    status = VALUES(status);
