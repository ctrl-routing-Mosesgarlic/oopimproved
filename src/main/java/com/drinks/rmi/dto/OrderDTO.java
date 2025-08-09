package com.drinks.rmi.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Order information
 * Must implement Serializable for RMI transfer
 */
public class OrderDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long customerId;
    private String customerName;
    private Long branchId;
    private String branchName;
    private LocalDateTime orderTime;
    private double totalAmount;
    private List<OrderItemDTO> items;
    private String status;
    private String orderDate; // String representation of the order date
    
    // Default constructor required for serialization
    public OrderDTO() {
        this.items = new ArrayList<>();
    }
    
    public OrderDTO(Long id, Long customerId, String customerName, Long branchId, 
                   String branchName, LocalDateTime orderTime) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.branchId = branchId;
        this.branchName = branchName;
        this.orderTime = orderTime;
        this.items = new ArrayList<>();
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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
    
    public LocalDateTime getOrderTime() {
        return orderTime;
    }
    
    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public List<OrderItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
    
    public void addItem(OrderItemDTO item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }
    
    @Override
    public String toString() {
        return "OrderDTO{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", branchId=" + branchId +
                ", branchName='" + branchName + '\'' +
                ", orderTime=" + orderTime +
                ", totalAmount=" + totalAmount +
                ", items=" + items +
                '}';
    }
}
