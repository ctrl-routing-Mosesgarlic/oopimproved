package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.PaymentDTO;
import com.drinks.rmi.dto.PaymentResultDTO;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.PaymentService;
import com.drinks.rmi.server.security.RoleBasedAccessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the PaymentService interface for simulated payment processing
 */
public class PaymentServiceImpl extends UnicastRemoteObject implements PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    public PaymentServiceImpl() throws RemoteException {
        super();
    }
    
    @Override
    public PaymentResultDTO processPayment(UserDTO currentUser, Long orderId, BigDecimal amount, 
                                          String paymentMethod, String paymentDetails) throws RemoteException {
        logger.info("Processing payment for order {} with amount {} using {}", orderId, amount, paymentMethod);
        
        // Validate user permissions
        RoleBasedAccessControl.checkPermission(currentUser, "payment:create");
        
        // Validate order exists and belongs to the customer
        if (!validateOrderOwnership(currentUser, orderId)) {
            return PaymentResultDTO.failed("You can only make payments for your own orders", amount, orderId, paymentMethod);
        }
        
        // Generate a unique transaction ID
        String transactionId = generateTransactionId();
        
        // Simulate payment processing delay (1-2 seconds)
        simulateProcessingDelay();
        
        // Simulate payment success/failure (90% success rate)
        boolean paymentSuccessful = simulatePaymentResult(paymentMethod, paymentDetails);
        
        if (paymentSuccessful) {
            // Save payment record to database
            if (savePaymentRecord(currentUser, orderId, amount, paymentMethod, transactionId, "SUCCESS")) {
                // Update order status to PAID
                updateOrderStatus(orderId, "PAID");
                
                return PaymentResultDTO.success(transactionId, amount, orderId, paymentMethod);
            } else {
                return PaymentResultDTO.failed("Failed to record payment", amount, orderId, paymentMethod);
            }
        } else {
            // Save failed payment attempt
            savePaymentRecord(currentUser, orderId, amount, paymentMethod, transactionId, "FAILED");
            
            return PaymentResultDTO.failed("Payment declined", amount, orderId, paymentMethod);
        }
    }
    
    @Override
    public PaymentResultDTO getPaymentStatus(UserDTO currentUser, Long orderId) throws RemoteException {
        logger.info("Getting payment status for order {}", orderId);
        
        // Validate user permissions
        RoleBasedAccessControl.checkPermission(currentUser, "payment:read");
        
        // Validate order exists and belongs to the customer (or user is admin/staff)
        if (!currentUser.getRole().equals("admin") && !currentUser.getRole().equals("staff") && 
            !validateOrderOwnership(currentUser, orderId)) {
            throw new RemoteException("You can only view payment status for your own orders");
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC LIMIT 1")) {
            
            stmt.setLong(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("status");
                    String txnId = rs.getString("transaction_id");
                    BigDecimal paymentAmount = rs.getBigDecimal("amount");
                    String method = rs.getString("payment_method");
                    
                    return new PaymentResultDTO(
                        status,
                        txnId,
                        status.equals("SUCCESS") ? "Payment completed successfully" : "Payment failed",
                        paymentAmount,
                        orderId,
                        method
                    );
                } else {
                    return PaymentResultDTO.failed("No payment record found for this order", BigDecimal.ZERO, orderId, "UNKNOWN");
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while getting payment status", e);
            throw new RemoteException("Failed to get payment status: " + e.getMessage());
        }
    }
    
    /**
     * Validate that the order exists and belongs to the current user
     */
    private boolean validateOrderOwnership(UserDTO currentUser, Long orderId) {
        // Admin and staff can access any order
        if (currentUser.getRole().equals("admin") || currentUser.getRole().equals("staff")) {
            return true;
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT customer_id FROM orders WHERE id = ?")) {
            
            stmt.setLong(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long customerId = rs.getLong("customer_id");
                    return currentUser.getCustomerId().equals(customerId.toString());
                }
                return false;
            }
        } catch (SQLException e) {
            logger.error("Database error while validating order ownership", e);
            return false;
        }
    }
    
    /**
     * Generate a unique transaction ID for the payment
     */
    private String generateTransactionId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        String uniquePart = UUID.randomUUID().toString().substring(0, 8);
        
        return "TXN-SIM-" + datePart + "-" + uniquePart;
    }
    
    /**
     * Simulate payment processing delay
     */
    private void simulateProcessingDelay() {
        try {
            // Random delay between 1-2 seconds
            long delay = 1000 + (long) (Math.random() * 1000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Simulate payment success/failure with a 90% success rate
     */
    private boolean simulatePaymentResult(String paymentMethod, String paymentDetails) {
        // Special test cases for demo purposes
        if (paymentDetails != null) {
            // Test card number for simulating declined payment
            if (paymentDetails.contains("4000000000000002")) {
                return false;
            }
            // Test card number for simulating network error
            if (paymentDetails.contains("4000000000000069")) {
                throw new RuntimeException("Simulated network error");
            }
        }
        
        // 90% success rate for normal cases
        return Math.random() < 0.9;
    }
    
    /**
     * Save payment record to database
     */
    private boolean savePaymentRecord(UserDTO currentUser, Long orderId, BigDecimal amount, 
                                     String paymentMethod, String transactionId, String status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO payments (order_id, customer_id, amount, payment_method, transaction_id, status, created_at) " +
                 "VALUES (?, ?, ?, ?, ?, ?, NOW())", Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, orderId);
            stmt.setLong(2, currentUser.getCustomerId());
            stmt.setBigDecimal(3, amount);
            stmt.setString(4, paymentMethod);
            stmt.setString(5, transactionId);
            stmt.setString(6, status);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            logger.error("Database error while saving payment record", e);
            return false;
        }
    }
    
    /**
     * Update order status after payment
     */
    private void updateOrderStatus(Long orderId, String status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE orders SET status = ? WHERE id = ?")) {
            
            stmt.setString(1, status);
            stmt.setLong(2, orderId);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            logger.error("Database error while updating order status", e);
        }
    }
    
    @Override
    public List<PaymentDTO> getAllPayments(UserDTO currentUser) throws RemoteException {
        logger.info("Retrieving all payment records");
        
        // Only admins and staff can view all payments
        if (!currentUser.getRole().equals("admin") && !currentUser.getRole().equals("staff")) {
            throw new RemoteException("You do not have permission to view all payment records");
        }
        
        // Validate user permissions
        RoleBasedAccessControl.checkPermission(currentUser, "payment:read");
        
        List<PaymentDTO> payments = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT p.*, o.customer_id FROM payments p " +
                 "JOIN orders o ON p.order_id = o.id " +
                 "ORDER BY p.created_at DESC")) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PaymentDTO payment = new PaymentDTO();
                    payment.setId(rs.getLong("id"));
                    payment.setOrderId(rs.getLong("order_id"));
                    payment.setCustomerId(String.valueOf(rs.getLong("customer_id")));
                    payment.setAmount(rs.getBigDecimal("amount"));
                    payment.setPaymentMethod(rs.getString("payment_method"));
                    payment.setTransactionId(rs.getString("transaction_id"));
                    payment.setStatus(rs.getString("status"));
                    
                    // Convert SQL timestamp to LocalDateTime
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        payment.setCreatedAt(timestamp.toLocalDateTime());
                    }
                    
                    payments.add(payment);
                }
            }
            
            return payments;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving payment records", e);
            throw new RemoteException("Failed to retrieve payment records: " + e.getMessage());
        }
    }
}
