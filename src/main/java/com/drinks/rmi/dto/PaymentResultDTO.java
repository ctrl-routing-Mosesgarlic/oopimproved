package com.drinks.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for payment transaction results
 */
public class PaymentResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String status;          // SUCCESS, FAILED, PENDING
    private String transactionId;   // Unique transaction identifier
    private String message;         // Human-readable message
    private BigDecimal amount;      // Payment amount
    private Long orderId;           // Associated order ID
    private String paymentMethod;   // Payment method used
    private LocalDateTime timestamp; // Payment timestamp
    
    // Default constructor for serialization
    public PaymentResultDTO() {
    }
    
    public PaymentResultDTO(String status, String transactionId, String message, 
                           BigDecimal amount, Long orderId, String paymentMethod) {
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.amount = amount;
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.timestamp = LocalDateTime.now();
    }
    
    // Static factory methods for common responses
    public static PaymentResultDTO success(String transactionId, BigDecimal amount, Long orderId, String paymentMethod) {
        return new PaymentResultDTO(
            "SUCCESS",
            transactionId,
            "Payment processed successfully",
            amount,
            orderId,
            paymentMethod
        );
    }
    
    public static PaymentResultDTO failed(String message, BigDecimal amount, Long orderId, String paymentMethod) {
        return new PaymentResultDTO(
            "FAILED",
            "TXN-FAILED-" + System.currentTimeMillis(),
            message,
            amount,
            orderId,
            paymentMethod
        );
    }
    
    public static PaymentResultDTO pending(BigDecimal amount, Long orderId, String paymentMethod) {
        return new PaymentResultDTO(
            "PENDING",
            "TXN-PENDING-" + System.currentTimeMillis(),
            "Payment is being processed",
            amount,
            orderId,
            paymentMethod
        );
    }
    
    // Getters and setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "PaymentResultDTO{" +
                "status='" + status + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", message='" + message + '\'' +
                ", amount=" + amount +
                ", orderId=" + orderId +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}