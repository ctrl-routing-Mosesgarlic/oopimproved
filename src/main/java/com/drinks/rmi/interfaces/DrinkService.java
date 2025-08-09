package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.dto.DrinkDTO;

/**
 * RMI interface for drink management
 */
public interface DrinkService extends Remote {
    
    /**
     * Get all available drinks
     * 
     * @return List of all drinks
     * @throws RemoteException RMI exception
     */
    List<DrinkDTO> getAllDrinks() throws RemoteException;
    
    /**
     * Get a drink by ID
     * 
     * @param id The drink ID
     * @return The drink if found, null otherwise
     * @throws RemoteException RMI exception
     */
    DrinkDTO getDrinkById(Long id) throws RemoteException;
    
    /**
     * Get a drink by name
     * 
     * @param name The drink name
     * @return The drink if found, null otherwise
     * @throws RemoteException RMI exception
     */
    DrinkDTO getDrinkByName(String name) throws RemoteException;
    
    /**
     * Create a new drink (admin only)
     * 
     * @param currentUser The current user making the request
     * @param name The drink name
     * @param price The drink price
     * @return The created drink
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    DrinkDTO createDrink(UserDTO currentUser, String name, double price) throws RemoteException;
    
    /**
     * Update a drink (admin and global manager)
     * 
     * @param currentUser The current user making the request
     * @param id The drink ID
     * @param name The new name (null to keep current)
     * @param price The new price (null to keep current)
     * @return The updated drink
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    DrinkDTO updateDrink(UserDTO currentUser, Long id, String name, Double price) throws RemoteException;
    
    /**
     * Delete a drink (admin only)
     * 
     * @param currentUser The current user making the request
     * @param id The drink ID
     * @return true if deleted, false otherwise
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    boolean deleteDrink(UserDTO currentUser, Long id) throws RemoteException;
}
