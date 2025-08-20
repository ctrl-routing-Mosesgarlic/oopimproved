-- Migration: Add missing columns to notifications table
-- Date: 2025-08-20
-- Description: Add action_url and metadata columns to notifications table if they don't exist

USE drinkdbsales;

-- Add action_url column if it doesn't exist
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'drinkdbsales' 
     AND TABLE_NAME = 'notifications' 
     AND COLUMN_NAME = 'action_url') = 0,
    'ALTER TABLE notifications ADD COLUMN action_url VARCHAR(500) AFTER created_at;',
    'SELECT "action_url column already exists";'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add metadata column if it doesn't exist
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'drinkdbsales' 
     AND TABLE_NAME = 'notifications' 
     AND COLUMN_NAME = 'metadata') = 0,
    'ALTER TABLE notifications ADD COLUMN metadata TEXT AFTER action_url;',
    'SELECT "metadata column already exists";'
));

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the columns were added
SELECT 'Notifications table columns:' as status;
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'drinkdbsales' 
AND TABLE_NAME = 'notifications'
ORDER BY ORDINAL_POSITION;
