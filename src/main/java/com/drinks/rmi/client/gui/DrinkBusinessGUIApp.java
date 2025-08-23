package com.drinks.rmi.client.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main JavaFX Application for Drink Business RMI System
 * Entry point for the GUI client application
 */
public class DrinkBusinessGUIApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(DrinkBusinessGUIApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            logger.info("Starting Drink Business GUI Application...");
            
            // Load the login screen FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            
            // Set up the primary stage
            primaryStage.setTitle("Drink Business RMI System - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setResizable(true);
            
            // Add application icon (optional)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
            } catch (Exception e) {
                logger.warn("Could not load application icon: {}", e.getMessage());
            }
            
            // Set up close operation
            primaryStage.setOnCloseRequest(event -> {
                logger.info("Application closing...");
                System.exit(0);
            });
            
            primaryStage.show();
            logger.info("GUI Application started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start GUI application", e);
            System.exit(1);
        }
    }
    
    public static void main(String[] args) {
        logger.info("Launching JavaFX GUI Application...");
        
        // Configure SSL properties for RMI connections
        System.setProperty("javax.net.ssl.trustStore", "config/hq-keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "securepassword123");
        System.setProperty("javax.net.ssl.keyStore", "config/hq-keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "securepassword123");
        
        // SSL configuration for development with self-signed certificates
        System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");
        
        // Allow users to specify HQ server IP via command line arguments or system properties
        String hqServerIP = System.getProperty("hq.server.ip");
        if (hqServerIP == null && args.length > 0) {
            // Check if first argument is an IP address for HQ server
            if (args[0].matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                hqServerIP = args[0];
                System.setProperty("hq.server.ip", hqServerIP);
                logger.info("Using HQ server IP from command line: {}", hqServerIP);
            }
        }
        
        // Set RMI hostname if HQ server IP is specified
        if (hqServerIP != null) {
            System.setProperty("java.rmi.server.hostname", hqServerIP);
            System.setProperty("rmi.server.host", hqServerIP);
            logger.info("Set RMI server hostname to: {}", hqServerIP);
        }
        
        launch(args);
    }
}
