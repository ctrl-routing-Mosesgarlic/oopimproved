package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.DrinkService;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.server.security.RoleBasedAccessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of DrinkService for drink management
 */
public class DrinkServiceImpl extends UnicastRemoteObject implements DrinkService {
    
    private static final Logger logger = LoggerFactory.getLogger(DrinkServiceImpl.class);
    
    public DrinkServiceImpl() throws RemoteException {
        super();
    }
    
    @Override
    public List<DrinkDTO> getAllDrinks() throws RemoteException {
        // No permission check needed - all roles can view drinks
        logger.info("Retrieving all drinks");
        
        String sql = "SELECT id, name, price FROM drinks ORDER BY name";
        List<DrinkDTO> drinks = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                DrinkDTO drink = new DrinkDTO(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price")
                );
                drinks.add(drink);
            }
            
            logger.info("Retrieved {} drinks", drinks.size());
            return drinks;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving all drinks", e);
            throw new RemoteException("Failed to retrieve drinks due to database error", e);
        }
    }
    
    @Override
    public DrinkDTO getDrinkById(Long id) throws RemoteException {
        // No permission check needed - all roles can view drinks
        logger.info("Retrieving drink with ID: {}", id);
        
        String sql = "SELECT id, name, price FROM drinks WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                DrinkDTO drink = new DrinkDTO(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price")
                );
                logger.info("Retrieved drink: {}", drink.getName());
                return drink;
            }
            
            logger.warn("Drink not found with ID: {}", id);
            return null;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving drink with ID: {}", id, e);
            throw new RemoteException("Failed to retrieve drink due to database error", e);
        }
    }
    
    @Override
    public DrinkDTO getDrinkByName(String name) throws RemoteException {
        logger.info("Retrieving drink by name: {}", name);
        
        String sql = "SELECT id, name, price FROM drinks WHERE name = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new DrinkDTO(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getBigDecimal("price")
                );
            }
            return null;
        } catch (SQLException e) {
            logger.error("Error retrieving drink by name", e);
            throw new RemoteException("Error retrieving drink", e);
        }
    }
    
    @Override
    public DrinkDTO createDrink(UserDTO currentUser, String name, double price) throws RemoteException {
        // Check if user has permission to create drinks
        RoleBasedAccessControl.checkPermission(currentUser, "drink:create");
        
        logger.info("Creating new drink: {} with price: {}", name, price);
        
        String sql = "INSERT INTO drinks (name, price) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, name);
            stmt.setBigDecimal(2, BigDecimal.valueOf(price));
            
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Creating drink failed, no rows affected");
            }
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (!keys.next()) {
                throw new SQLException("Creating drink failed, no ID obtained");
            }
            
            Long drinkId = keys.getLong(1);
            DrinkDTO drink = new DrinkDTO(drinkId, name, BigDecimal.valueOf(price));
            
            logger.info("Created drink successfully: {} with ID: {}", name, drinkId);
            return drink;
            
        } catch (SQLException e) {
            logger.error("Database error while creating drink: {}", name, e);
            throw new RemoteException("Failed to create drink due to database error", e);
        }
    }
    
    @Override
    public DrinkDTO updateDrink(UserDTO currentUser, Long id, String name, Double price) throws RemoteException {
        // Check if user has permission to update drinks
        RoleBasedAccessControl.checkPermission(currentUser, "drink:update");
        
        logger.info("Updating drink with ID: {}", id);
        
        // First, get the current drink to check if it exists
        DrinkDTO currentDrink = getDrinkById(id);
        if (currentDrink == null) {
            logger.warn("Cannot update drink - not found with ID: {}", id);
            return null;
        }
        
        // Build dynamic SQL based on what fields are being updated
        StringBuilder sqlBuilder = new StringBuilder("UPDATE drinks SET ");
        List<Object> parameters = new ArrayList<>();
        
        if (name != null) {
            sqlBuilder.append("name = ?, ");
            parameters.add(name);
        }
        
        if (price != null) {
            sqlBuilder.append("price = ?, ");
            parameters.add(BigDecimal.valueOf(price));
        }
        
        if (parameters.isEmpty()) {
            logger.warn("No fields to update for drink ID: {}", id);
            return currentDrink;
        }
        
        // Remove the trailing comma and space
        String sql = sqlBuilder.substring(0, sqlBuilder.length() - 2) + " WHERE id = ?";
        parameters.add(id);
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                logger.warn("No rows updated for drink ID: {}", id);
                return null;
            }
            
            // Return the updated drink
            DrinkDTO updatedDrink = getDrinkById(id);
            logger.info("Updated drink successfully: {}", updatedDrink.getName());
            return updatedDrink;
            
        } catch (SQLException e) {
            logger.error("Database error while updating drink with ID: {}", id, e);
            throw new RemoteException("Failed to update drink due to database error", e);
        }
    }
    
    @Override
    public boolean deleteDrink(UserDTO currentUser, Long id) throws RemoteException {
        // Check if user has permission to delete drinks
        RoleBasedAccessControl.checkPermission(currentUser, "drink:delete");
        
        logger.info("Deleting drink with ID: {}", id);
        
        // Check if drink exists first
        DrinkDTO drink = getDrinkById(id);
        if (drink == null) {
            logger.warn("Cannot delete drink - not found with ID: {}", id);
            return false;
        }
        
        String sql = "DELETE FROM drinks WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                logger.info("Deleted drink successfully: {}", drink.getName());
                return true;
            } else {
                logger.warn("No rows deleted for drink ID: {}", id);
                return false;
            }
            
        } catch (SQLException e) {
            logger.error("Database error while deleting drink with ID: {}", id, e);
            throw new RemoteException("Failed to delete drink due to database error", e);
        }
    }
}
