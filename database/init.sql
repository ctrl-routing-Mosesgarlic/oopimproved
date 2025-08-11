-- Drink Business RMI System Database Initialization Script
-- Run this script to set up the database with sample data

CREATE DATABASE IF NOT EXISTS drinkdb1;
USE drinkdb1;

-- Disable foreign key checks for clean table dropping
SET FOREIGN_KEY_CHECKS = 0;

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stocks;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS drinks;
DROP TABLE IF EXISTS branches;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Create tables
-- Customers
CREATE TABLE customers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    UNIQUE(name)
);

-- Branches
CREATE TABLE branches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Insert branch data
INSERT INTO branches (id, name) VALUES 
(1, 'Nakuru'), 
(2, 'Mombasa'), 
(3, 'Kisumu'), 
(4, 'Nairobi');

-- Drinks
CREATE TABLE drinks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    price DECIMAL(10,2) NOT NULL
);

-- Insert drink data (12+ different types)
INSERT INTO drinks (name, price) VALUES
('Soda', 50.0), 
('Milk', 70.0), 
('Water', 30.0), 
('Alcohol', 150.0),
('Juice', 80.0),
('Coffee', 120.0),
('Tea', 60.0),
('Energy Drink', 200.0),
('Smoothie', 180.0),
('Iced Coffee', 140.0),
('Hot Chocolate', 110.0),
('Fresh Lemonade', 90.0),
('Sports Drink', 160.0),
('Coconut Water', 120.0),
('Herbal Tea', 80.0),
('Protein Shake', 250.0);

-- Stocks
CREATE TABLE stocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    branch_id BIGINT NOT NULL,
    drink_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    threshold INT DEFAULT 10,
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id),
    UNIQUE(branch_id, drink_id)
);

-- Initialize stock for all branches and drinks with varied quantities (totaling 300+ per drink type across all branches)
INSERT INTO stocks (branch_id, drink_id, quantity, threshold) VALUES
-- Nakuru Branch (branch_id = 1)
(1, 1, 85, 10),   -- Soda
(1, 2, 75, 10),   -- Milk
(1, 3, 95, 10),   -- Water
(1, 4, 45, 10),   -- Alcohol
(1, 5, 70, 10),   -- Juice
(1, 6, 60, 10),   -- Coffee
(1, 7, 80, 10),   -- Tea
(1, 8, 40, 10),   -- Energy Drink
(1, 9, 55, 10),   -- Smoothie
(1, 10, 50, 10),  -- Iced Coffee
(1, 11, 65, 10),  -- Hot Chocolate
(1, 12, 70, 10),  -- Fresh Lemonade
(1, 13, 45, 10),  -- Sports Drink
(1, 14, 60, 10),  -- Coconut Water
(1, 15, 75, 10),  -- Herbal Tea
(1, 16, 35, 10),  -- Protein Shake

-- Mombasa Branch (branch_id = 2)
(2, 1, 90, 10),   -- Soda
(2, 2, 80, 10),   -- Milk
(2, 3, 100, 10),  -- Water
(2, 4, 50, 10),   -- Alcohol
(2, 5, 75, 10),   -- Juice
(2, 6, 65, 10),   -- Coffee
(2, 7, 85, 10),   -- Tea
(2, 8, 45, 10),   -- Energy Drink
(2, 9, 60, 10),   -- Smoothie
(2, 10, 55, 10),  -- Iced Coffee
(2, 11, 70, 10),  -- Hot Chocolate
(2, 12, 75, 10),  -- Fresh Lemonade
(2, 13, 50, 10),  -- Sports Drink
(2, 14, 65, 10),  -- Coconut Water
(2, 15, 80, 10),  -- Herbal Tea
(2, 16, 40, 10),  -- Protein Shake

-- Kisumu Branch (branch_id = 3)
(3, 1, 80, 10),   -- Soda
(3, 2, 70, 10),   -- Milk
(3, 3, 90, 10),   -- Water
(3, 4, 40, 10),   -- Alcohol
(3, 5, 65, 10),   -- Juice
(3, 6, 55, 10),   -- Coffee
(3, 7, 75, 10),   -- Tea
(3, 8, 35, 10),   -- Energy Drink
(3, 9, 50, 10),   -- Smoothie
(3, 10, 45, 10),  -- Iced Coffee
(3, 11, 60, 10),  -- Hot Chocolate
(3, 12, 65, 10),  -- Fresh Lemonade
(3, 13, 40, 10),  -- Sports Drink
(3, 14, 55, 10),  -- Coconut Water
(3, 15, 70, 10),  -- Herbal Tea
(3, 16, 30, 10),  -- Protein Shake

