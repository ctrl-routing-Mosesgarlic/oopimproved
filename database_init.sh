#!/bin/bash
# database_init.sh - Initializes the drinkdb1 database using init.sql

DB_NAME="drinkdb1"
INIT_FILE="database/init.sql"
MYSQL_USER="dba"
MYSQL_PASS="zIppoRah" # Leave empty to prompt for password

echo "Initializing database $DB_NAME..."

# Check if MySQL is running
if ! systemctl is-active --quiet mysql; then
    echo "Starting MySQL service..."
    sudo systemctl start mysql
fi

# Execute the initialization script
if [ -z "$MYSQL_PASS" ]; then
    echo "Please enter MySQL password for user $MYSQL_USER"
    mysql -u "$MYSQL_USER" -p -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
    mysql -u "$MYSQL_USER" -p "$DB_NAME" < "$INIT_FILE"
else
    mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
    mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" "$DB_NAME" < "$INIT_FILE"
fi

if [ $? -eq 0 ]; then
    echo "Database $DB_NAME initialized successfully!"
else
    echo "Error initializing database $DB_NAME"
    exit 1
fi
