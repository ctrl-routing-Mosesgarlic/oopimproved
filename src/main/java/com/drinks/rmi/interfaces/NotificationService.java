package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.drinks.rmi.dto.NotificationDTO;

/**
 * RMI Service for handling real-time notifications
 * Supports callback-based push notifications to clients
 */
public interface NotificationService extends Remote {
    
    /**
     * Register a client callback for receiving real-time notifications
     * @param userId User ID to register notifications for
     * @param callback Client callback interface
     * @throws RemoteException if RMI communication fails
     */
    void registerCallback(Long userId, NotificationCallback callback) throws RemoteException;
    
    /**
     * Unregister a client callback
     * @param userId User ID to unregister
     * @throws RemoteException if RMI communication fails
     */
    void unregisterCallback(Long userId) throws RemoteException;
    
    /**
     * Send a notification to a specific user
     * @param userId Target user ID
     * @param notification Notification to send
     * @throws RemoteException if RMI communication fails
     */
    void sendNotification(Long userId, NotificationDTO notification) throws RemoteException;
    
    /**
     * Send a notification to all users with a specific role
     * @param role Target role (admin, customer, etc.)
     * @param notification Notification to send
     * @throws RemoteException if RMI communication fails
     */
    void sendNotificationToRole(String role, NotificationDTO notification) throws RemoteException;
    
    /**
     * Send a notification to all users in a specific branch
     * @param branchName Target branch name
     * @param notification Notification to send
     * @throws RemoteException if RMI communication fails
     */
    void sendNotificationToBranch(String branchName, NotificationDTO notification) throws RemoteException;
    
    /**
     * Get all notifications for a user (fallback for offline clients)
     * @param userId User ID
     * @param limit Maximum number of notifications to retrieve
     * @return List of notifications
     * @throws RemoteException if RMI communication fails
     */
    List<NotificationDTO> getNotifications(Long userId, int limit) throws RemoteException;
    
    /**
     * Mark notifications as read
     * @param userId User ID
     * @param notificationIds List of notification IDs to mark as read
     * @throws RemoteException if RMI communication fails
     */
    void markNotificationsAsRead(Long userId, List<Long> notificationIds) throws RemoteException;
    
    /**
     * Get count of unread notifications for a user
     * @param userId User ID
     * @return Number of unread notifications
     * @throws RemoteException if RMI communication fails
     */
    int getUnreadNotificationCount(Long userId) throws RemoteException;
}
