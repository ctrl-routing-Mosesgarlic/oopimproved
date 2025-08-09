package com.drinks.rmi.client.gui;

import com.drinks.rmi.client.gui.controller.*;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.AuthService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Routes users to appropriate dashboards based on their roles
 */
public class DashboardRouter {
    private static final Logger logger = LoggerFactory.getLogger(DashboardRouter.class);
    
    // FXML file paths for different role dashboards
    private static final Map<String, String> DASHBOARD_FXML_PATHS = new HashMap<>();
    
    static {
        // Initialize dashboard paths for each role
        DASHBOARD_FXML_PATHS.put("admin", "/fxml/admin_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("customer_support", "/fxml/customer_support_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("auditor", "/fxml/auditor_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("global_manager", "/fxml/global_manager_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("branch_manager", "/fxml/branch_manager_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("branch_staff", "/fxml/branch_staff_dashboard.fxml");
        DASHBOARD_FXML_PATHS.put("customer", "/fxml/customer_dashboard.fxml");
    }
    
    /**
     * Routes the user to the appropriate dashboard based on their role
     * @param stage The JavaFX stage
     * @param user The authenticated user
     * @throws IOException If the FXML file cannot be loaded
     */
    public static void routeToDashboard(Stage stage, UserDTO user, AuthService authService, String serverInfo) throws IOException {
        String role = user.getRole().toLowerCase();
        logger.info("Routing user {} with role {} to dashboard", user.getUsername(), role);
        
        String fxmlPath = DASHBOARD_FXML_PATHS.get(role);
        if (fxmlPath == null) {
            logger.error("No dashboard defined for role: {}", role);
            throw new IllegalArgumentException("No dashboard defined for role: " + role);
        }
        
        FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource(fxmlPath));
        Parent root = loader.load();
        
        // Get controller and initialize with user
        Object controller = loader.getController();
        if (controller instanceof DashboardController) {
            ((DashboardController) controller).setUserData(user, authService, serverInfo);
        } else {
            logger.error("Controller does not implement DashboardController interface");
            throw new IllegalStateException("Controller does not implement DashboardController interface");
        }
        
        // Set up the scene
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle(formatTitle(role) + " Dashboard - Drink Business");
        stage.setMaximized(true);
        stage.show();
    }
    
    /**
     * Format role for display in window title
     */
    private static String formatTitle(String role) {
        String[] parts = role.split("_");
        StringBuilder title = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                title.append(Character.toUpperCase(part.charAt(0)))
                     .append(part.substring(1))
                     .append(" ");
            }
        }
        
        return title.toString().trim();
    }
}