-- Nairobi Branch (branch_id = 4)
(4, 1, 95, 10),   -- Soda
(4, 2, 85, 10),   -- Milk
(4, 3, 105, 10),  -- Water
(4, 4, 55, 10),   -- Alcohol
(4, 5, 80, 10),   -- Juice
(4, 6, 70, 10),   -- Coffee
(4, 7, 90, 10),   -- Tea
(4, 8, 50, 10),   -- Energy Drink
(4, 9, 65, 10),   -- Smoothie
(4, 10, 60, 10),  -- Iced Coffee
(4, 11, 75, 10),  -- Hot Chocolate
(4, 12, 80, 10),  -- Fresh Lemonade
(4, 13, 55, 10),  -- Sports Drink
(4, 14, 70, 10),  -- Coconut Water
(4, 15, 85, 10),  -- Herbal Tea
(4, 16, 45, 10);  -- Protein Shake

-- Orders
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    order_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DOUBLE NOT NULL DEFAULT 0,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (branch_id) REFERENCES branches(id)
);

-- Order Items
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    drink_id BIGINT NOT NULL,
    drink_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (drink_id) REFERENCES drinks(id)
);

-- Users for login and role access
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin','customer_support','auditor','globalmanager','branchmanager','branch_staff','customer') NOT NULL,
    branch_id BIGINT,
    customer_id BIGINT,
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Insert sample customers
INSERT INTO customers (name, email, phone) VALUES
('John Doe', 'john.doe@email.com', '+254700123456'),
('Jane Smith', 'jane.smith@email.com', '+254700234567'),
('Mike Johnson', 'mike.johnson@email.com', '+254700345678'),
('Sarah Wilson', 'sarah.wilson@email.com', '+254700456789'),
('David Brown', 'david.brown@email.com', '+254700567890');

-- Insert sample users with hashed passwords
-- Note: All passwords are "password123" hashed with BCrypt
-- In production, use proper password hashing
INSERT INTO users (username, password_hash, role, branch_id, customer_id) VALUES
-- Admin users (HQ)
('admin', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'admin', NULL, NULL),
('support1', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'customer_support', NULL, NULL),
('auditor1', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'auditor', NULL, NULL),
('globalmanager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'globalmanager', NULL, NULL),

-- Branch managers
('nakuru_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branchmanager', 1, NULL),
('mombasa_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branchmanager', 2, NULL),
('kisumu_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branchmanager', 3, NULL),
('nairobi_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', 4, NULL),

-- Branch staff
('nakuru_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branch_staff', 1, NULL),
('mombasa_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branch_staff', 2, NULL),
('kisumu_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branch_staff', 3, NULL),
('nairobi_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'branch_staff', 4, NULL),

-- Customer users
('customer1', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'customer', NULL, 1),
('customer2', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'customer', NULL, 2),
('customer3', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'customer', NULL, 3);

-- Insert sample orders
INSERT INTO orders (customer_id, branch_id, order_time, status) VALUES
(1, 1, '2024-01-15 10:30:00', 'pending'),
(2, 2, '2024-01-15 14:20:00', 'pending'),
(3, 3, '2024-01-16 09:15:00', 'pending'),
(1, 4, '2024-01-16 16:45:00', 'pending'),
(2, 1, '2024-01-17 11:30:00', 'pending'),
(3, 2, '2024-01-17 13:45:00', 'pending'),
(4, 3, '2024-01-18 08:20:00', 'pending'),
(5, 4, '2024-01-18 15:30:00', 'pending');

-- Insert sample order items (including new drinks)
INSERT INTO order_items (order_id, drink_id, drink_name, quantity, unit_price) VALUES
-- Order 1: John Doe at Nakuru
(1, 1, 'Soda', 2, 50.0),
(1, 3, 'Water', 1, 30.0),
-- Order 2: Jane Smith at Mombasa
(2, 2, 'Milk', 1, 70.0),
(2, 4, 'Alcohol', 1, 150.0),
-- Order 3: Mike Johnson at Kisumu
(3, 1, 'Soda', 3, 50.0),
(3, 5, 'Juice', 2, 80.0),
-- Order 4: John Doe at Nairobi
(4, 6, 'Coffee', 2, 120.0),
(4, 10, 'Iced Coffee', 1, 140.0),
-- Order 5: Jane Smith at Nakuru
(5, 7, 'Tea', 1, 60.0),
(5, 8, 'Energy Drink', 1, 200.0),
-- Order 6: Mike Johnson at Mombasa (new drinks)
(6, 9, 'Smoothie', 2, 180.0),
(6, 12, 'Fresh Lemonade', 1, 90.0),
-- Order 7: Sarah Wilson at Kisumu
(7, 11, 'Hot Chocolate', 1, 110.0),
(7, 15, 'Herbal Tea', 2, 80.0),
-- Order 8: David Brown at Nairobi
(8, 13, 'Sports Drink', 1, 160.0),
(8, 16, 'Protein Shake', 1, 250.0);

-- Update total amounts for orders
UPDATE orders o SET total_amount = (
    SELECT SUM(oi.quantity * oi.unit_price)
    FROM order_items oi
    WHERE oi.order_id = o.id
);

-- Create notifications table for real-time notifications
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'INFO',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    action_url VARCHAR(500),
    metadata TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_user_read (user_id, is_read)
);

-- Create payments table if it doesn't exist
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Add index for faster lookups
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_customer_id ON payments(customer_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);

-- Add payment_status column to orders table if it doesn't exist
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50) DEFAULT 'PENDING';

