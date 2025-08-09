package com.drinks.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object for Sales Report information
 * Must implement Serializable for RMI transfer
 */
public class SalesReportDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long branchId;
    private String branchName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalOrders;
    private BigDecimal totalSales;
    private double totalRevenue;
    private Map<String, BigDecimal> salesByDrink;
    private Map<LocalDate, BigDecimal> salesByDate;
    
    // Default constructor required for serialization
    public SalesReportDTO() {
        this.salesByDrink = new HashMap<>();
        this.salesByDate = new HashMap<>();
    }
    
    public SalesReportDTO(Long branchId, String branchName, LocalDate startDate, LocalDate endDate) {
        this.branchId = branchId;
        this.branchName = branchName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = 0;
        this.totalSales = BigDecimal.ZERO;
        this.totalRevenue = 0.0;
        this.salesByDrink = new HashMap<>();
        this.salesByDate = new HashMap<>();
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
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public int getTotalOrders() {
        return totalOrders;
    }
    
    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }
    
    public BigDecimal getTotalSales() {
        return totalSales;
    }
    
    public void setTotalSales(BigDecimal totalSales) {
        this.totalSales = totalSales;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public Map<String, BigDecimal> getSalesByDrink() {
        return salesByDrink;
    }
    
    public void setSalesByDrink(Map<String, BigDecimal> salesByDrink) {
        this.salesByDrink = salesByDrink;
    }
    
    public Map<LocalDate, BigDecimal> getSalesByDate() {
        return salesByDate;
    }
    
    public void setSalesByDate(Map<LocalDate, BigDecimal> salesByDate) {
        this.salesByDate = salesByDate;
    }
    
    @Override
    public String toString() {
        return "SalesReportDTO{" +
                "branchId=" + branchId +
                ", branchName='" + branchName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalOrders=" + totalOrders +
                ", totalSales=" + totalSales +
                ", totalRevenue=" + totalRevenue +
                ", salesByDrink=" + salesByDrink +
                ", salesByDate=" + salesByDate +
                '}';
    }
}
