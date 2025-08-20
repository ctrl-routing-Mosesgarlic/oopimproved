package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.interfaces.AuthService;
import com.drinks.rmi.interfaces.DrinkService;
import com.drinks.rmi.interfaces.OrderService;
import com.drinks.rmi.interfaces.PaymentService;
import com.drinks.rmi.interfaces.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Branch RMI Server
 * Provides local services for branch manager, staff, and customer roles
 */
public class BranchServer {
    
    private static final Logger logger = LoggerFactory.getLogger(BranchServer.class);
    // Get server host from system property, default to localhost for backward compatibility
    private static final String SERVER_HOST = System.getProperty("java.rmi.server.hostname", "localhost");
    
    private final String branchName;
    private final Long branchId;
    private final int rmiPort;
    
    public BranchServer(String branchName, int rmiPort) {
        this.branchName = branchName;
        this.rmiPort = rmiPort;
        this.branchId = getBranchId(branchName);
        
        if (this.branchId == null) {
            throw new RuntimeException("Branch not found: " + branchName);
        }
    }
    
    public void start() {
        try {
            logger.info("Starting {} Branch RMI Server on port {}...", branchName, rmiPort);
            
            // Test database connection
            if (!DatabaseConfig.testConnection()) {
                logger.error("Database connection failed. Please check your database configuration.");
                System.exit(1);
            }
            
            // Create RMI registry
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            logger.info("RMI Registry created on port {}", rmiPort);
            
            // Create service implementations
            AuthService authService = new AuthServiceImpl();
            DrinkService drinkService = new DrinkServiceImpl();
            StockService stockService = new StockServiceImpl();
            OrderService orderService = new OrderServiceImpl(stockService, drinkService);
            PaymentService paymentService = new PaymentServiceImpl();
            
            // Bind services to registry with branch-specific names
            String baseUrl = "rmi://" + SERVER_HOST + ":" + rmiPort + "/";
            String branchPrefix = branchName.toUpperCase() + "_";
            
            Naming.rebind(baseUrl + branchPrefix + "AuthService", authService);
            Naming.rebind(baseUrl + branchPrefix + "DrinkService", drinkService);
            Naming.rebind(baseUrl + branchPrefix + "StockService", stockService);
            Naming.rebind(baseUrl + branchPrefix + "OrderService", orderService);
            Naming.rebind(baseUrl + branchPrefix + "PaymentService", paymentService);
            
            logger.info("{} Branch RMI Server services bound successfully:", branchName);
            logger.info("  - AuthService: {}", baseUrl + branchPrefix + "AuthService");
            logger.info("  - DrinkService: {}", baseUrl + branchPrefix + "DrinkService");
            logger.info("  - StockService: {}", baseUrl + branchPrefix + "StockService");
            logger.info("  - OrderService: {}", baseUrl + branchPrefix + "OrderService");
            logger.info("  - PaymentService: {}", baseUrl + branchPrefix + "PaymentService");
            
            // Initialize stock for this branch if not exists
            initializeBranchStock();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down {} Branch RMI Server...", branchName);
                try {
                    // Unbind services
                    Naming.unbind(baseUrl + branchPrefix + "AuthService");
                    Naming.unbind(baseUrl + branchPrefix + "DrinkService");
                    Naming.unbind(baseUrl + branchPrefix + "StockService");
                    Naming.unbind(baseUrl + branchPrefix + "OrderService");
                    Naming.unbind(baseUrl + branchPrefix + "PaymentService");
                    
                    logger.info("{} Branch RMI Server shutdown complete", branchName);
                } catch (Exception e) {
                    logger.error("Error during server shutdown", e);
                }
            }));
            
            logger.info("{} Branch RMI Server is running and ready to accept connections...", branchName);
            logger.info("Branch ID: {}, Port: {}", branchId, rmiPort);
            logger.info("Press Ctrl+C to stop the server");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start {} Branch RMI Server", branchName, e);
            System.exit(1);
        }
    }
    
    private Long getBranchId(String branchName) {
        String sql = "SELECT id FROM branches WHERE name = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, branchName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("id");
            }
            return null;
            
        } catch (SQLException e) {
            logger.error("Error getting branch ID for: {}", branchName, e);
            return null;
        }
    }
    
    private void initializeBranchStock() {
        try {
            logger.info("Initializing stock for {} branch...", branchName);
            
            // Get all drinks
            String drinkSql = "SELECT id FROM drinks";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement drinkStmt = conn.prepareStatement(drinkSql)) {
                
                ResultSet drinkRs = drinkStmt.executeQuery();
                
                while (drinkRs.next()) {
                    Long drinkId = drinkRs.getLong("id");
                    
                    // Check if stock exists for this branch and drink
                    String stockCheckSql = "SELECT id FROM stocks WHERE branch_id = ? AND drink_id = ?";
                    PreparedStatement stockCheckStmt = conn.prepareStatement(stockCheckSql);
                    stockCheckStmt.setLong(1, branchId);
                    stockCheckStmt.setLong(2, drinkId);
                    
                    ResultSet stockRs = stockCheckStmt.executeQuery();
                    
                    if (!stockRs.next()) {
                        // Stock doesn't exist, create it with default quantity
                        String insertStockSql = "INSERT INTO stocks (branch_id, drink_id, quantity) VALUES (?, ?, ?)";
                        PreparedStatement insertStmt = conn.prepareStatement(insertStockSql);
                        insertStmt.setLong(1, branchId);
                        insertStmt.setLong(2, drinkId);
                        insertStmt.setInt(3, 100); // Default initial stock
                        
                        insertStmt.executeUpdate();
                        logger.debug("Initialized stock for drink ID {} at {} branch", drinkId, branchName);
                    }
                }
                
                logger.info("Stock initialization completed for {} branch", branchName);
                
            }
            
        } catch (SQLException e) {
            logger.error("Error initializing stock for {} branch", branchName, e);
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java BranchServer <branch_name> <rmi_port>");
            System.err.println("Example: java BranchServer Nakuru 1100");
            System.err.println("Available branches: Nakuru, Mombasa, Kisumu, Nairobi");
            System.err.println("Suggested ports: Nakuru=1100, Mombasa=1101, Kisumu=1102, Nairobi=1103");
            System.exit(1);
        }
        
        String branchName = args[0];
        int rmiPort;
        
        try {
            rmiPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            System.exit(1);
            return;
        }
        
        // Validate branch name
        if (!isValidBranch(branchName)) {
            System.err.println("Invalid branch name: " + branchName);
            System.err.println("Available branches: Nakuru, Mombasa, Kisumu, Nairobi");
            System.exit(1);
        }
        
        BranchServer server = new BranchServer(branchName, rmiPort);
        server.start();
    }
    
    private static boolean isValidBranch(String branchName) {
        return branchName.equalsIgnoreCase("Nakuru") ||
               branchName.equalsIgnoreCase("Mombasa") ||
               branchName.equalsIgnoreCase("Kisumu") ||
               branchName.equalsIgnoreCase("Nairobi");
    }
}
