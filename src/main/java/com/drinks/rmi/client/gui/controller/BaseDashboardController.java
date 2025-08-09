package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.interfaces.*;
import com.drinks.rmi.client.gui.NotificationCallbackImpl;
import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.UserDTO;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Base Dashboard Controller with common functionality for all user roles
 * Provides shared methods for service connections, notifications, and logout
 */
public abstract class BaseDashboardController implements DashboardController {
    
    protected static final Logger logger = LoggerFactory.getLogger(BaseDashboardController.class);
    
    // Common FXML elements
    @FXML protected Label welcomeLabel;
    @FXML protected Label serverInfoLabel;
    @FXML protected Button logoutButton;
    @FXML protected Label statusLabel;
    @FXML protected ProgressIndicator progressIndicator;
    
    // Common services
    protected UserDTO currentUser;
    protected AuthService authService;
    protected DrinkService drinkService;
    protected OrderService orderService;
    protected StockService stockService;
    protected ReportService reportService;
    protected NotificationService notificationService;
    protected LoadBalancerService loadBalancerService;
    protected NotificationCallbackImpl notificationCallback;
    protected String serverInfo;
    
    /**
     * Set user data and initialize services
     */
    @Override
    public void setUserData(UserDTO user, AuthService authService, String serverInfo) {
        this.currentUser = user;
        this.authService = authService;
        this.serverInfo = serverInfo;
        
        // Update UI
        welcomeLabel.setText("Welcome, " + user.getUsername() + " (" + formatRoleName(user.getRole()) + ")");
        serverInfoLabel.setText("Connected to: " + serverInfo);
        
        // Connect to services and initialize dashboard
        connectToServices();
        initializeDashboard();
        setupNotifications();
    }
    
    /**
     * Connect to RMI services
     */
    protected void connectToServices() {
        try {
            String[] parts = serverInfo.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            
            Registry registry = LocateRegistry.getRegistry(host, port);
            
            // Connect to services based on user role and location
            String prefix = "HQ_"; // Default to HQ
            
            // For branch-specific roles, use branch prefix
            if (currentUser.getBranchId() != null && 
                (currentUser.getRole().equals("branch_manager") || 
                 currentUser.getRole().equals("branch_staff"))) {
                prefix = currentUser.getBranchName().toUpperCase() + "_";
            }
            
            // Lookup services
            drinkService = (DrinkService) registry.lookup(prefix + "DrinkService");
            orderService = (OrderService) registry.lookup(prefix + "OrderService");
            stockService = (StockService) registry.lookup(prefix + "StockService");
            reportService = (ReportService) registry.lookup("HQ_ReportService"); // Reports always from HQ
            notificationService = (NotificationService) registry.lookup("HQ_NotificationService");
            loadBalancerService = (LoadBalancerService) registry.lookup("HQ_LoadBalancerService");
            
            logger.info("Connected to services with prefix: {}", prefix);
            
        } catch (Exception e) {
            logger.error("Failed to connect to services", e);
            showError("Failed to connect to services: " + e.getMessage());
        }
    }
    
    /**
     * Initialize dashboard specific to role
     */
    public abstract void initializeDashboard();
    
    /**
     * Setup notifications for real-time updates
     */
    protected void setupNotifications() {
        try {
            // Create notification callback
            notificationCallback = new NotificationCallbackImpl(
                this::handleNotification,
                statusLabel
            );
            
            // Register for notifications
            notificationService.registerCallback(currentUser.getId(), notificationCallback);
            
            // Send welcome notification
            NotificationDTO welcomeNotification = new NotificationDTO(
                formatRoleName(currentUser.getRole()) + " Connected",
                "You are now connected to real-time notifications",
                NotificationDTO.NotificationType.INFO
            );
            notificationService.sendNotification(currentUser.getId(), welcomeNotification);
            
            logger.info("Real-time notifications enabled for {}", currentUser.getRole());
            
        } catch (Exception e) {
            logger.error("Failed to setup notifications", e);
            statusLabel.setText("⚠️ Notifications unavailable");
        }
    }
    
    /**
     * Handle incoming notifications
     */
    protected void handleNotification(NotificationDTO notification) {
        logger.info("Received notification: {}", notification.getTitle());
        
        // Update UI based on notification type
        switch (notification.getType()) {
            case STOCK_LOW:
            case STOCK_OUT:
                Platform.runLater(() -> {
                    statusLabel.setText("📦 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: orange;");
                });
                break;
            case USER_REGISTERED:
                Platform.runLater(() -> {
                    statusLabel.setText("👤 " + notification.getTitle());
                });
                break;
            case ORDER_CONFIRMED:
                Platform.runLater(() -> {
                    statusLabel.setText("🛒 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: green;");
                });
                break;
            default:
                Platform.runLater(() -> {
                    statusLabel.setText("📢 " + notification.getTitle());
                });
                break;
        }
    }
    
    /**
     * Handle logout and cleanup
     */
    @Override
    public void handleLogout() {
        try {
            // Cleanup notifications
            if (notificationCallback != null) {
                notificationCallback.setActive(false);
                if (notificationService != null) {
                    notificationService.unregisterCallback(currentUser.getId());
                }
                notificationCallback.shutdown();
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene loginScene = new Scene(loader.load(), 800, 600);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Drink Business RMI System - Login");
            stage.setScene(loginScene);
            stage.setMaximized(false);
            stage.centerOnScreen();
            
            logger.info("User logged out successfully");
            
        } catch (Exception e) {
            logger.error("Failed to logout", e);
            showError("Failed to logout: " + e.getMessage());
        }
    }
    
    /**
     * Format role name for display
     */
    protected String formatRoleName(String role) {
        if (role == null) return "Unknown";
        
        String[] parts = role.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                formatted.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Show error message
     */
    protected void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: red;");
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
