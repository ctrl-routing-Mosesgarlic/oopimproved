package com.drinks.rmi.client.gui;

import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.interfaces.NotificationCallback;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import org.controlsfx.control.Notifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * Client-side implementation of NotificationCallback
 * Handles real-time notifications from the server
 */
public class NotificationCallbackImpl extends UnicastRemoteObject implements NotificationCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationCallbackImpl.class);
    private static final long serialVersionUID = 1L;
    
    private volatile boolean active = true;
    private Consumer<NotificationDTO> notificationHandler;
    private Label statusLabel;
    
    public NotificationCallbackImpl() throws RemoteException {
        super();
        logger.info("NotificationCallback initialized");
    }
    
    public NotificationCallbackImpl(Consumer<NotificationDTO> notificationHandler, Label statusLabel) throws RemoteException {
        this();
        this.notificationHandler = notificationHandler;
        this.statusLabel = statusLabel;
    }
    
    @Override
    public void onNotificationReceived(NotificationDTO notification) throws RemoteException {
        if (!active || notification == null) {
            return;
        }
        
        logger.info("Received notification: {}", notification.getTitle());
        
        // Update UI on JavaFX Application Thread
        Platform.runLater(() -> {
            try {
                // Update status label if available
                if (statusLabel != null) {
                    statusLabel.setText("📢 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: #28a745;");
                }
                
                // Show desktop notification using ControlsFX
                showDesktopNotification(notification);
                
                // Call custom notification handler if provided
                if (notificationHandler != null) {
                    notificationHandler.accept(notification);
                }
                
                // Show dialog for high priority notifications
                if (notification.getPriority() == NotificationDTO.NotificationPriority.HIGH ||
                    notification.getPriority() == NotificationDTO.NotificationPriority.URGENT) {
                    showHighPriorityDialog(notification);
                }
                
            } catch (Exception e) {
                logger.error("Error processing notification in UI", e);
            }
        });
    }
    
    @Override
    public boolean isActive() throws RemoteException {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
        logger.info("NotificationCallback active status set to: {}", active);
    }
    
    public void setNotificationHandler(Consumer<NotificationDTO> handler) {
        this.notificationHandler = handler;
    }
    
    public void setStatusLabel(Label statusLabel) {
        this.statusLabel = statusLabel;
    }
    
    private void showDesktopNotification(NotificationDTO notification) {
        try {
            // Use ControlsFX for desktop notifications
            Notifications notificationBuilder = Notifications.create()
                .title(notification.getTitle())
                .text(notification.getMessage())
                .hideAfter(javafx.util.Duration.seconds(5));
            
            // Set notification style based on type
            switch (notification.getType()) {
                case ORDER_CONFIRMED:
                case ORDER_DISPATCHED:
                    notificationBuilder.graphic(null); // Could add custom icons
                    break;
                case STOCK_LOW:
                case STOCK_OUT:
                    notificationBuilder.graphic(null);
                    break;
                case ERROR:
                    notificationBuilder.graphic(null);
                    break;
                case WARNING:
                    notificationBuilder.graphic(null);
                    break;
                default:
                    notificationBuilder.graphic(null);
                    break;
            }
            
            notificationBuilder.show();
            
        } catch (Exception e) {
            logger.warn("Failed to show desktop notification, falling back to console", e);
            System.out.println("NOTIFICATION: " + notification.getDisplayMessage());
        }
    }
    
    private void showHighPriorityDialog(NotificationDTO notification) {
        Alert alert = new Alert(getAlertType(notification.getType()));
        alert.setTitle("Important Notification");
        alert.setHeaderText(notification.getTitle());
        alert.setContentText(notification.getMessage());
        
        // Show dialog without blocking
        alert.show();
    }
    
    private Alert.AlertType getAlertType(NotificationDTO.NotificationType type) {
        switch (type) {
            case ERROR:
                return Alert.AlertType.ERROR;
            case WARNING:
            case STOCK_LOW:
            case STOCK_OUT:
                return Alert.AlertType.WARNING;
            case ORDER_CONFIRMED:
            case ORDER_DISPATCHED:
                return Alert.AlertType.INFORMATION;
            case SYSTEM_ALERT:
                return Alert.AlertType.WARNING;
            default:
                return Alert.AlertType.INFORMATION;
        }
    }
    
    public void shutdown() {
        active = false;
        try {
            // Unexport the remote object
            UnicastRemoteObject.unexportObject(this, true);
            logger.info("NotificationCallback shutdown completed");
        } catch (Exception e) {
            logger.warn("Error during NotificationCallback shutdown", e);
        }
    }
}
