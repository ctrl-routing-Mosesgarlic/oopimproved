package com.drinks.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object for OrderItem information
 * Must implement Serializable for RMI transfer
 */
public class OrderItemDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long orderId;
    private Long drinkId;
    private String drinkName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    
    // Default constructor required for serialization
    public OrderItemDTO() {
    }
    
    public OrderItemDTO(Long id, Long orderId, Long drinkId, String drinkName, 
                       int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.drinkId = drinkId;
        this.drinkName = drinkName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    
    public OrderItemDTO(Long id, Long drinkId, String drinkName, 
                       int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.drinkId = drinkId;
        this.drinkName = drinkName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
        if (this.unitPrice != null) {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        if (unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }
    
    public BigDecimal getSubtotal() {
        return subtotal;
    }
    
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
    
    public BigDecimal getTotalPrice() {
        return subtotal; // Same as subtotal for an individual item
    }
    
    @Override
    public String toString() {
        return "OrderItemDTO{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", drinkId=" + drinkId +
                ", drinkName='" + drinkName + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", subtotal=" + subtotal +
                '}';
    }
}
