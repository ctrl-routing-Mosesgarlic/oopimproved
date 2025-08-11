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

# Drop existing database if it exists
mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "DROP DATABASE IF EXISTS $DB_NAME;"

# Create new database
mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" -e "CREATE DATABASE $DB_NAME;"

# Initialize schema
mysql -u "$MYSQL_USER" -p"$MYSQL_PASS" "$DB_NAME" < "$INIT_FILE"

echo "Database $DB_NAME has been recreated and initialized"

if [ $? -eq 0 ]; then
    echo "Database $DB_NAME initialized successfully!"
else
    echo "Error initializing database $DB_NAME"
    exit 1
fi
