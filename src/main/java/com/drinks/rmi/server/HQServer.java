package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.interfaces.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.util.Map;

/**
 * Headquarters RMI Server
 * Provides global services for admin, customer support, auditor, and global manager roles
 */
public class HQServer {
    
    private static final Logger logger = LoggerFactory.getLogger(HQServer.class);
    private static final int RMI_PORT = 1099;
    // Get server host from system property, default to localhost for backward compatibility
    private static final String SERVER_HOST = System.getProperty("java.rmi.server.hostname", "localhost");
    private static final String SERVICE_VERSION = "v1";
    
    // SSL Configuration
    private static final String KEYSTORE_PATH = "config/hq-keystore.jks";
    private static final String TRUSTSTORE_PATH = "config/hq-keystore.jks";
    private static final String STORE_PASSWORD = "securepassword123";
    
    // Service instances
    private static AuthServiceImpl authService;
    private static DrinkServiceImpl drinkService;
    private static StockServiceImpl stockService;
    private static OrderServiceImpl orderService;
    private static ReportServiceImpl reportService;
    private static NotificationServiceImpl notificationService;
    private static LoadBalancerServiceImpl loadBalancerService;
    private static PaymentServiceImpl paymentService;
    
    public static void main(String[] args) {
        try {
            // Configure SSL
            System.setProperty("javax.net.ssl.keyStore", KEYSTORE_PATH);
            System.setProperty("javax.net.ssl.keyStorePassword", STORE_PASSWORD);
            System.setProperty("javax.net.ssl.trustStore", TRUSTSTORE_PATH);
            System.setProperty("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
            
            logger.info("Starting HQ RMI Server with SSL encryption...");
            
            // Test database connection
            if (!DatabaseConfig.testConnection()) {
                logger.error("Database connection failed. Please check your database configuration.");
                System.exit(1);
            }
            
            // Create or get RMI registry with SSL
            Registry registry;
            try {
                // Try to get existing registry first
                registry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT);
                registry.list(); // Test if registry is accessible
                logger.info("Connected to existing RMI registry on port {}", RMI_PORT);
            } catch (Exception e) {
                // Create new registry if none exists
                logger.info("Creating new RMI registry on port {}", RMI_PORT);
                registry = LocateRegistry.createRegistry(RMI_PORT,
                    new SslRMIClientSocketFactory(),
                    new SslRMIServerSocketFactory());
                logger.info("RMI Registry created on port {}", RMI_PORT);
            }
            
            // Create service implementations (they auto-export since they extend UnicastRemoteObject)
            logger.info("Creating service implementations...");
            authService = new AuthServiceImpl();
            drinkService = new DrinkServiceImpl();
            stockService = new StockServiceImpl();
            orderService = new OrderServiceImpl(stockService, drinkService);
            reportService = new ReportServiceImpl();
            notificationService = new NotificationServiceImpl();
            loadBalancerService = new LoadBalancerServiceImpl();
            paymentService = new PaymentServiceImpl();
            
            logger.info("All services created and auto-exported successfully");
            
            // Bind services with retry logic
            Map<String, Remote> services = Map.of(
                "HQ_AuthService", authService,
                "HQ_DrinkService", drinkService,
                "HQ_StockService", stockService,
                "HQ_OrderService", orderService,
                "HQ_ReportService", reportService,
                "HQ_NotificationService", notificationService,
                "HQ_LoadBalancerService", loadBalancerService,
                "HQ_PaymentService", paymentService
            );
            
            for (Map.Entry<String, Remote> entry : services.entrySet()) {
                int bindAttempts = 0;
                boolean bound = false;
                while (!bound && bindAttempts < 3) {
                    try {
                        registry.bind(entry.getKey(), entry.getValue());
                        bound = true;
                        logger.info("Bound service: {}", entry.getKey());
                    } catch (Exception e) {
                        bindAttempts++;
                        logger.warn("Failed to bind {} (attempt {}/3): {}", 
                            entry.getKey(), bindAttempts, e.getMessage());
                        if (bindAttempts >= 3) {
                            logger.error("Failed to bind service {} after {} attempts", 
                                entry.getKey(), bindAttempts);
                            System.exit(1);
                        }
                        Thread.sleep(1000);
                    }
                }
            }
            
            logger.info("All services bound successfully");
            
            String baseUrl = "rmi://" + SERVER_HOST + ":" + RMI_PORT + "/";
            
            logger.info("Secure HQ RMI Server services bound successfully:");
            logger.info("  - AuthService: {}", baseUrl + "HQ_AuthService");
            logger.info("  - DrinkService: {}", baseUrl + "HQ_DrinkService");
            logger.info("  - StockService: {}", baseUrl + "HQ_StockService");
            logger.info("  - OrderService: {}", baseUrl + "HQ_OrderService");
            logger.info("  - ReportService: {}", baseUrl + "HQ_ReportService");
            logger.info("  - NotificationService: {}", baseUrl + "HQ_NotificationService");
            logger.info("  - LoadBalancerService: {}", baseUrl + "HQ_LoadBalancerService");
            logger.info("  - PaymentService: {}", baseUrl + "HQ_PaymentService");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down HQ RMI Server...");
                try {
                    if (reportService != null) {
                        UnicastRemoteObject.unexportObject(reportService, true);
                    }
                    if (notificationService != null) {
                        notificationService.shutdown();
                        UnicastRemoteObject.unexportObject(notificationService, true);
                    }
                    if (loadBalancerService != null) {
                        loadBalancerService.shutdown();
                        UnicastRemoteObject.unexportObject(loadBalancerService, true);
                    }
                    if (paymentService != null) {
                        UnicastRemoteObject.unexportObject(paymentService, true);
                    }
                    
                    // Use SSL registry for unbinding services
                    try {
                        Registry sslRegistry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT, new SslRMIClientSocketFactory());
                        sslRegistry.unbind("HQ_AuthService");
                        sslRegistry.unbind("HQ_DrinkService");
                        sslRegistry.unbind("HQ_StockService");
                        sslRegistry.unbind("HQ_OrderService");
                        sslRegistry.unbind("HQ_ReportService");
                        sslRegistry.unbind("HQ_NotificationService");
                        sslRegistry.unbind("HQ_LoadBalancerService");
                        sslRegistry.unbind("HQ_PaymentService");
                        logger.info("All services unbound from registry");
                    } catch (Exception e) {
                        logger.warn("Failed to unbind services from registry: {}", e.getMessage());
                    }
                    
                    // Close database connections
                    DatabaseConfig.close();
                    
                    logger.info("HQ RMI Server shutdown complete");
                } catch (Exception e) {
                    logger.error("Error during server shutdown", e);
                }
            }));
            
            logger.info("Secure HQ RMI Server is running and ready to accept connections...");
            logger.info("Press Ctrl+C to stop the server");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start secure HQ RMI Server", e);
            System.exit(1);
        }
    }
}
