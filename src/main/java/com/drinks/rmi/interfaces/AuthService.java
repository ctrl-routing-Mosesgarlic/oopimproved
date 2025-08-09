package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.drinks.rmi.dto.UserDTO;

/**
 * RMI interface for authentication and user management
 */
public interface AuthService extends Remote {
    
    /**
     * Authenticate a user with username and password
     * 
     * @param username The username
     * @param password The password (plain text)
     * @return User object if authentication successful, null otherwise
     * @throws RemoteException RMI exception
     */
    UserDTO login(String username, String password) throws RemoteException;
    
    /**
     * Register a new customer user
     * 
     * @param username The username
     * @param password The password
     * @param name Customer name
     * @param email Customer email
     * @param phone Customer phone
     * @return The created user if successful, null otherwise
     * @throws RemoteException RMI exception
     */
    UserDTO registerCustomer(String username, String password, String name, String email, String phone) throws RemoteException;
    
    /**
     * Create a new staff user (admin only)
     * 
     * @param username The username
     * @param password The password
     * @param role The role (admin, support, auditor, manager, staff)
     * @param branchId The branch ID (null for HQ roles)
     * @return The created user if successful, null otherwise
     * @throws RemoteException RMI exception
     */
    UserDTO createStaffUser(String username, String password, String role, Long branchId) throws RemoteException;
    
    /**
     * Get all users (admin only)
     * 
     * @param currentUser The current user making the request
     * @return List of all users
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<UserDTO> getAllUsers(UserDTO currentUser) throws RemoteException;
}
