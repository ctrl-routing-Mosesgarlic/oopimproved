package com.drinks.rmi.util;

import com.drinks.rmi.common.DatabaseConfig;
import org.mindrot.jbcrypt.BCrypt;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Database seeder that matches the init.sql schema
 * Programmatically initializes the database with all required data
 */
public class DatabaseSeeder {
    private static final Logger logger = Logger.getLogger(DatabaseSeeder.class.getName());
    
    public static void main(String[] args) {
        DatabaseSeeder seeder = new DatabaseSeeder();
        try {
            seeder.seedDatabase();
            logger.info("Database seeding completed successfully!");
        } catch (Exception e) {
            logger.severe("Database seeding failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void seedDatabase() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            logger.info("Starting database seeding...");
            
            // Drop and recreate all tables
            dropAllTables(conn);
            createAllTables(conn);
            
            // Seed in order of dependencies
            seedBranches(conn);
            seedCustomers(conn);
            seedUsers(conn);
            seedDrinks(conn);
            seedStocks(conn);
            seedOrders(conn);
            seedOrderItems(conn);
            updateOrderTotals(conn);
            seedNotifications(conn);
            seedPayments(conn);
            updateStockQuantities(conn);
            
            conn.commit();
            logger.info("All data seeded successfully!");
            
        } catch (SQLException e) {
            logger.severe("Error during seeding: " + e.getMessage());
            throw e;

            
        }
    }
    
    private void dropAllTables(Connection conn) throws SQLException {
        logger.info("Dropping all tables...");
        
        try (Statement stmt = conn.createStatement()) {
            // Disable foreign key checks temporarily
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            
            // Drop tables in reverse dependency order
            stmt.executeUpdate("DROP TABLE IF EXISTS payments");
            stmt.executeUpdate("DROP TABLE IF EXISTS notifications");
            stmt.executeUpdate("DROP TABLE IF EXISTS order_items");
            stmt.executeUpdate("DROP TABLE IF EXISTS orders");
            stmt.executeUpdate("DROP TABLE IF EXISTS stocks");
            stmt.executeUpdate("DROP TABLE IF EXISTS users");
            stmt.executeUpdate("DROP TABLE IF EXISTS customers");
            stmt.executeUpdate("DROP TABLE IF EXISTS drinks");
            stmt.executeUpdate("DROP TABLE IF EXISTS branches");
            
            // Re-enable foreign key checks
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
            
            logger.info("All tables dropped successfully");
        }
    }
    
    private void createAllTables(Connection conn) throws SQLException {
        logger.info("Creating all tables...");
        
        try (Statement stmt = conn.createStatement()) {
            // Create customers table
            stmt.executeUpdate(
                "CREATE TABLE customers (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255), " +
                "phone VARCHAR(50), " +
                "UNIQUE(name)" +
                ")"
            );
            
            // Create branches table
            stmt.executeUpdate(
                "CREATE TABLE branches (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name VARCHAR(255) NOT NULL UNIQUE" +
                ")"
            );
            
            // Create users table
            stmt.executeUpdate(
                "CREATE TABLE users (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "username VARCHAR(255) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role ENUM('admin', 'customer_support', 'auditor', 'globalmanager', 'branchmanager', 'branch_staff', 'customer') NOT NULL, " +
                "branch_id BIGINT, " +
                "customer_id BIGINT, " +
                "FOREIGN KEY (branch_id) REFERENCES branches(id), " +
                "FOREIGN KEY (customer_id) REFERENCES customers(id)" +
                ")"
            );
            
            // Create drinks table
            stmt.executeUpdate(
                "CREATE TABLE drinks (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "name VARCHAR(255) NOT NULL UNIQUE, " +
                "price DECIMAL(10,2) NOT NULL" +
                ")"
            );
            
            // Create stocks table
            stmt.executeUpdate(
                "CREATE TABLE stocks (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "branch_id BIGINT NOT NULL, " +
                "drink_id BIGINT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "threshold INT NOT NULL DEFAULT 10, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (branch_id) REFERENCES branches(id), " +
                "FOREIGN KEY (drink_id) REFERENCES drinks(id), " +
                "UNIQUE(branch_id, drink_id)" +
                ")"
            );
            
            // Create orders table
            stmt.executeUpdate(
                "CREATE TABLE orders (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "customer_id BIGINT NOT NULL, " +
                "branch_id BIGINT NOT NULL, " +
                "order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "status VARCHAR(50) DEFAULT 'PENDING', " +
                "total_amount DECIMAL(10,2) DEFAULT 0.00, " +
                "payment_status VARCHAR(50) DEFAULT 'PENDING', " +
                "FOREIGN KEY (customer_id) REFERENCES customers(id), " +
                "FOREIGN KEY (branch_id) REFERENCES branches(id)" +
                ")"
            );
            
            // Create order_items table
            stmt.executeUpdate(
                "CREATE TABLE order_items (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "order_id BIGINT NOT NULL, " +
                "drink_id BIGINT NOT NULL, " +
                "drink_name VARCHAR(255) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "unit_price DECIMAL(10,2) NOT NULL, " +
                "total_price DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id), " +
                "FOREIGN KEY (drink_id) REFERENCES drinks(id)" +
                ")"
            );
            
            // Create notifications table
            stmt.executeUpdate(
                "CREATE TABLE notifications (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "user_id BIGINT NOT NULL, " +
                "title VARCHAR(255) NOT NULL, " +
                "message TEXT, " +
                "type VARCHAR(50) DEFAULT 'INFO', " +
                "priority VARCHAR(20) DEFAULT 'NORMAL', " +
                "is_read BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "INDEX idx_user_read (user_id, is_read)" +
                ")"
            );
            
            // Create payments table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS payments (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id BIGINT NOT NULL, " +
                "customer_id VARCHAR(255) NOT NULL, " +
                "amount DECIMAL(10,2) NOT NULL, " +
                "payment_method VARCHAR(50) NOT NULL, " +
                "transaction_id VARCHAR(255) NOT NULL, " +
                "status VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id)" +
                ")"
            );
            
            // Create indexes for payments table
            stmt.executeUpdate("CREATE INDEX idx_payments_order_id ON payments(order_id)");
            stmt.executeUpdate("CREATE INDEX idx_payments_customer_id ON payments(customer_id)");
            stmt.executeUpdate("CREATE INDEX idx_payments_transaction_id ON payments(transaction_id)");
            
            logger.info("All tables created successfully");
        }
    }
    
