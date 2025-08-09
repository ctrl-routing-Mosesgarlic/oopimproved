package com.drinks.rmi.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for notifications
 * Used for sending notification data over RMI
 */
public class NotificationDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationPriority priority;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String actionUrl; // Optional URL for action buttons
    private String metadata; // Optional JSON metadata
    
    // Constructors
    public NotificationDTO() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.priority = NotificationPriority.NORMAL;
    }
    
    public NotificationDTO(String title, String message, NotificationType type) {
        this();
        this.title = title;
        this.message = message;
        this.type = type;
    }
    
    public NotificationDTO(Long userId, String title, String message, NotificationType type) {
        this(title, message, type);
        this.userId = userId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public NotificationPriority getPriority() {
        return priority;
    }
    
    public void setPriority(NotificationPriority priority) {
        this.priority = priority;
    }
    
    public boolean isRead() {
        return isRead;
    }
    
    public void setRead(boolean read) {
        isRead = read;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getActionUrl() {
        return actionUrl;
    }
    
    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    // Utility methods
    public String getFormattedTime() {
        if (createdAt != null) {
            return createdAt.toString();
        }
        return "";
    }
    
    public String getDisplayMessage() {
        return String.format("[%s] %s: %s", 
            type != null ? type.name() : "INFO", 
            title != null ? title : "Notification", 
            message != null ? message : "");
    }
    
    @Override
    public String toString() {
        return "NotificationDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", isRead=" + isRead +
                ", createdAt=" + createdAt +
                '}';
    }
    
    // Enums for notification types and priorities
    public enum NotificationType {
        ORDER_CONFIRMED,
        ORDER_DISPATCHED,
        ORDER_CANCELLED,
        ORDER_UPDATE,
        STOCK_LOW,
        STOCK_OUT,
        STOCK_UPDATED,
        STOCK_UPDATE,
        STOCK_REQUEST,
        USER_REGISTERED,
        SYSTEM_ALERT,
        PROMOTION,
        INFO,
        WARNING,
        ERROR,
        CUSTOMER_ISSUE
    }
    
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}
