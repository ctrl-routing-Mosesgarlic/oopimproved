package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.drinks.rmi.dto.StockDTO;

/**
 * RMI interface for stock management
 */
public interface StockService extends Remote {
    
    /**
     * Get stock for a specific branch
     * 
     * @param branchId The branch ID
     * @return List of stock items for the branch
     * @throws RemoteException RMI exception
     */
    List<StockDTO> getStockByBranch(Long branchId) throws RemoteException;
    
    /**
     * Get stock for a specific drink across all branches
     * 
     * @param drinkId The drink ID
     * @return List of stock items for the drink
     * @throws RemoteException RMI exception
     */
    List<StockDTO> getStockByDrink(Long drinkId) throws RemoteException;
    
    /**
     * Get stock for a specific branch and drink
     * 
     * @param branchId The branch ID
     * @param drinkId The drink ID
     * @return The stock item if found, null otherwise
     * @throws RemoteException RMI exception
     */
    StockDTO getStockByBranchAndDrink(Long branchId, Long drinkId) throws RemoteException;
    
    /**
     * Update stock quantity (branch manager or staff only)
     * 
     * @param branchId The branch ID
     * @param drinkId The drink ID
     * @param quantity The new quantity
     * @return The updated stock item
     * @throws RemoteException RMI exception
     */
    StockDTO updateStockQuantity(Long branchId, Long drinkId, int quantity) throws RemoteException;
    
    /**
     * Transfer stock between branches (branch manager only)
     * 
     * @param sourceBranchId The source branch ID
     * @param targetBranchId The target branch ID
     * @param drinkId The drink ID
     * @param quantity The quantity to transfer
     * @return true if transfer successful, false otherwise
     * @throws RemoteException RMI exception
     */
    boolean transferStock(Long sourceBranchId, Long targetBranchId, Long drinkId, int quantity) throws RemoteException;
    
    /**
     * Get low stock alerts for a branch
     * 
     * @param branchId The branch ID
     * @return List of stock items below threshold
     * @throws RemoteException RMI exception
     */
    List<StockDTO> getLowStockAlerts(Long branchId) throws RemoteException;
    
    /**
     * Get all stock
     * 
     * @return List of all stock items
     * @throws RemoteException RMI exception
     */
    List<StockDTO> getAllStock() throws RemoteException;
    
    /**
     * Get stock for a specific branch
     * 
     * @param branchId The branch ID
     * @return List of stock items for the branch
     * @throws RemoteException RMI exception
     */
    List<StockDTO> getBranchStock(Long branchId) throws RemoteException;
    
    /**
     * Update stock
     * 
     * @param stockId The stock ID
     * @param drinkName The drink name
     * @param quantity The new quantity
     * @return true if update successful, false otherwise
     * @throws RemoteException RMI exception
     */
    boolean updateStock(Long stockId, String drinkName, int quantity) throws RemoteException;
}