    private void clearExistingData(Connection conn) throws SQLException {
        logger.info("Clearing existing data...");
        
        try (Statement stmt = conn.createStatement()) {
            // Disable foreign key checks temporarily
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            
            // Clear tables in reverse dependency order (only if they exist)
            clearTableIfExists(stmt, "payments");
            clearTableIfExists(stmt, "notifications");
            clearTableIfExists(stmt, "order_items");
            clearTableIfExists(stmt, "orders");
            clearTableIfExists(stmt, "stocks");
            clearTableIfExists(stmt, "drinks");
            clearTableIfExists(stmt, "users");
            clearTableIfExists(stmt, "customers");
            clearTableIfExists(stmt, "branches");
            
            // Reset auto-increment counters (only if tables exist)
            resetAutoIncrementIfExists(stmt, "branches");
            resetAutoIncrementIfExists(stmt, "customers");
            resetAutoIncrementIfExists(stmt, "users");
            resetAutoIncrementIfExists(stmt, "drinks");
            resetAutoIncrementIfExists(stmt, "orders");
            resetAutoIncrementIfExists(stmt, "notifications");
            resetAutoIncrementIfExists(stmt, "payments");
            
            // Re-enable foreign key checks
            stmt.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
            
            logger.info("Existing data cleared successfully");
        }
    }
    
    private void clearTableIfExists(Statement stmt, String tableName) {
        try {
            stmt.executeUpdate("DELETE FROM " + tableName);
            logger.info("Cleared table: " + tableName);
        } catch (SQLException e) {
            logger.info("Table " + tableName + " does not exist or could not be cleared: " + e.getMessage());
        }
    }
    
    private void resetAutoIncrementIfExists(Statement stmt, String tableName) {
        try {
            stmt.executeUpdate("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1");
        } catch (SQLException e) {
            // Table doesn't exist or doesn't have auto increment - ignore
        }
    }
    
