-- Drink Business RMI System Database Initialization Script
-- Run this script to set up the database with sample data

CREATE DATABASE IF NOT EXISTS drinkdb1;
USE drinkdb1;

-- Drop existing tables if they exist (for clean setup)
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS stocks;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS drinks;
DROP TABLE IF EXISTS branches;

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

-- Insert drink data
INSERT INTO drinks (name, price) VALUES
('Soda', 50.0), 
('Milk', 70.0), 
('Water', 30.0), 
('Alcohol', 150.0),
('Juice', 80.0),
('Coffee', 120.0),
('Tea', 60.0),
('Energy Drink', 200.0);

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

-- Initialize stock for all branches and drinks
INSERT INTO stocks (branch_id, drink_id, quantity, threshold) 
SELECT b.id, d.id, 100, 10
FROM branches b CROSS JOIN drinks d;

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
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('admin', 'support', 'auditor', 'manager', 'staff', 'customer') NOT NULL,
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
('support1', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'support', NULL, NULL),
('auditor1', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'auditor', NULL, NULL),
('globalmanager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', NULL, NULL),

-- Branch managers
('nakuru_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', 1, NULL),
('mombasa_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', 2, NULL),
('kisumu_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', 3, NULL),
('nairobi_manager', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'manager', 4, NULL),

-- Branch staff
('nakuru_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'staff', 1, NULL),
('mombasa_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'staff', 2, NULL),
('kisumu_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'staff', 3, NULL),
('nairobi_staff', '$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG', 'staff', 4, NULL),

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
(2, 1, '2024-01-17 11:30:00', 'pending');

-- Insert sample order items
INSERT INTO order_items (order_id, drink_id, quantity) VALUES
-- Order 1: John Doe at Nakuru
(1, 1, 2), -- 2 Sodas
(1, 3, 1), -- 1 Water
-- Order 2: Jane Smith at Mombasa
(2, 2, 1), -- 1 Milk
(2, 4, 1), -- 1 Alcohol
-- Order 3: Mike Johnson at Kisumu
(3, 1, 3), -- 3 Sodas
(3, 5, 2), -- 2 Juices
-- Order 4: John Doe at Nairobi
(4, 6, 2), -- 2 Coffees
-- Order 5: Jane Smith at Nakuru
(5, 7, 1), -- 1 Tea
(5, 8, 1); -- 1 Energy Drink

-- Update stock quantities to reflect the orders
UPDATE orders o SET total_amount = (
    SELECT SUM(oi.quantity * d.price)
    FROM order_items oi
    JOIN drinks d ON oi.drink_id = d.id
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
(4, 'System Update', 'System maintenance scheduled for tonight', 'SYSTEM_ALERT', 'NORMAL');

-- Update stock quantities
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 1 AND drink_id = 1; -- Nakuru Soda
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 3; -- Nakuru Water
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 2; -- Mombasa Milk
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 4; -- Mombasa Alcohol
UPDATE stocks SET quantity = quantity - 3 WHERE branch_id = 3 AND drink_id = 1; -- Kisumu Soda
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 3 AND drink_id = 5; -- Kisumu Juice
UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 4 AND drink_id = 6; -- Nairobi Coffee
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 7; -- Nakuru Tea
UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 8; -- Nakuru Energy Drink

-- Set some items to low stock for testing alerts
UPDATE stocks SET quantity = 5 WHERE branch_id = 1 AND drink_id = 4; -- Nakuru Alcohol low
UPDATE stocks SET quantity = 3 WHERE branch_id = 2 AND drink_id = 1; -- Mombasa Soda low
UPDATE stocks SET quantity = 0 WHERE branch_id = 3 AND drink_id = 8; -- Kisumu Energy Drink out of stock

COMMIT;

-- Display summary
SELECT 'Database initialization completed successfully!' as Status;
SELECT COUNT(*) as 'Total Branches' FROM branches;
SELECT COUNT(*) as 'Total Drinks' FROM drinks;
SELECT COUNT(*) as 'Total Customers' FROM customers;
SELECT COUNT(*) as 'Total Users' FROM users;
SELECT COUNT(*) as 'Total Stock Items' FROM stocks;
SELECT COUNT(*) as 'Total Orders' FROM orders;
SELECT COUNT(*) as 'Total Order Items' FROM order_items;
