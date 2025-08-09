package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.math.BigDecimal;
import java.util.List;

import com.drinks.rmi.dto.PaymentDTO;
import com.drinks.rmi.dto.PaymentResultDTO;
import com.drinks.rmi.dto.UserDTO;

/**
 * Remote interface for payment processing operations
 */
public interface PaymentService extends Remote {
    
    /**
     * Process a simulated payment for an order
     * 
     * @param currentUser The authenticated user making the payment
     * @param orderId The ID of the order being paid for
     * @param amount The payment amount
     * @param paymentMethod The payment method (e.g., "CARD", "MOBILE_MONEY")
     * @param paymentDetails Additional payment details (e.g., card number, expiry)
     * @return A PaymentResultDTO containing the transaction result
     * @throws RemoteException If a remote communication error occurs
     */
    PaymentResultDTO processPayment(UserDTO currentUser, Long orderId, BigDecimal amount, 
                                   String paymentMethod, String paymentDetails) throws RemoteException;
    
    /**
     * Get payment status for an order
     * 
     * @param currentUser The authenticated user requesting payment status
     * @param orderId The ID of the order
     * @return A PaymentResultDTO containing the payment status
     * @throws RemoteException If a remote communication error occurs
     */
    PaymentResultDTO getPaymentStatus(UserDTO currentUser, Long orderId) throws RemoteException;
    
    /**
     * Get all payment records (admin only)
     * 
     * @param currentUser The authenticated user requesting payment records
     * @return A list of PaymentDTO objects containing all payment records
     * @throws RemoteException If a remote communication error occurs
     */
    List<PaymentDTO> getAllPayments(UserDTO currentUser) throws RemoteException;
}