-- Insert sample notifications
INSERT INTO notifications (user_id, title, message, type, priority) VALUES
(1, 'Welcome!', 'Welcome to the Drink Business RMI System', 'INFO', 'NORMAL'),
(2, 'Order Confirmed', 'Your order #2 has been confirmed', 'ORDER_CONFIRMED', 'HIGH'),
(3, 'Stock Alert', 'Low stock alert for Soda at Kisumu branch', 'STOCK_LOW', 'HIGH'),
(4, 'System Update', 'System maintenance scheduled for tonight', 'SYSTEM_ALERT', 'NORMAL'),
(5, 'New Drinks Added', 'Check out our new smoothies and protein shakes!', 'PRODUCT_UPDATE', 'NORMAL'),
(6, 'Inventory Update', 'Stock levels have been updated across all branches', 'INVENTORY_UPDATE', 'NORMAL');

-- Update stock quantities based on orders
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 1 AND drink_id = 1; -- Nakuru Soda
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 3; -- Nakuru Water
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 2; -- Mombasa Milk
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 4; -- Mombasa Alcohol
UPDATE stocks SET quantity = quantity - 3 WHERE branch_id = 3 AND drink_id = 1; -- Kisumu Soda
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 3 AND drink_id = 5; -- Kisumu Juice
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 4 AND drink_id = 6; -- Nairobi Coffee
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 10; -- Nairobi Iced Coffee
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 7; -- Nakuru Tea
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 8; -- Nakuru Energy Drink
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 2 AND drink_id = 9; -- Mombasa Smoothie
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 12; -- Mombasa Fresh Lemonade
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 3 AND drink_id = 11; -- Kisumu Hot Chocolate
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 3 AND drink_id = 15; -- Kisumu Herbal Tea
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 13; -- Nairobi Sports Drink
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 16; -- Nairobi Protein Shake

-- Set some items to low stock for testing alerts
UPDATE stocks SET quantity = 8 WHERE branch_id = 1 AND drink_id = 4; -- Nakuru Alcohol low
UPDATE stocks SET quantity = 5 WHERE branch_id = 2 AND drink_id = 1; -- Mombasa Soda low
UPDATE stocks SET quantity = 2 WHERE branch_id = 3 AND drink_id = 8; -- Kisumu Energy Drink very low
UPDATE stocks SET quantity = 6 WHERE branch_id = 4 AND drink_id = 16; -- Nairobi Protein Shake low

COMMIT;

-- Display summary with stock totals
SELECT 'Database initialization completed successfully!' as Status;
SELECT COUNT(*) as 'Total Branches' FROM branches;
SELECT COUNT(*) as 'Total Drinks' FROM drinks;
SELECT COUNT(*) as 'Total Customers' FROM customers;
SELECT COUNT(*) as 'Total Users' FROM users;
SELECT COUNT(*) as 'Total Stock Items' FROM stocks;
SELECT COUNT(*) as 'Total Orders' FROM orders;
SELECT COUNT(*) as 'Total Order Items' FROM order_items;

-- Show drink inventory totals across all branches
SELECT 
    d.name as 'Drink Name',
    SUM(s.quantity) as 'Total Stock Across All Branches',
    d.price as 'Unit Price'
FROM drinks d
JOIN stocks s ON d.id = s.drink_id
GROUP BY d.id, d.name, d.price
ORDER BY d.name;

-- Show total stock value
SELECT 
    SUM(s.quantity * d.price) as 'Total Stock Value (KES)'
FROM stocks s
JOIN drinks d ON s.drink_id = d.id;