package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.interfaces.StockService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of StockService for stock management
 */
public class StockServiceImpl extends UnicastRemoteObject implements StockService {
    
    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);
    
    public StockServiceImpl() throws RemoteException {
        super();
    }
    
    private StockDTO createStockDTO(ResultSet rs) throws SQLException {
        return new StockDTO(
            rs.getLong("id"),
            rs.getLong("branch_id"),
            rs.getString("branch_name"),
            rs.getLong("drink_id"),
            rs.getString("drink_name"),
            rs.getInt("quantity"),
            rs.getInt("threshold")
        );
    }
    
    @Override
    public List<StockDTO> getStockByBranch(Long branchId) throws RemoteException {
        logger.info("Retrieving stock for branch ID: {}", branchId);
        
        String sql = """
            SELECT s.id, s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, 
                   s.quantity, COALESCE(s.threshold, 10) as threshold
            FROM stocks s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.branch_id = ?
            ORDER BY d.name
            """;
        
        List<StockDTO> stockItems = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stockItems.add(createStockDTO(rs));
            }
            
            logger.info("Retrieved {} stock items for branch ID: {}", stockItems.size(), branchId);
            return stockItems;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving stock for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to retrieve stock due to database error", e);
        }
    }
    
    @Override
    public List<StockDTO> getStockByDrink(Long drinkId) throws RemoteException {
        logger.info("Retrieving stock for drink ID: {}", drinkId);
        
        String sql = """
            SELECT s.id, s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, 
                   s.quantity, COALESCE(s.threshold, 10) as threshold
            FROM stocks s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.drink_id = ?
            ORDER BY b.name
            """;
        
        List<StockDTO> stockItems = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, drinkId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                stockItems.add(createStockDTO(rs));
            }
            
            logger.info("Retrieved {} stock items for drink ID: {}", stockItems.size(), drinkId);
            return stockItems;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving stock for drink ID: {}", drinkId, e);
            throw new RemoteException("Failed to retrieve stock due to database error", e);
        }
    }
    
    @Override
    public StockDTO getStockByBranchAndDrink(Long branchId, Long drinkId) throws RemoteException {
        logger.info("Retrieving stock for branch ID: {} and drink ID: {}", branchId, drinkId);
        
        String sql = """
            SELECT s.id, s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, 
                   s.quantity, COALESCE(s.threshold, 10) as threshold
            FROM stocks s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.branch_id = ? AND s.drink_id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            stmt.setLong(2, drinkId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                StockDTO stock = createStockDTO(rs);
                logger.info("Retrieved stock: {} units of {} at {}", stock.getQuantity(), stock.getDrinkName(), stock.getBranchName());
                return stock;
            }
            
            logger.warn("Stock not found for branch ID: {} and drink ID: {}", branchId, drinkId);
            return null;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving stock for branch ID: {} and drink ID: {}", branchId, drinkId, e);
            throw new RemoteException("Failed to retrieve stock due to database error", e);
        }
    }
    
    @Override
    public StockDTO updateStockQuantity(Long branchId, Long drinkId, int quantity) throws RemoteException {
        logger.info("Updating stock quantity for branch ID: {} and drink ID: {} to {}", branchId, drinkId, quantity);
        
        // First check if stock record exists
        StockDTO existingStock = getStockByBranchAndDrink(branchId, drinkId);
        
        if (existingStock != null) {
            // Update existing stock
            String updateSql = "UPDATE stocks SET quantity = ? WHERE branch_id = ? AND drink_id = ?";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                
                stmt.setInt(1, quantity);
                stmt.setLong(2, branchId);
                stmt.setLong(3, drinkId);
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    logger.info("Updated stock quantity successfully");
                    return getStockByBranchAndDrink(branchId, drinkId);
                } else {
                    logger.warn("No rows updated for stock");
                    return null;
                }
                
            } catch (SQLException e) {
                logger.error("Database error while updating stock quantity", e);
                throw new RemoteException("Failed to update stock due to database error", e);
            }
        } else {
            // Create new stock record
            String insertSql = "INSERT INTO stocks (branch_id, drink_id, quantity) VALUES (?, ?, ?)";
            
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                
                stmt.setLong(1, branchId);
                stmt.setLong(2, drinkId);
                stmt.setInt(3, quantity);
                
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    logger.info("Created new stock record successfully");
                    return getStockByBranchAndDrink(branchId, drinkId);
                } else {
                    logger.warn("No rows inserted for new stock");
                    return null;
                }
                
            } catch (SQLException e) {
                logger.error("Database error while creating new stock record", e);
                throw new RemoteException("Failed to create stock due to database error", e);
            }
        }
    }
    
    @Override
    public boolean updateStock(Long stockId, String drinkName, int quantity) throws RemoteException {
        logger.info("Updating stock ID: {} with new quantity: {}", stockId, quantity);
        
        String sql = "UPDATE stocks SET quantity = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantity);
            stmt.setLong(2, stockId);
            
            int rowsAffected = stmt.executeUpdate();
            logger.info("Updated {} stock records", rowsAffected);
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating stock", e);
            throw new RemoteException("Error updating stock", e);
        }
    }
    
    @Override
    public boolean transferStock(Long sourceBranchId, Long targetBranchId, Long drinkId, int quantity) throws RemoteException {
        logger.info("Transferring {} units of drink ID: {} from branch ID: {} to branch ID: {}", 
                   quantity, drinkId, sourceBranchId, targetBranchId);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Check source stock availability
                StockDTO sourceStock = getStockByBranchAndDrink(sourceBranchId, drinkId);
                if (sourceStock == null || sourceStock.getQuantity() < quantity) {
                    logger.warn("Insufficient stock for transfer. Available: {}, Requested: {}", 
                               sourceStock != null ? sourceStock.getQuantity() : 0, quantity);
                    conn.rollback();
                    return false;
                }
                
                // Reduce source stock
                updateStockQuantity(sourceBranchId, drinkId, sourceStock.getQuantity() - quantity);
                
                // Increase target stock
                StockDTO targetStock = getStockByBranchAndDrink(targetBranchId, drinkId);
                int newTargetQuantity = (targetStock != null ? targetStock.getQuantity() : 0) + quantity;
                updateStockQuantity(targetBranchId, drinkId, newTargetQuantity);
                
                conn.commit();
                logger.info("Stock transfer completed successfully");
                return true;
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Database error during stock transfer", e);
            throw new RemoteException("Failed to transfer stock due to database error", e);
        }
    }
    
    @Override
    public List<StockDTO> getLowStockAlerts(Long branchId) throws RemoteException {
        logger.info("Retrieving low stock alerts for branch ID: {}", branchId);
        
        String sql = """
            SELECT s.id, s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, 
                   s.quantity, COALESCE(s.threshold, 10) as threshold
            FROM stocks s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.branch_id = ? AND s.quantity <= COALESCE(s.threshold, 10)
            ORDER BY s.quantity ASC, d.name
            """;
        
        List<StockDTO> lowStockItems = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                lowStockItems.add(createStockDTO(rs));
            }
            
            logger.info("Retrieved {} low stock alerts for branch ID: {}", lowStockItems.size(), branchId);
            return lowStockItems;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving low stock alerts for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to retrieve low stock alerts due to database error", e);
        }
    }
    
    @Override
    public List<StockDTO> getBranchStock(Long branchId) throws RemoteException {
        return getStockByBranch(branchId);
    }
    
    @Override
    public List<StockDTO> getAllStock() throws RemoteException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM stocks");
             ResultSet rs = stmt.executeQuery()) {
            
            List<StockDTO> stockList = new ArrayList<>();
            while (rs.next()) {
                StockDTO stock = new StockDTO(
                    rs.getLong("id"),
                    rs.getLong("branch_id"),
                    rs.getLong("drink_id"),
                    rs.getInt("quantity")
                );
                stockList.add(stock);
            }
            return stockList;
        } catch (SQLException e) {
            logger.error("Error getting all stock", e);
            throw new RemoteException("Error getting all stock", e);
        }
    }
}
