package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.client.gui.DashboardRouter;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.AuthService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
 * Controller for the Login Screen
 * Handles user authentication and server connection
 */
public class LoginController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static final Map<String, AuthService> authServicePool = new ConcurrentHashMap<>();
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> serverComboBox;
    @FXML private ComboBox<String> branchComboBox;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox loginForm;
    
    private AuthService authService;
    private UserDTO currentUser;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Login Controller");
        
        // Initialize server options
        serverComboBox.getItems().addAll("Headquarters (HQ)", "Branch Server");
        serverComboBox.setValue("Headquarters (HQ)");
        
        // Initialize branch options (disabled initially)
        branchComboBox.getItems().addAll("Nakuru", "Mombasa", "Kisumu", "Nairobi");
        branchComboBox.setValue("Nakuru");
        branchComboBox.setDisable(true);
        
        // Set up server selection listener
        serverComboBox.setOnAction(event -> {
            String selected = serverComboBox.getValue();
            branchComboBox.setDisable(!"Branch Server".equals(selected));
        });
        
        // Set up login button action
        loginButton.setOnAction(event -> handleLogin());
        
        // Enable Enter key for login
        loginForm.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER")) {
                handleLogin();
            }
        });
        
        progressIndicator.setVisible(false);
        statusLabel.setText("Please enter your credentials");
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }
        
        // Disable form during login
        setFormEnabled(false);
        progressIndicator.setVisible(true);
        statusLabel.setText("Connecting to server...");
        
        // Perform login in background thread
        Task<UserDTO> loginTask = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                return performLogin(username, password);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    currentUser = getValue();
                    if (currentUser != null) {
                        logger.info("Login successful for user: {}", currentUser.getUsername());
                        openDashboard();
                    } else {
                        showError("Invalid username or password");
                        setFormEnabled(true);
                        progressIndicator.setVisible(false);
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Login failed", getException());
                    showError("Connection failed: " + getException().getMessage());
                    setFormEnabled(true);
                    progressIndicator.setVisible(false);
                });
            }
        };
        
        Thread loginThread = new Thread(loginTask);
        loginThread.setDaemon(true);
        loginThread.start();
    }
    
    private AuthService getAuthService(String host, int port) throws RemoteException {
        String key = host + ":" + port;
        
        return authServicePool.computeIfAbsent(key, k -> {
            try {
                Registry registry;
                String serviceName;
                
                if (port == 1099) {
                    // HQ server uses SSL
                    RMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
                    registry = LocateRegistry.getRegistry(host, port, socketFactory);
                    serviceName = "HQ_AuthService";
                } else {
                    // Branch servers use regular RMI with branch-specific service names
                    registry = LocateRegistry.getRegistry(host, port);
                    String branchName = getBranchNameByPort(port);
                    serviceName = branchName.toUpperCase() + "_AuthService";
                }
                
                return (AuthService) registry.lookup(serviceName);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create AuthService connection", e);
            }
        });
    }
    
    private UserDTO performLogin(String username, String password) throws Exception {
        String serverType = serverComboBox.getValue();
        int port;
        
        if ("Headquarters (HQ)".equals(serverType)) {
            port = 1099;
        } else {
            // Branch server
            String branch = branchComboBox.getValue();
            switch (branch) {
                case "Nakuru": port = 1100; break;
                case "Mombasa": port = 1101; break;
                case "Kisumu": port = 1102; break;
                case "Nairobi": port = 1103; break;
                default: throw new IllegalArgumentException("Invalid branch: " + branch);
            }
        }
        
        // Get server hostname from system property, default to localhost
        String serverHost = System.getProperty("rmi.server.host", "localhost");
        
        logger.info("Connecting to {} on {}:{}", serverType, serverHost, port);
        
        // Enhanced connection with retry logic
        int maxRetries = 3;
        int retryDelayMs = 1000;
        Exception lastError = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Get service from pool instead of creating new connection
                authService = getAuthService(serverHost, port);
                return authService.login(username, password);
                
            } catch (Exception e) {
                lastError = e;
                logger.warn("Connection attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // Exponential backoff
                }
            }
        }
        
        throw new RemoteException("Failed to connect after " + maxRetries + " attempts", lastError);
    }
    
    private void openDashboard() {
        try {
            // Get current stage and use DashboardRouter to route to appropriate dashboard
            Stage stage = (Stage) loginButton.getScene().getWindow();
            
            // Use DashboardRouter to route to the appropriate dashboard based on user role
            DashboardRouter.routeToDashboard(stage, currentUser, authService, getServerInfo());
            
            logger.info("Dashboard opened for user: {} with role: {}", currentUser.getUsername(), currentUser.getRole());
            
        } catch (Exception e) {
            logger.error("Failed to open dashboard", e);
            showError("Failed to open dashboard: " + e.getMessage());
            setFormEnabled(true);
            progressIndicator.setVisible(false);
        }
    }
    
    // getDashboardFXML method removed as we now use DashboardRouter
    
    private String getServerInfo() {
        String serverType = serverComboBox.getValue();
        if ("Branch Server".equals(serverType)) {
            return serverType + " (" + branchComboBox.getValue() + ")";
        }
        return serverType;
    }
    
    private void setFormEnabled(boolean enabled) {
        usernameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        serverComboBox.setDisable(!enabled);
        branchComboBox.setDisable(!enabled || !"Branch Server".equals(serverComboBox.getValue()));
        loginButton.setDisable(!enabled);
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }
    
    private String getBranchNameByPort(int port) {
        switch (port) {
            case 1100: return "Nakuru";
            case 1101: return "Mombasa";
            case 1102: return "Kisumu";
            case 1103: return "Nairobi";
            default: return "Branch";
        }
    }
}
