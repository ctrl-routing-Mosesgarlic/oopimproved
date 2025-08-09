-- Migration to add default value to total_amount column in orders table
USE drinks1;

-- Modify the total_amount column to have a default value of 0
ALTER TABLE orders MODIFY COLUMN total_amount DOUBLE NOT NULL DEFAULT 0;
