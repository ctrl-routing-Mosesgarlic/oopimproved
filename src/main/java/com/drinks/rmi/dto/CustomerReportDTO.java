package com.drinks.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Customer Report information
 * Must implement Serializable for RMI transfer
 */
public class CustomerReportDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalOrders;
    private BigDecimal totalSpent;
    private List<OrderDTO> orders;
    private int totalCustomers;
    private int activeCustomers;
    private Map<UserDTO, List<OrderDTO>> customerOrderHistory;
    private List<CustomerOrderSummary> topCustomers;
    private int loginCount;
    private List<PaymentDTO> payments;
    private List<LoginHistoryDTO> loginHistory;
    
    // Default constructor required for serialization
    public CustomerReportDTO() {
        this.orders = new ArrayList<>();
        this.customerOrderHistory = new HashMap<>();
        this.topCustomers = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.loginHistory = new ArrayList<>();
    }
    
    public CustomerReportDTO(Long customerId, String customerName, String customerEmail, 
                           String customerPhone, LocalDate startDate, LocalDate endDate) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalOrders = 0;
        this.totalSpent = BigDecimal.ZERO;
        this.orders = new ArrayList<>();
        this.customerOrderHistory = new HashMap<>();
        this.topCustomers = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.loginHistory = new ArrayList<>();
    }
    
    // Getters and setters
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
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
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
    
    public BigDecimal getTotalSpent() {
        return totalSpent;
    }
    
    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }
    
    public List<OrderDTO> getOrders() {
        return orders;
    }
    
    public void setOrders(List<OrderDTO> orders) {
        this.orders = orders;
        if (orders != null) {
            this.totalOrders = orders.size();
            this.totalSpent = orders.stream()
                .map(order -> BigDecimal.valueOf(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
    
    public int getTotalCustomers() {
        return totalCustomers;
    }
    
    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }
    
    public int getActiveCustomers() {
        return activeCustomers;
    }
    
    public void setActiveCustomers(int activeCustomers) {
        this.activeCustomers = activeCustomers;
    }
    
    public Map<UserDTO, List<OrderDTO>> getCustomerOrderHistory() {
        return customerOrderHistory;
    }
    
    public void setCustomerOrderHistory(Map<UserDTO, List<OrderDTO>> customerOrderHistory) {
        this.customerOrderHistory = customerOrderHistory;
    }
    
    public List<CustomerOrderSummary> getTopCustomers() {
        return topCustomers;
    }
    
    public void setTopCustomers(List<CustomerOrderSummary> topCustomers) {
        this.topCustomers = topCustomers;
    }
    
    public int getLoginCount() {
        return loginCount;
    }
    
    public void setLoginCount(int loginCount) {
        this.loginCount = loginCount;
    }
    
    public List<PaymentDTO> getPayments() {
        return payments;
    }
    
    public void setPayments(List<PaymentDTO> payments) {
        this.payments = payments;
    }
    
    public List<LoginHistoryDTO> getLoginHistory() {
        return loginHistory;
    }
    
    public void setLoginHistory(List<LoginHistoryDTO> loginHistory) {
        this.loginHistory = loginHistory;
    }
    
    public static class CustomerOrderSummary {
        private Long customerId;
        private String customerName;
        private int orderCount;
        private BigDecimal totalSpent;

        public CustomerOrderSummary(Long customerId, String customerName, int orderCount, BigDecimal totalSpent) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.orderCount = orderCount;
            this.totalSpent = totalSpent;
        }

        // Getters and setters
        public Long getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public int getOrderCount() { return orderCount; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public int getTotalOrders() {
            return orderCount;
        }
    }
    
    @Override
    public String toString() {
        return "CustomerReportDTO{" +
                "customerId=" + customerId +
                ", customerName='" + customerName + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerPhone='" + customerPhone + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", totalOrders=" + totalOrders +
                ", totalSpent=" + totalSpent +
                ", orders=" + orders +
                ", totalCustomers=" + totalCustomers +
                ", activeCustomers=" + activeCustomers +
                ", customerOrderHistory=" + customerOrderHistory +
                ", topCustomers=" + topCustomers +
                ", loginCount=" + loginCount +
                ", payments=" + payments +
                ", loginHistory=" + loginHistory +
                '}';
    }
}
