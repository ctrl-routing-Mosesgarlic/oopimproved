package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.AuthService;

/**
 * Base interface for all dashboard controllers
 * Provides common methods for setting user data and RMI services
 */
public interface DashboardController {
    
    /**
     * Set user data and RMI services for the dashboard
     * @param user Current logged-in user
     * @param authService Authentication service reference
     * @param serverInfo Server connection information
     */
    void setUserData(UserDTO user, AuthService authService, String serverInfo);
    
    /**
     * Initialize the dashboard with user-specific data
     */
    void initializeDashboard();
    
    /**
     * Handle logout action
     */
    void handleLogout();
}
