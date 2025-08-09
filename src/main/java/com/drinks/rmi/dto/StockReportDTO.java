package com.drinks.rmi.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Stock Report information
 * Must implement Serializable for RMI transfer
 */
public class StockReportDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long branchId;
    private String branchName;
    private int totalItems;
    private int lowStockItems;
    private int outOfStockItems;
    private int lowStockCount;
    private int outOfStockCount;
    private List<StockDTO> stockItems;
    private List<StockDTO> lowStockAlerts;
    
    // Default constructor required for serialization
    public StockReportDTO() {
        this.stockItems = new ArrayList<>();
        this.lowStockAlerts = new ArrayList<>();
    }
    
    public StockReportDTO(Long branchId, String branchName) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.totalItems = 0;
        this.lowStockItems = 0;
        this.outOfStockItems = 0;
        this.lowStockCount = 0;
        this.outOfStockCount = 0;
        this.stockItems = new ArrayList<>();
        this.lowStockAlerts = new ArrayList<>();
    }
    
    // Getters and setters
    public Long getBranchId() {
        return branchId;
    }
    
    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
    
    public String getBranchName() {
        return branchName;
    }
    
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
    
    public int getTotalItems() {
        return totalItems;
    }
    
    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
    
    public int getLowStockItems() {
        return lowStockItems;
    }
    
    public void setLowStockItems(int lowStockItems) {
        this.lowStockItems = lowStockItems;
    }
    
    public int getOutOfStockItems() {
        return outOfStockItems;
    }
    
    public void setOutOfStockItems(int outOfStockItems) {
        this.outOfStockItems = outOfStockItems;
    }
    
    public int getLowStockCount() {
        return lowStockCount;
    }
    
    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }
    
    public int getOutOfStockCount() {
        return outOfStockCount;
    }
    
    public void setOutOfStockCount(int outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }
    
    public List<StockDTO> getStockItems() {
        return stockItems;
    }
    
    public void setStockItems(List<StockDTO> stockItems) {
        this.stockItems = stockItems;
        if (stockItems != null) {
            this.totalItems = stockItems.size();
            this.lowStockItems = (int) stockItems.stream()
                .filter(item -> item.getQuantity() > 0 && item.getQuantity() <= item.getThreshold())
                .count();
            this.outOfStockItems = (int) stockItems.stream()
                .filter(item -> item.getQuantity() == 0)
                .count();
            this.lowStockCount = (int) stockItems.stream()
                .filter(item -> item.getQuantity() <= item.getThreshold())
                .count();
            this.outOfStockCount = (int) stockItems.stream()
                .filter(item -> item.getQuantity() == 0)
                .count();
            
            this.lowStockAlerts = stockItems.stream()
                .filter(item -> item.getQuantity() <= item.getThreshold())
                .toList();
        }
    }
    
    public List<StockDTO> getLowStockAlerts() {
        return lowStockAlerts;
    }
    
    public void setLowStockAlerts(List<StockDTO> lowStockAlerts) {
        this.lowStockAlerts = lowStockAlerts;
    }
    
    @Override
    public String toString() {
        return "StockReportDTO{" +
                "branchId=" + branchId +
                ", branchName='" + branchName + '\'' +
                ", totalItems=" + totalItems +
                ", lowStockItems=" + lowStockItems +
                ", outOfStockItems=" + outOfStockItems +
                ", lowStockCount=" + lowStockCount +
                ", outOfStockCount=" + outOfStockCount +
                ", stockItems=" + stockItems +
                ", lowStockAlerts=" + lowStockAlerts +
                '}';
    }
}
