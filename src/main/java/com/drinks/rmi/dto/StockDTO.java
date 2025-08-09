package com.drinks.rmi.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for Stock information
 * Must implement Serializable for RMI transfer
 */
public class StockDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long branchId;
    private String branchName;
    private Long drinkId;
    private String drinkName;
    private int quantity;
    private int threshold;
    private double unitPrice;
    
    // Default constructor required for serialization
    public StockDTO() {
    }
    
    public StockDTO(Long id, Long branchId, Long drinkId, int quantity) {
        this.id = id;
        this.branchId = branchId;
        this.drinkId = drinkId;
        this.quantity = quantity;
    }
    
    public StockDTO(Long id, Long branchId, String branchName, Long drinkId, 
                   String drinkName, int quantity, int threshold) {
        this.id = id;
        this.branchId = branchId;
        this.branchName = branchName;
        this.drinkId = drinkId;
        this.drinkName = drinkName;
        this.quantity = quantity;
        this.threshold = threshold;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Long getDrinkId() {
        return drinkId;
    }
    
    public void setDrinkId(Long drinkId) {
        this.drinkId = drinkId;
    }
    
    public String getDrinkName() {
        return drinkName;
    }
    
    public void setDrinkName(String drinkName) {
        this.drinkName = drinkName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
    
    public double getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    @Override
    public String toString() {
        return "StockDTO{" +
                "id=" + id +
                ", branchId=" + branchId +
                ", branchName='" + branchName + '\'' +
                ", drinkId=" + drinkId +
                ", drinkName='" + drinkName + '\'' +
                ", quantity=" + quantity +
                ", threshold=" + threshold +
                '}';
    }
}
