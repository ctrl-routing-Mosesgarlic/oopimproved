package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.Map;

import com.drinks.rmi.dto.CustomerReportDTO;
import com.drinks.rmi.dto.DrinkPopularityReportDTO;
import com.drinks.rmi.dto.SalesReportDTO;
import com.drinks.rmi.dto.StockReportDTO;

/**
 * RMI interface for report generation
 * This service is primarily available at the HQ server
 */
public interface ReportService extends Remote {
    
    /**
     * Generate sales report for a specific branch within a date range
     * 
     * @param branchId The branch ID
     * @param startDate The start date
     * @param endDate The end date
     * @return Sales report data
     * @throws RemoteException RMI exception
     */
    SalesReportDTO generateBranchSalesReport(Long branchId, LocalDate startDate, LocalDate endDate) throws RemoteException;
    
    /**
     * Generate sales report for all branches within a date range
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Map of branch ID to sales report data
     * @throws RemoteException RMI exception
     */
    Map<Long, SalesReportDTO> generateAllBranchesSalesReport(LocalDate startDate, LocalDate endDate) throws RemoteException;
    
    /**
     * Generate stock report for a specific branch
     * 
     * @param branchId The branch ID
     * @return Stock report data
     * @throws RemoteException RMI exception
     */
    StockReportDTO generateBranchStockReport(Long branchId) throws RemoteException;
    
    /**
     * Generate stock report for all branches
     * 
     * @return Map of branch ID to stock report data
     * @throws RemoteException RMI exception
     */
    Map<Long, StockReportDTO> generateAllBranchesStockReport() throws RemoteException;
    
    /**
     * Generate customer order history report
     * 
     * @param customerId The customer ID
     * @param startDate The start date
     * @param endDate The end date
     * @return Customer order history report data
     * @throws RemoteException RMI exception
     */
    CustomerReportDTO generateCustomerReport(Long customerId, LocalDate startDate, LocalDate endDate) throws RemoteException;
    
    /**
     * Generate drink popularity report
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Drink popularity report data
     * @throws RemoteException RMI exception
     */
    DrinkPopularityReportDTO generateDrinkPopularityReport(LocalDate startDate, LocalDate endDate) throws RemoteException;
    
    /**
     * Get drink popularity report
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Drink popularity report data
     * @throws RemoteException RMI exception
     */
    DrinkPopularityReportDTO getDrinkPopularityReport(LocalDate startDate, LocalDate endDate) throws RemoteException;
    
    /**
     * Get customer report
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @return Customer report data
     * @throws RemoteException RMI exception
     */
    CustomerReportDTO getCustomerReport(LocalDate startDate, LocalDate endDate) throws RemoteException;
}
