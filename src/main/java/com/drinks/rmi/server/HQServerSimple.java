package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.interfaces.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 * Simple HQ Server without SSL for testing
 */
public class HQServerSimple {
    
    private static final Logger logger = LoggerFactory.getLogger(HQServerSimple.class);
    private static final int RMI_PORT = 1099;
    // Get server host from system property, default to localhost for backward compatibility
    private static final String SERVER_HOST = System.getProperty("java.rmi.server.hostname", "localhost");
    
    // Service instances
    private static AuthService authService;
    private static DrinkService drinkService;
    private static StockService stockService;
    private static OrderService orderService;
    private static ReportService reportService;
    private static NotificationService notificationService;
    private static LoadBalancerService loadBalancerService;
    
    public static void main(String[] args) {
        try {
            logger.info("Starting Simple HQ RMI Server...");
            
            // Test database connection
            if (!DatabaseConfig.testConnection()) {
                logger.error("Database connection failed. Please check your database configuration.");
                System.exit(1);
            }
            
            // Get existing registry (should be started externally)
            Registry registry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT);
            logger.info("Connected to RMI registry on port {}", RMI_PORT);
            
            // Create service implementations (they auto-export since they extend UnicastRemoteObject)
            logger.info("Creating service implementations...");
            authService = new AuthServiceImpl();
            drinkService = new DrinkServiceImpl();
            stockService = new StockServiceImpl();
            orderService = new OrderServiceImpl(stockService, drinkService);
            reportService = new ReportServiceImpl();
            notificationService = new NotificationServiceImpl();
            loadBalancerService = new LoadBalancerServiceImpl();
            
            logger.info("All services exported successfully");
            
            // Bind services to registry
            Map<String, Remote> services = Map.of(
                "HQ_AuthService", authService,
                "HQ_DrinkService", drinkService,
                "HQ_StockService", stockService,
                "HQ_OrderService", orderService,
                "HQ_ReportService", reportService,
                "HQ_NotificationService", notificationService,
                "LoadBalancerService", loadBalancerService
            );
            
            for (Map.Entry<String, Remote> entry : services.entrySet()) {
                try {
                    registry.bind(entry.getKey(), entry.getValue());
                    logger.info("Bound service: {}", entry.getKey());
                } catch (Exception e) {
                    logger.warn("Service {} might already be bound, trying rebind: {}", entry.getKey(), e.getMessage());
                    try {
                        registry.rebind(entry.getKey(), entry.getValue());
                        logger.info("Rebound service: {}", entry.getKey());
                    } catch (Exception rebindEx) {
                        logger.error("Failed to rebind service {}: {}", entry.getKey(), rebindEx.getMessage());
                        throw rebindEx;
                    }
                }
            }
            
            logger.info("All services bound successfully");
            
            String baseUrl = "rmi://" + SERVER_HOST + ":" + RMI_PORT + "/";
            logger.info("Simple HQ RMI Server services available at:");
            logger.info("  - AuthService: {}", baseUrl + "HQ_AuthService");
            logger.info("  - DrinkService: {}", baseUrl + "HQ_DrinkService");
            logger.info("  - StockService: {}", baseUrl + "HQ_StockService");
            logger.info("  - OrderService: {}", baseUrl + "HQ_OrderService");
            logger.info("  - ReportService: {}", baseUrl + "HQ_ReportService");
            logger.info("  - NotificationService: {}", baseUrl + "HQ_NotificationService");
            logger.info("  - LoadBalancerService: {}", baseUrl + "LoadBalancerService");
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down Simple HQ RMI Server...");
                try {
                    // Unbind services
                    for (String serviceName : services.keySet()) {
                        try {
                            registry.unbind(serviceName);
                            logger.info("Unbound service: {}", serviceName);
                        } catch (Exception e) {
                            logger.warn("Failed to unbind {}: {}", serviceName, e.getMessage());
                        }
                    }
                    
                    // Unexport objects
                    if (authService != null) UnicastRemoteObject.unexportObject(authService, true);
                    if (drinkService != null) UnicastRemoteObject.unexportObject(drinkService, true);
                    if (stockService != null) UnicastRemoteObject.unexportObject(stockService, true);
                    if (orderService != null) UnicastRemoteObject.unexportObject(orderService, true);
                    if (reportService != null) UnicastRemoteObject.unexportObject(reportService, true);
                    if (notificationService != null) {
                        ((NotificationServiceImpl) notificationService).shutdown();
                        UnicastRemoteObject.unexportObject(notificationService, true);
                    }
                    if (loadBalancerService != null) {
                        ((LoadBalancerServiceImpl) loadBalancerService).shutdown();
                        UnicastRemoteObject.unexportObject(loadBalancerService, true);
                    }
                    
                    // Close database connections
                    DatabaseConfig.close();
                    
                    logger.info("Simple HQ RMI Server shutdown complete");
                } catch (Exception e) {
                    logger.error("Error during server shutdown", e);
                }
            }));
            
            logger.info("Simple HQ RMI Server is running and ready to accept connections...");
            logger.info("Press Ctrl+C to stop the server");
            
            // Keep the server running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start Simple HQ RMI Server", e);
            System.exit(1);
        }
    }
}
