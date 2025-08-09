package com.drinks.rmi.dto;

import java.io.Serializable;

/**
 * Data Transfer Object for User information
 * Must implement Serializable for RMI transfer
 */
public class UserDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String username;
    private String role;
    private Long branchId;
    private String branchName;
    private Long customerId;
    private String customerName;
    
    // Default constructor required for serialization
    public UserDTO() {
    }
    
    public UserDTO(Long id, String username, String role, Long branchId, 
                  String branchName, Long customerId, String customerName) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.branchId = branchId;
        this.branchName = branchName;
        this.customerId = customerId;
        this.customerName = customerName;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
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
    
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", branchId=" + branchId +
                ", branchName='" + branchName + '\'' +
                ", customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                '}';
    }
}