    private void seedBranches(Connection conn) throws SQLException {
        logger.info("Seeding branches...");
        String sql = "INSERT INTO branches (name) VALUES (?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Nakuru
            stmt.setString(1, "Nakuru");
            stmt.addBatch();
            
            // Mombasa
            stmt.setString(1, "Mombasa");
            stmt.addBatch();
            
            // Kisumu
            stmt.setString(1, "Kisumu");
            stmt.addBatch();
            
            // Nairobi
            stmt.setString(1, "Nairobi");
            stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void seedUsers(Connection conn) throws SQLException {
        logger.info("Seeding users...");
        String sql = "INSERT INTO users (username, password_hash, role, branch_id, customer_id) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Hash password "password123" for all users
            String passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt(10));
            
            // Admin users
            addUser(stmt, "admin", passwordHash, "admin", null, null);
            addUser(stmt, "support1", passwordHash, "customer_support", null, null);
            addUser(stmt, "auditor1", passwordHash, "auditor", null, null);
            addUser(stmt, "globalmanager", passwordHash, "globalmanager", null, null);
            
            // Branch managers
            addUser(stmt, "nakuru_manager", passwordHash, "branchmanager", 1L, null);
            addUser(stmt, "mombasa_manager", passwordHash, "branchmanager", 2L, null);
            addUser(stmt, "kisumu_manager", passwordHash, "branchmanager", 3L, null);
            addUser(stmt, "nairobi_manager", passwordHash, "branchmanager", 4L, null);
            
            // Branch staff
            addUser(stmt, "nakuru_staff", passwordHash, "branch_staff", 1L, null);
            addUser(stmt, "mombasa_staff", passwordHash, "branch_staff", 2L, null);
            addUser(stmt, "kisumu_staff", passwordHash, "branch_staff", 3L, null);
            addUser(stmt, "nairobi_staff", passwordHash, "branch_staff", 4L, null);
            
            // Customer users
            addUser(stmt, "customer1", passwordHash, "customer", null, 1L);
            addUser(stmt, "customer2", passwordHash, "customer", null, 2L);
            addUser(stmt, "customer3", passwordHash, "customer", null, 3L);
            addUser(stmt, "customer4", passwordHash, "customer", null, 4L);
            addUser(stmt, "customer5", passwordHash, "customer", null, 5L);
            addUser(stmt, "customer6", passwordHash, "customer", null, 6L);
            addUser(stmt, "customer7", passwordHash, "customer", null, 7L);
            addUser(stmt, "customer8", passwordHash, "customer", null, 8L);
            addUser(stmt, "customer9", passwordHash, "customer", null, 9L);
            addUser(stmt, "customer10", passwordHash, "customer", null, 10L);
            
            stmt.executeBatch();
        }
    }
    
    private static void addUser(PreparedStatement stmt, String username, String passwordHash, String role, Long branchId, Long customerId) throws SQLException {
        System.out.println("Inserting user: " + username + " with role: '" + role + "'");
        stmt.setString(1, username);
        stmt.setString(2, passwordHash);
        stmt.setString(3, role);
        stmt.setObject(4, branchId);
        stmt.setObject(5, customerId);
        stmt.addBatch();
    }
    
    private void seedCustomers(Connection conn) throws SQLException {
        logger.info("Seeding customers...");
        String sql = "INSERT INTO customers (name, email, phone) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Customer 1
            stmt.setString(1, "John Doe");
            stmt.setString(2, "john.doe@email.com");
            stmt.setString(3, "+254712345678");
            stmt.addBatch();
            
            // Customer 2
            stmt.setString(1, "Jane Smith");
            stmt.setString(2, "jane.smith@email.com");
            stmt.setString(3, "+254723456789");
            stmt.addBatch();
            
            // Customer 3
            stmt.setString(1, "Mary Muthoni");
            stmt.setString(2, "mary.muthoni@email.com");
            stmt.setString(3, "+254734567890");
            stmt.addBatch();

            // Customer 4
            stmt.setString(1, "Mike Jones");
            stmt.setString(2, "mike.jones@email.com");
            stmt.setString(3, "+254745678901");
            stmt.addBatch();

            // Customer 5
            stmt.setString(1, "Wakabando Wambugu");
            stmt.setString(2, "wakabando.wambugu@email.com");
            stmt.setString(3, "+254756789012");
            stmt.addBatch();

            // Customer 6
            stmt.setString(1, "Claving Ochieng");
            stmt.setString(2, "claving.ochieng@email.com");
            stmt.setString(3, "+254767890123");
            stmt.addBatch();

            // Customer 7
            stmt.setString(1, "Demakufu Mwangi");
            stmt.setString(2, "demakufu.mwangi@email.com");
            stmt.setString(3, "+254778901234");
            stmt.addBatch();

            // Customer 8
            stmt.setString(1, "Garang Wong");
            stmt.setString(2, "garang.wong@email.com");
            stmt.setString(3, "+254789012345");
            stmt.addBatch();

            // Customer 9
            stmt.setString(1, "Glen Moses");
            stmt.setString(2, "glen.moses@email.com");
            stmt.setString(3, "+254790123456");
            stmt.addBatch();

            // Customer 10
            stmt.setString(1, "Timothy Ong");
            stmt.setString(2, "timothy.ong@email.com");
            stmt.setString(3, "+254701234567");
            stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void seedDrinks(Connection conn) throws SQLException {
        logger.info("Seeding drinks...");
        String sql = "INSERT INTO drinks (name, price) VALUES (?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            addDrink(stmt, "Soda", 50.0);
            addDrink(stmt, "Milk", 70.0);
            addDrink(stmt, "Water", 30.0);
            addDrink(stmt, "Alcohol", 150.0);
            addDrink(stmt, "Juice", 80.0);
            addDrink(stmt, "Coffee", 120.0);
            addDrink(stmt, "Tea", 60.0);
            addDrink(stmt, "Energy Drink", 200.0);
            addDrink(stmt, "Smoothie", 180.0);
            addDrink(stmt, "Iced Coffee", 140.0);
            addDrink(stmt, "Hot Chocolate", 110.0);
            addDrink(stmt, "Fresh Lemonade", 90.0);
            addDrink(stmt, "Sports Drink", 160.0);
            addDrink(stmt, "Coconut Water", 120.0);
            addDrink(stmt, "Herbal Tea", 80.0);
            addDrink(stmt, "Protein Shake", 250.0);
            
            stmt.executeBatch();
        }
    }
    
    private void addDrink(PreparedStatement stmt, String name, double price) throws SQLException {
        stmt.setString(1, name);
        stmt.setDouble(2, price);
        stmt.addBatch();
    }
    
    private void seedStocks(Connection conn) throws SQLException {
        logger.info("Seeding stocks...");
        String sql = "INSERT INTO stocks (branch_id, drink_id, quantity, threshold) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Nakuru Branch (branch_id = 1)
            addStock(stmt, 1, 1, 85, 10);   // Soda
            addStock(stmt, 1, 2, 75, 10);   // Milk
            addStock(stmt, 1, 3, 95, 10);   // Water
            addStock(stmt, 1, 4, 45, 10);   // Alcohol
            addStock(stmt, 1, 5, 70, 10);   // Juice
            addStock(stmt, 1, 6, 60, 10);   // Coffee
            addStock(stmt, 1, 7, 80, 10);   // Tea
            addStock(stmt, 1, 8, 40, 10);   // Energy Drink
            addStock(stmt, 1, 9, 55, 10);   // Smoothie
            addStock(stmt, 1, 10, 50, 10);  // Iced Coffee
            addStock(stmt, 1, 11, 65, 10);  // Hot Chocolate
            addStock(stmt, 1, 12, 70, 10);  // Fresh Lemonade
            addStock(stmt, 1, 13, 45, 10);  // Sports Drink
            addStock(stmt, 1, 14, 60, 10);  // Coconut Water
            addStock(stmt, 1, 15, 75, 10);  // Herbal Tea
            addStock(stmt, 1, 16, 35, 10);  // Protein Shake
            
            // Mombasa Branch (branch_id = 2)
            addStock(stmt, 2, 1, 90, 10);   // Soda
            addStock(stmt, 2, 2, 80, 10);   // Milk
            addStock(stmt, 2, 3, 100, 10);  // Water
            addStock(stmt, 2, 4, 50, 10);   // Alcohol
            addStock(stmt, 2, 5, 75, 10);   // Juice
            addStock(stmt, 2, 6, 65, 10);   // Coffee
            addStock(stmt, 2, 7, 85, 10);   // Tea
            addStock(stmt, 2, 8, 45, 10);   // Energy Drink
            addStock(stmt, 2, 9, 60, 10);   // Smoothie
            addStock(stmt, 2, 10, 55, 10);  // Iced Coffee
            addStock(stmt, 2, 11, 70, 10);  // Hot Chocolate
            addStock(stmt, 2, 12, 75, 10);  // Fresh Lemonade
            addStock(stmt, 2, 13, 50, 10);  // Sports Drink
            addStock(stmt, 2, 14, 65, 10);  // Coconut Water
            addStock(stmt, 2, 15, 80, 10);  // Herbal Tea
            addStock(stmt, 2, 16, 40, 10);  // Protein Shake
            
            // Kisumu Branch (branch_id = 3)
            addStock(stmt, 3, 1, 80, 10);   // Soda
            addStock(stmt, 3, 2, 70, 10);   // Milk
            addStock(stmt, 3, 3, 90, 10);   // Water
            addStock(stmt, 3, 4, 40, 10);   // Alcohol
            addStock(stmt, 3, 5, 65, 10);   // Juice
            addStock(stmt, 3, 6, 55, 10);   // Coffee
            addStock(stmt, 3, 7, 75, 10);   // Tea
            addStock(stmt, 3, 8, 35, 10);   // Energy Drink
            addStock(stmt, 3, 9, 50, 10);   // Smoothie
            addStock(stmt, 3, 10, 45, 10);  // Iced Coffee
            addStock(stmt, 3, 11, 60, 10);  // Hot Chocolate
            addStock(stmt, 3, 12, 65, 10);  // Fresh Lemonade
            addStock(stmt, 3, 13, 40, 10);  // Sports Drink
            addStock(stmt, 3, 14, 55, 10);  // Coconut Water
            addStock(stmt, 3, 15, 70, 10);  // Herbal Tea
            addStock(stmt, 3, 16, 30, 10);  // Protein Shake
            
            // Nairobi Branch (branch_id = 4)
            addStock(stmt, 4, 1, 95, 10);   // Soda
            addStock(stmt, 4, 2, 85, 10);   // Milk
            addStock(stmt, 4, 3, 105, 10);  // Water
            addStock(stmt, 4, 4, 55, 10);   // Alcohol
            addStock(stmt, 4, 5, 80, 10);   // Juice
            addStock(stmt, 4, 6, 70, 10);   // Coffee
            addStock(stmt, 4, 7, 90, 10);   // Tea
            addStock(stmt, 4, 8, 50, 10);   // Energy Drink
            addStock(stmt, 4, 9, 65, 10);   // Smoothie
            addStock(stmt, 4, 10, 60, 10);  // Iced Coffee
            addStock(stmt, 4, 11, 75, 10);  // Hot Chocolate
            addStock(stmt, 4, 12, 80, 10);  // Fresh Lemonade
            addStock(stmt, 4, 13, 55, 10);  // Sports Drink
            addStock(stmt, 4, 14, 70, 10);  // Coconut Water
            addStock(stmt, 4, 15, 85, 10);  // Herbal Tea
            addStock(stmt, 4, 16, 45, 10);  // Protein Shake
            
            stmt.executeBatch();
        }
    }
    
    private void addStock(PreparedStatement stmt, int branchId, int drinkId, 
                         int quantity, int threshold) throws SQLException {
        stmt.setInt(1, branchId);
        stmt.setInt(2, drinkId);
        stmt.setInt(3, quantity);
        stmt.setInt(4, threshold);
        stmt.addBatch();
    }
    
    private void seedOrders(Connection conn) throws SQLException {
        logger.info("Seeding orders...");
        String sql = "INSERT INTO orders (customer_id, branch_id, order_time, status) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, 1); stmt.setLong(2, 1); stmt.setString(3, "2024-01-15 10:30:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setLong(2, 2); stmt.setString(3, "2024-01-15 14:20:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 3); stmt.setLong(2, 3); stmt.setString(3, "2024-01-16 09:15:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 1); stmt.setLong(2, 4); stmt.setString(3, "2024-01-16 16:45:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setLong(2, 1); stmt.setString(3, "2024-01-17 11:30:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 3); stmt.setLong(2, 2); stmt.setString(3, "2024-01-17 13:45:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 1); stmt.setLong(2, 3); stmt.setString(3, "2024-01-18 08:20:00"); stmt.setString(4, "pending"); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setLong(2, 4); stmt.setString(3, "2024-01-18 15:30:00"); stmt.setString(4, "pending"); stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void seedOrderItems(Connection conn) throws SQLException {
        logger.info("Seeding order items...");
        String sql = "INSERT INTO order_items (order_id, drink_id, drink_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Order 1: John Doe at Nakuru
            stmt.setLong(1, 1); stmt.setLong(2, 1); stmt.setString(3, "Soda"); stmt.setInt(4, 2); stmt.setDouble(5, 50.0); stmt.addBatch();
            stmt.setLong(1, 1); stmt.setLong(2, 3); stmt.setString(3, "Water"); stmt.setInt(4, 1); stmt.setDouble(5, 30.0); stmt.addBatch();
            
            // Order 2: Jane Smith at Mombasa
            stmt.setLong(1, 2); stmt.setLong(2, 2); stmt.setString(3, "Milk"); stmt.setInt(4, 1); stmt.setDouble(5, 70.0); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setLong(2, 4); stmt.setString(3, "Alcohol"); stmt.setInt(4, 1); stmt.setDouble(5, 150.0); stmt.addBatch();
            
            // Order 3: Mike Johnson at Kisumu
            stmt.setLong(1, 3); stmt.setLong(2, 1); stmt.setString(3, "Soda"); stmt.setInt(4, 3); stmt.setDouble(5, 50.0); stmt.addBatch();
            stmt.setLong(1, 3); stmt.setLong(2, 5); stmt.setString(3, "Juice"); stmt.setInt(4, 2); stmt.setDouble(5, 80.0); stmt.addBatch();
            
            // Order 4: John Doe at Nairobi
            stmt.setLong(1, 4); stmt.setLong(2, 6); stmt.setString(3, "Coffee"); stmt.setInt(4, 2); stmt.setDouble(5, 120.0); stmt.addBatch();
            stmt.setLong(1, 4); stmt.setLong(2, 10); stmt.setString(3, "Iced Coffee"); stmt.setInt(4, 1); stmt.setDouble(5, 140.0); stmt.addBatch();
            
            // Order 5: Jane Smith at Nakuru
            stmt.setLong(1, 5); stmt.setLong(2, 7); stmt.setString(3, "Tea"); stmt.setInt(4, 1); stmt.setDouble(5, 60.0); stmt.addBatch();
            stmt.setLong(1, 5); stmt.setLong(2, 8); stmt.setString(3, "Energy Drink"); stmt.setInt(4, 1); stmt.setDouble(5, 200.0); stmt.addBatch();
            
            // Order 6: Mike Johnson at Mombasa
            stmt.setLong(1, 6); stmt.setLong(2, 9); stmt.setString(3, "Smoothie"); stmt.setInt(4, 2); stmt.setDouble(5, 180.0); stmt.addBatch();
            stmt.setLong(1, 6); stmt.setLong(2, 12); stmt.setString(3, "Fresh Lemonade"); stmt.setInt(4, 1); stmt.setDouble(5, 90.0); stmt.addBatch();
            
            // Order 7: John Doe at Kisumu
            stmt.setLong(1, 7); stmt.setLong(2, 11); stmt.setString(3, "Hot Chocolate"); stmt.setInt(4, 1); stmt.setDouble(5, 110.0); stmt.addBatch();
            stmt.setLong(1, 7); stmt.setLong(2, 15); stmt.setString(3, "Herbal Tea"); stmt.setInt(4, 2); stmt.setDouble(5, 80.0); stmt.addBatch();
            
            // Order 8: Jane Smith at Nairobi
            stmt.setLong(1, 8); stmt.setLong(2, 13); stmt.setString(3, "Sports Drink"); stmt.setInt(4, 1); stmt.setDouble(5, 160.0); stmt.addBatch();
            stmt.setLong(1, 8); stmt.setLong(2, 16); stmt.setString(3, "Protein Shake"); stmt.setInt(4, 1); stmt.setDouble(5, 250.0); stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void updateOrderTotals(Connection conn) throws SQLException {
        logger.info("Updating order totals...");
        String sql = "UPDATE orders o SET total_amount = (SELECT SUM(oi.quantity * oi.unit_price) FROM order_items oi WHERE oi.order_id = o.id)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    private void seedNotifications(Connection conn) throws SQLException {
        logger.info("Seeding notifications...");
        String sql = "INSERT INTO notifications (user_id, title, message, type, priority) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, 1); stmt.setString(2, "Welcome!"); stmt.setString(3, "Welcome to the Drink Business RMI System"); stmt.setString(4, "INFO"); stmt.setString(5, "NORMAL"); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setString(2, "Order Confirmed"); stmt.setString(3, "Your order has been confirmed and is being processed"); stmt.setString(4, "ORDER_CONFIRMED"); stmt.setString(5, "HIGH"); stmt.addBatch();
            stmt.setLong(1, 3); stmt.setString(2, "Stock Alert"); stmt.setString(3, "Low stock alert for Soda at Kisumu branch"); stmt.setString(4, "STOCK_LOW"); stmt.setString(5, "HIGH"); stmt.addBatch();
            stmt.setLong(1, 4); stmt.setString(2, "System Update"); stmt.setString(3, "System maintenance scheduled for tonight"); stmt.setString(4, "SYSTEM_ALERT"); stmt.setString(5, "NORMAL"); stmt.addBatch();
            stmt.setLong(1, 5); stmt.setString(2, "New Drinks Added"); stmt.setString(3, "Check out our new smoothies and protein shakes!"); stmt.setString(4, "PRODUCT_UPDATE"); stmt.setString(5, "NORMAL"); stmt.addBatch();
            stmt.setLong(1, 6); stmt.setString(2, "Inventory Update"); stmt.setString(3, "Stock levels have been updated across all branches"); stmt.setString(4, "INVENTORY_UPDATE"); stmt.setString(5, "NORMAL"); stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void seedPayments(Connection conn) throws SQLException {
        logger.info("Seeding sample payments...");
        String sql = "INSERT INTO payments (order_id, customer_id, amount, payment_method, transaction_id, status) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Sample payments for some orders
            stmt.setLong(1, 1); stmt.setString(2, "1"); stmt.setDouble(3, 130.0); stmt.setString(4, "CREDIT_CARD"); stmt.setString(5, "TXN_001"); stmt.setString(6, "SUCCESS"); stmt.addBatch();
            stmt.setLong(1, 2); stmt.setString(2, "2"); stmt.setDouble(3, 220.0); stmt.setString(4, "MPESA"); stmt.setString(5, "TXN_002"); stmt.setString(6, "SUCCESS"); stmt.addBatch();
            stmt.setLong(1, 3); stmt.setString(2, "3"); stmt.setDouble(3, 310.0); stmt.setString(4, "CASH"); stmt.setString(5, "TXN_003"); stmt.setString(6, "PENDING"); stmt.addBatch();
            
            stmt.executeBatch();
        }
    }
    
    private void updateStockQuantities(Connection conn) throws SQLException {
        logger.info("Updating stock quantities based on orders...");
        
        try (Statement stmt = conn.createStatement()) {
            // Update stock quantities based on orders
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 1 AND drink_id = 1"); // Nakuru Soda
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 3"); // Nakuru Water
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 2"); // Mombasa Milk
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 4"); // Mombasa Alcohol
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 3 WHERE branch_id = 3 AND drink_id = 1"); // Kisumu Soda
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 3 AND drink_id = 5"); // Kisumu Juice
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 4 AND drink_id = 6"); // Nairobi Coffee
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 10"); // Nairobi Iced Coffee
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 7"); // Nakuru Tea
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 1 AND drink_id = 8"); // Nakuru Energy Drink
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 2 AND drink_id = 9"); // Mombasa Smoothie
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 2 AND drink_id = 12"); // Mombasa Fresh Lemonade
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 3 AND drink_id = 11"); // Kisumu Hot Chocolate
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 2 WHERE branch_id = 3 AND drink_id = 15"); // Kisumu Herbal Tea
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 13"); // Nairobi Sports Drink
            stmt.executeUpdate("UPDATE stocks SET quantity = quantity - 1 WHERE branch_id = 4 AND drink_id = 16"); // Nairobi Protein Shake
            
            // Set some items to low stock for testing alerts
            stmt.executeUpdate("UPDATE stocks SET quantity = 8 WHERE branch_id = 1 AND drink_id = 4"); // Nakuru Alcohol low
            stmt.executeUpdate("UPDATE stocks SET quantity = 5 WHERE branch_id = 2 AND drink_id = 1"); // Mombasa Soda low
            stmt.executeUpdate("UPDATE stocks SET quantity = 2 WHERE branch_id = 3 AND drink_id = 8"); // Kisumu Energy Drink very low
            stmt.executeUpdate("UPDATE stocks SET quantity = 6 WHERE branch_id = 4 AND drink_id = 16"); // Nairobi Protein Shake low
        }
    }
}
