package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.drinks.rmi.dto.NotificationDTO;

/**
 * RMI Callback interface for receiving real-time notifications
 * Clients implement this interface to receive push notifications
 */
public interface NotificationCallback extends Remote {
    
    /**
     * Called when a new notification is received
     * @param notification The notification data
     * @throws RemoteException if RMI communication fails
     */
    void onNotificationReceived(NotificationDTO notification) throws RemoteException;
    
    /**
     * Called to test if the callback is still active
     * @return true if callback is active
     * @throws RemoteException if RMI communication fails
     */
    boolean isActive() throws RemoteException;
}
