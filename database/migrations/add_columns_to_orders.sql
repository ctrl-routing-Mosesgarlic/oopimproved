-- Migration to add missing columns to orders table
USE drinks1;

-- Add status column with default value 'PENDING'
ALTER TABLE orders ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER order_time;

-- Add total_amount column with default value 0
ALTER TABLE orders ADD COLUMN total_amount DOUBLE NOT NULL DEFAULT 0 AFTER status;
