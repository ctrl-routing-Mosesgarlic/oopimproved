package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.OrderItemDTO;
import com.drinks.rmi.dto.UserDTO;

/**
 * RMI interface for order management
 */
public interface OrderService extends Remote {
    
    /**
     * Place a new order
     * 
     * @param currentUser The current user making the request
     * @param customerId The customer ID
     * @param branchId The branch ID
     * @param items Map of drink IDs to quantities
     * @return The created order
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    OrderDTO placeOrder(UserDTO currentUser, Long customerId, Long branchId, Map<Long, Integer> items) throws RemoteException;
    
    /**
     * Get all orders for a customer
     * 
     * @param currentUser The current user making the request
     * @param customerId The customer ID
     * @return List of orders for the customer
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderDTO> getOrdersByCustomer(UserDTO currentUser, Long customerId) throws RemoteException;
    
    /**
     * Get all orders for a branch
     * 
     * @param currentUser The current user making the request
     * @param branchId The branch ID
     * @return List of orders for the branch
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderDTO> getOrdersByBranch(UserDTO currentUser, Long branchId) throws RemoteException;
    
    /**
     * Get an order by ID
     * 
     * @param currentUser The current user making the request
     * @param orderId The order ID
     * @return The order if found, null otherwise
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    OrderDTO getOrderById(UserDTO currentUser, Long orderId) throws RemoteException;
    
    /**
     * Get all order items for an order
     * 
     * @param currentUser The current user making the request
     * @param orderId The order ID
     * @return List of order items for the order
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderItemDTO> getOrderItemsByOrder(UserDTO currentUser, Long orderId) throws RemoteException;
    
    /**
     * Cancel an order (if possible)
     * 
     * @param currentUser The current user making the request
     * @param orderId The order ID
     * @return true if cancelled, false otherwise
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    boolean cancelOrder(UserDTO currentUser, Long orderId) throws RemoteException;
    
    /**
     * Get all orders for a branch
     * 
     * @param branchId The branch ID
     * @return List of orders for the branch
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderDTO> getBranchOrders(Long branchId) throws RemoteException;
    
    /**
     * Update the status of an order
     * 
     * @param orderId The order ID
     * @param status The new status
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    void updateOrderStatus(Long orderId, String status) throws RemoteException;
    
    /**
     * Create a new order
     * 
     * @param order The order to create
     * @param items The order items
     * @return The created order
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    OrderDTO createOrder(OrderDTO order, List<OrderItemDTO> items) throws RemoteException;
    
    /**
     * Get all orders
     * 
     * @return List of all orders
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderDTO> getAllOrders() throws RemoteException;
    
    /**
     * Get all order items for an order
     * 
     * @param orderId The order ID
     * @return List of order items for the order
     * @throws RemoteException RMI exception if user doesn't have permission
     */
    List<OrderItemDTO> getOrderItems(Long orderId) throws RemoteException;
}
