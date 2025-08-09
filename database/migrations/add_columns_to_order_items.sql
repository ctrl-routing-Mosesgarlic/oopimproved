-- Migration to add missing columns to order_items table
ALTER TABLE order_items ADD COLUMN drink_name VARCHAR(255) NOT NULL AFTER drink_id;
ALTER TABLE order_items ADD COLUMN unit_price DECIMAL(10,2) NOT NULL AFTER quantity;
ALTER TABLE order_items ADD COLUMN total_price DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED;
