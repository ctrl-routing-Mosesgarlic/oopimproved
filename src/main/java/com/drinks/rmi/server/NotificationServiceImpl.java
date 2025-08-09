package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.interfaces.NotificationCallback;
import com.drinks.rmi.interfaces.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of NotificationService with RMI callbacks
 * Handles real-time push notifications to registered clients
 */
public class NotificationServiceImpl extends UnicastRemoteObject implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final long serialVersionUID = 1L;
    
    // Map of user ID to their registered callback
    private final Map<Long, NotificationCallback> callbacks = new ConcurrentHashMap<>();
    
    // Scheduled executor for cleanup tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    public NotificationServiceImpl() throws RemoteException {
        super();
        logger.info("NotificationService initialized");
        
        // Start cleanup task to remove inactive callbacks
        scheduler.scheduleAtFixedRate(this::cleanupInactiveCallbacks, 30, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public void registerCallback(Long userId, NotificationCallback callback) throws RemoteException {
        if (userId == null || callback == null) {
            throw new IllegalArgumentException("User ID and callback cannot be null");
        }
        
        callbacks.put(userId, callback);
        logger.info("Registered notification callback for user: {}", userId);
        
        // Send welcome notification
        NotificationDTO welcomeNotification = new NotificationDTO(
            "Connected", 
            "You are now connected to real-time notifications", 
            NotificationDTO.NotificationType.INFO
        );
        sendNotification(userId, welcomeNotification);
    }
    
    @Override
    public void unregisterCallback(Long userId) throws RemoteException {
        if (userId != null) {
            callbacks.remove(userId);
            logger.info("Unregistered notification callback for user: {}", userId);
        }
    }
    
    @Override
    public void sendNotification(Long userId, NotificationDTO notification) throws RemoteException {
        if (userId == null || notification == null) {
            return;
        }
        
        // Store notification in database
        storeNotification(userId, notification);
        
        // Try to send via callback if user is connected
        NotificationCallback callback = callbacks.get(userId);
        if (callback != null) {
            try {
                callback.onNotificationReceived(notification);
                logger.debug("Sent real-time notification to user: {}", userId);
            } catch (RemoteException e) {
                logger.warn("Failed to send notification to user {}, removing callback", userId, e);
                callbacks.remove(userId);
            }
        } else {
            logger.debug("No active callback for user {}, notification stored in database", userId);
        }
    }
    
    @Override
    public void sendNotificationToRole(String role, NotificationDTO notification) throws RemoteException {
        if (role == null || notification == null) {
            return;
        }
        
        List<Long> userIds = getUserIdsByRole(role);
        logger.info("Sending notification to {} users with role: {}", userIds.size(), role);
        
        for (Long userId : userIds) {
            sendNotification(userId, notification);
        }
    }
    
    @Override
    public void sendNotificationToBranch(String branchName, NotificationDTO notification) throws RemoteException {
        if (branchName == null || notification == null) {
            return;
        }
        
        List<Long> userIds = getUserIdsByBranch(branchName);
        logger.info("Sending notification to {} users in branch: {}", userIds.size(), branchName);
        
        for (Long userId : userIds) {
            sendNotification(userId, notification);
        }
    }
    
    @Override
    public List<NotificationDTO> getNotifications(Long userId, int limit) throws RemoteException {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        List<NotificationDTO> notifications = new ArrayList<>();
        String sql = "SELECT id, user_id, title, message, type, priority, is_read, created_at, action_url, metadata " +
                    "FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setInt(2, Math.max(1, Math.min(limit, 100))); // Limit between 1 and 100
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    NotificationDTO notification = mapResultSetToNotification(rs);
                    notifications.add(notification);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to retrieve notifications for user: {}", userId, e);
            throw new RemoteException("Failed to retrieve notifications", e);
        }
        
        return notifications;
    }
    
    @Override
    public void markNotificationsAsRead(Long userId, List<Long> notificationIds) throws RemoteException {
        if (userId == null || notificationIds == null || notificationIds.isEmpty()) {
            return;
        }
        
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (Long notificationId : notificationIds) {
                stmt.setLong(1, userId);
                stmt.setLong(2, notificationId);
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            int updatedCount = 0;
            for (int result : results) {
                if (result > 0) updatedCount++;
            }
            
            logger.info("Marked {} notifications as read for user: {}", updatedCount, userId);
            
        } catch (SQLException e) {
            logger.error("Failed to mark notifications as read for user: {}", userId, e);
            throw new RemoteException("Failed to mark notifications as read", e);
        }
    }
    
    @Override
    public int getUnreadNotificationCount(Long userId) throws RemoteException {
        if (userId == null) {
            return 0;
        }
        
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get unread notification count for user: {}", userId, e);
            throw new RemoteException("Failed to get unread notification count", e);
        }
        
        return 0;
    }
    
    // Helper methods
    
    private void storeNotification(Long userId, NotificationDTO notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, type, priority, is_read, created_at, action_url, metadata) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, userId);
            stmt.setString(2, notification.getTitle());
            stmt.setString(3, notification.getMessage());
            stmt.setString(4, notification.getType() != null ? notification.getType().name() : "INFO");
            stmt.setString(5, notification.getPriority() != null ? notification.getPriority().name() : "NORMAL");
            stmt.setBoolean(6, notification.isRead());
            stmt.setTimestamp(7, Timestamp.valueOf(notification.getCreatedAt()));
            stmt.setString(8, notification.getActionUrl());
            stmt.setString(9, notification.getMetadata());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        notification.setId(generatedKeys.getLong(1));
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to store notification for user: {}", userId, e);
        }
    }
    
    private List<Long> getUserIdsByRole(String role) {
        List<Long> userIds = new ArrayList<>();
        String sql = "SELECT id FROM users WHERE role = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, role);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("id"));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get user IDs by role: {}", role, e);
        }
        
        return userIds;
    }
    
    private List<Long> getUserIdsByBranch(String branchName) {
        List<Long> userIds = new ArrayList<>();
        String sql = "SELECT u.id FROM users u JOIN branches b ON u.branch_id = b.id WHERE b.name = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, branchName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("id"));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to get user IDs by branch: {}", branchName, e);
        }
        
        return userIds;
    }
    
    private NotificationDTO mapResultSetToNotification(ResultSet rs) throws SQLException {
        NotificationDTO notification = new NotificationDTO();
        notification.setId(rs.getLong("id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try {
                notification.setType(NotificationDTO.NotificationType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                notification.setType(NotificationDTO.NotificationType.INFO);
            }
        }
        
        String priorityStr = rs.getString("priority");
        if (priorityStr != null) {
            try {
                notification.setPriority(NotificationDTO.NotificationPriority.valueOf(priorityStr));
            } catch (IllegalArgumentException e) {
                notification.setPriority(NotificationDTO.NotificationPriority.NORMAL);
            }
        }
        
        notification.setRead(rs.getBoolean("is_read"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        notification.setActionUrl(rs.getString("action_url"));
        notification.setMetadata(rs.getString("metadata"));
        
        return notification;
    }
    
    private void cleanupInactiveCallbacks() {
        List<Long> inactiveUsers = new ArrayList<>();
        
        for (Map.Entry<Long, NotificationCallback> entry : callbacks.entrySet()) {
            try {
                if (!entry.getValue().isActive()) {
                    inactiveUsers.add(entry.getKey());
                }
            } catch (RemoteException e) {
                inactiveUsers.add(entry.getKey());
            }
        }
        
        for (Long userId : inactiveUsers) {
            callbacks.remove(userId);
            logger.debug("Removed inactive callback for user: {}", userId);
        }
        
        if (!inactiveUsers.isEmpty()) {
            logger.info("Cleaned up {} inactive notification callbacks", inactiveUsers.size());
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("NotificationService shutdown completed");
    }
}
