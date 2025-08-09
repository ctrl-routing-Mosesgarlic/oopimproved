package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.OrderItemDTO;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.*;
import com.drinks.rmi.server.security.RoleBasedAccessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of OrderService for order management
 */
public class OrderServiceImpl extends UnicastRemoteObject implements OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final StockService stockService;
    private final DrinkService drinkService;
    private Connection connection;
    
    public OrderServiceImpl(StockService stockService, DrinkService drinkService) throws RemoteException {
        super();
        this.stockService = stockService;
        this.drinkService = drinkService;
        try {
            this.connection = DatabaseConfig.getConnection();
        } catch (SQLException e) {
            throw new RemoteException("Failed to initialize database connection", e);
        }
    }
    
    @Override
    public OrderDTO placeOrder(UserDTO currentUser, Long customerId, Long branchId, Map<Long, Integer> items) throws RemoteException {
        // Check if user has permission to place orders
        RoleBasedAccessControl.checkPermission(currentUser, "order:create");
        
        // If customer is placing their own order, verify it's their own account
        if (currentUser.getRole().equals("customer") && !currentUser.getCustomerId().equals(customerId)) {
            logger.error("Customer {} attempted to place order for different customer {}", currentUser.getCustomerId(), customerId);
            throw new RemoteException("You can only place orders for your own account");
        }
        
        // Branch staff can only place orders for their own branch
        if (currentUser.getRole().equals("branch_staff") && !currentUser.getBranchId().equals(branchId)) {
            logger.error("Branch staff from branch {} attempted to place order for branch {}", currentUser.getBranchId(), branchId);
            throw new RemoteException("You can only place orders for your own branch");
        }
        logger.info("Placing order for customer ID: {} at branch ID: {} with {} items", customerId, branchId, items.size());
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Validate stock availability for all items first
                for (Map.Entry<Long, Integer> item : items.entrySet()) {
                    Long drinkId = item.getKey();
                    Integer quantity = item.getValue();
                    
                    StockDTO stock = stockService.getStockByBranchAndDrink(branchId, drinkId);
                    if (stock == null || stock.getQuantity() < quantity) {
                        DrinkDTO drink = drinkService.getDrinkById(drinkId);
                        String drinkName = drink != null ? drink.getName() : "Unknown";
                        logger.warn("Insufficient stock for drink: {} (ID: {}). Available: {}, Requested: {}", 
                                   drinkName, drinkId, stock != null ? stock.getQuantity() : 0, quantity);
                        conn.rollback();
                        throw new RemoteException("Insufficient stock for " + drinkName + ". Available: " + 
                                                 (stock != null ? stock.getQuantity() : 0) + ", Requested: " + quantity);
                    }
                }
                
                // Calculate total amount first
                double totalAmount = 0.0;
                for (Map.Entry<Long, Integer> item : items.entrySet()) {
                    Long drinkId = item.getKey();
                    Integer quantity = item.getValue();
                    
                    // Calculate total amount
                    DrinkDTO drink = drinkService.getDrinkById(drinkId);
                    if (drink != null) {
                        totalAmount += drink.getPrice().doubleValue() * quantity;
                    }
                }
                
                // Create the order
                String orderSql = "INSERT INTO orders (customer_id, branch_id, order_time, status, total_amount) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
                orderStmt.setLong(1, customerId);
                orderStmt.setLong(2, branchId);
                orderStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                orderStmt.setString(4, "PENDING");
                orderStmt.setDouble(5, totalAmount);
                
                int orderRows = orderStmt.executeUpdate();
                if (orderRows == 0) {
                    throw new SQLException("Creating order failed, no rows affected");
                }
                
                ResultSet orderKeys = orderStmt.getGeneratedKeys();
                if (!orderKeys.next()) {
                    throw new SQLException("Creating order failed, no ID obtained");
                }
                
                Long orderId = orderKeys.getLong(1);
                
                // Create order items and update stock
                String itemSql = "INSERT INTO order_items (order_id, drink_id, drink_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                
                for (Map.Entry<Long, Integer> item : items.entrySet()) {
                    Long drinkId = item.getKey();
                    Integer quantity = item.getValue();
                    
                    // Get drink details
                    DrinkDTO drink = drinkService.getDrinkById(drinkId);
                    if (drink == null) {
                        continue;
                    }
                    
                    // Add order item
                    itemStmt.setLong(1, orderId);
                    itemStmt.setLong(2, drinkId);
                    itemStmt.setString(3, drink.getName());
                    itemStmt.setInt(4, quantity);
                    itemStmt.setBigDecimal(5, drink.getPrice());
                    itemStmt.executeUpdate();
                    
                    // Update stock (reduce quantity)
                    StockDTO stock = stockService.getStockByBranchAndDrink(branchId, drinkId);
                    stockService.updateStockQuantity(branchId, drinkId, stock.getQuantity() - quantity);
                }
                
                conn.commit();
                
                // Return the created order
                OrderDTO order = getOrderById(currentUser, orderId);
                if (order != null) {
                    order.setTotalAmount(totalAmount);
                }
                
                logger.info("Order placed successfully with ID: {} for total amount: {}", orderId, totalAmount);
                return order;
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Database error while placing order for customer ID: {}", customerId, e);
            throw new RemoteException("Failed to place order due to database error", e);
        }
    }
    
    @Override
    public List<OrderDTO> getOrdersByCustomer(UserDTO currentUser, Long customerId) throws RemoteException {
        // Check if user has permission to view orders
        RoleBasedAccessControl.checkPermission(currentUser, "order:read");
        
        // If customer is viewing their own orders, verify it's their own account
        if (currentUser.getRole().equals("customer")) {
            logger.info("Authorization check - User: {} (ID: {}, CustomerID: {}) requesting orders for customer ID: {}", 
                      currentUser.getUsername(), currentUser.getId(), currentUser.getCustomerId(), customerId);
                      
            if (!currentUser.getCustomerId().equals(customerId)) {
                logger.error("Customer {} attempted to view orders for different customer {}", currentUser.getCustomerId(), customerId);
                throw new RemoteException("You can only view your own orders");
            }
        }
        
        List<OrderDTO> orders = new ArrayList<>();
        String sql = """
            SELECT o.id, o.customer_id, c.name as customer_name, 
                   o.branch_id, b.name as branch_name, 
                   o.order_time, o.status, o.total_amount
            FROM orders o
            LEFT JOIN customers c ON o.customer_id = c.id
            LEFT JOIN branches b ON o.branch_id = b.id
            WHERE o.customer_id = ?
            ORDER BY o.order_time DESC
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderDTO order = new OrderDTO(
                    rs.getLong("id"),
                    rs.getLong("customer_id"),
                    rs.getString("customer_name"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getTimestamp("order_time").toLocalDateTime()
                );
                
                // Set additional fields separately
                order.setStatus(rs.getString("status"));
                order.setTotalAmount(rs.getBigDecimal("total_amount").doubleValue());
                
                // Get order items
                List<OrderItemDTO> items = getOrderItems(order.getId());
                order.setItems(items);
                
                // Calculate total amount from items if not set
                if (order.getTotalAmount() == 0 && items != null && !items.isEmpty()) {
                    double totalAmount = items.stream()
                        .mapToDouble(item -> item.getSubtotal().doubleValue())
                        .sum();
                    order.setTotalAmount(totalAmount);
                }
                
                orders.add(order);
            }
            
            logger.info("Retrieved {} orders for customer ID: {}", orders.size(), customerId);
            return orders;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving orders for customer ID: {}", customerId, e);
            throw new RemoteException("Failed to retrieve orders due to database error", e);
        }
    }
    
    @Override
    public List<OrderDTO> getOrdersByBranch(UserDTO currentUser, Long branchId) throws RemoteException {
        // Check if user has permission to view orders
        RoleBasedAccessControl.checkPermission(currentUser, "order:read");
        
        // Branch staff and branch managers can only view orders for their own branch
        if ((currentUser.getRole().equals("branch_staff") || currentUser.getRole().equals("branch_manager")) 
            && !currentUser.getBranchId().equals(branchId)) {
            logger.error("User from branch {} attempted to view orders for branch {}", currentUser.getBranchId(), branchId);
            throw new RemoteException("You can only view orders for your own branch");
        }
        logger.info("Retrieving orders for branch ID: {}", branchId);
        
        String sql = """
            SELECT o.id, o.customer_id, c.name as customer_name, o.branch_id, b.name as branch_name, o.order_time
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN branches b ON o.branch_id = b.id
            WHERE o.branch_id = ?
            ORDER BY o.order_time DESC
            """;
        
        List<OrderDTO> orders = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderDTO order = new OrderDTO(
                    rs.getLong("id"),
                    rs.getLong("customer_id"),
                    rs.getString("customer_name"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getTimestamp("order_time").toLocalDateTime()
                );
                
                // Load order items
                List<OrderItemDTO> items = getOrderItemsByOrder(currentUser, order.getId());
                order.setItems(items);
                
                // Calculate total amount
                double totalAmount = items.stream()
                    .mapToDouble(item -> item.getSubtotal().doubleValue())
                    .sum();
                order.setTotalAmount(totalAmount);
                
                orders.add(order);
            }
            
            logger.info("Retrieved {} orders for branch ID: {}", orders.size(), branchId);
            return orders;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving orders for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to retrieve orders due to database error", e);
        }
    }
    
    @Override
    public OrderDTO getOrderById(UserDTO currentUser, Long orderId) throws RemoteException {
        // Check if user has permission to view orders
        RoleBasedAccessControl.checkPermission(currentUser, "order:read");
        
        logger.info("Retrieving order with ID: {}", orderId);
        
        String sql = """
            SELECT o.id, o.customer_id, c.name as customer_name, 
                   o.branch_id, b.name as branch_name, 
                   o.order_time, o.status, o.total_amount
            FROM orders o
            LEFT JOIN customers c ON o.customer_id = c.id
            LEFT JOIN branches b ON o.branch_id = b.id
            WHERE o.id = ?
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                OrderDTO order = new OrderDTO(
                    rs.getLong("id"),
                    rs.getLong("customer_id"),
                    rs.getString("customer_name"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getTimestamp("order_time").toLocalDateTime()
                );
                
                // Set additional fields separately
                order.setStatus(rs.getString("status"));
                order.setTotalAmount(rs.getBigDecimal("total_amount").doubleValue());
                
                // Additional permission check - customers can only view their own orders
                if (currentUser.getRole().equals("customer") && 
                    !currentUser.getCustomerId().equals(order.getCustomerId())) {
                    logger.error("Customer {} attempted to view order {} belonging to customer {}", 
                               currentUser.getCustomerId(), orderId, order.getCustomerId());
                    throw new RemoteException("You can only view your own orders");
                }
                
                // Branch staff can only view orders from their own branch
                if (currentUser.getRole().equals("branch_staff") && 
                    !currentUser.getBranchId().equals(order.getBranchId())) {
                    logger.error("Branch staff from branch {} attempted to view order from branch {}", 
                               currentUser.getBranchId(), order.getBranchId());
                    throw new RemoteException("You can only view orders from your own branch");
                }
                
                return order;
            }
            
            return null;
            
        } catch (SQLException e) {
            logger.error("Error retrieving order with ID: {}", orderId, e);
            throw new RemoteException("Error retrieving order", e);
        }
    }
    
    @Override
    public List<OrderItemDTO> getOrderItemsByOrder(UserDTO currentUser, Long orderId) throws RemoteException {
        // Check if user has permission to view order items
        RoleBasedAccessControl.checkPermission(currentUser, "order:read");
        
        // We'll check order-specific permissions in getOrderById, which should be called before this method
        // This method is often called internally by getOrderById
        logger.debug("Retrieving order items for order ID: {}", orderId);
        
        String sql = """
            SELECT oi.id, oi.order_id, oi.drink_id, d.name as drink_name, oi.quantity, d.price
            FROM order_items oi
            JOIN drinks d ON oi.drink_id = d.id
            WHERE oi.order_id = ?
            ORDER BY d.name
            """;
        
        List<OrderItemDTO> items = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderItemDTO item = new OrderItemDTO(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("drink_id"),
                    rs.getString("drink_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("price")
                );
                items.add(item);
            }
            
            logger.debug("Retrieved {} order items for order ID: {}", items.size(), orderId);
            return items;
            
        } catch (SQLException e) {
            logger.error("Database error while retrieving order items for order ID: {}", orderId, e);
            throw new RemoteException("Failed to retrieve order items due to database error", e);
        }
    }
    
    @Override
    public List<OrderItemDTO> getOrderItems(Long orderId) throws RemoteException {
        logger.info("Fetching order items for order ID: {}", orderId);
        
        List<OrderItemDTO> items = new ArrayList<>();
        String sql = """
            SELECT oi.id, oi.order_id, oi.drink_id, d.name as drink_name, 
                   oi.quantity, oi.unit_price, oi.total_price
            FROM order_items oi
            JOIN drinks d ON oi.drink_id = d.id
            WHERE oi.order_id = ?
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, orderId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderItemDTO item = new OrderItemDTO(
                    rs.getLong("id"),
                    rs.getLong("order_id"),
                    rs.getLong("drink_id"),
                    rs.getString("drink_name"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("unit_price")
                );
                items.add(item);
            }
        } catch (SQLException e) {
            logger.error("Error fetching order items", e);
            throw new RemoteException("Error fetching order items", e);
        }
        
        return items;
    }
    
    @Override
    public boolean cancelOrder(UserDTO currentUser, Long orderId) throws RemoteException {
        // Check if user has permission to cancel orders
        RoleBasedAccessControl.checkPermission(currentUser, "order:update");
        
        // Get order details first to check ownership
        OrderDTO order = getOrderById(currentUser, orderId);
        if (order == null) {
            logger.warn("Cannot cancel order - not found with ID: {}", orderId);
            return false;
        }
        
        // Additional permission checks based on role
        if (currentUser.getRole().equals("customer") && !currentUser.getId().equals(order.getCustomerId())) {
            logger.error("Customer {} attempted to cancel order for customer {}", currentUser.getId(), order.getCustomerId());
            throw new RemoteException("You can only cancel your own orders");
        }
        
        if ((currentUser.getRole().equals("branch_staff") || currentUser.getRole().equals("branch_manager")) 
            && !currentUser.getBranchId().equals(order.getBranchId())) {
            logger.error("User from branch {} attempted to cancel order from branch {}", 
                        currentUser.getBranchId(), order.getBranchId());
            throw new RemoteException("You can only cancel orders for your own branch");
        }
        logger.info("Attempting to cancel order with ID: {}", orderId);
        
        // For simplicity, we'll just delete the order and restore stock
        // In a real system, you might want to mark it as cancelled instead
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // We already retrieved and validated the order above
                
                // Restore stock for each item
                for (OrderItemDTO item : order.getItems()) {
                    StockDTO stock = stockService.getStockByBranchAndDrink(order.getBranchId(), item.getDrinkId());
                    if (stock != null) {
                        stockService.updateStockQuantity(order.getBranchId(), item.getDrinkId(), 
                                                       stock.getQuantity() + item.getQuantity());
                    }
                }
                
                // Delete order items first (due to foreign key constraint)
                String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
                PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItemsSql);
                deleteItemsStmt.setLong(1, orderId);
                deleteItemsStmt.executeUpdate();
                
                // Delete the order
                String deleteOrderSql = "DELETE FROM orders WHERE id = ?";
                PreparedStatement deleteOrderStmt = conn.prepareStatement(deleteOrderSql);
                deleteOrderStmt.setLong(1, orderId);
                int rows = deleteOrderStmt.executeUpdate();
                
                if (rows > 0) {
                    conn.commit();
                    logger.info("Order cancelled successfully with ID: {}", orderId);
                    return true;
                } else {
                    conn.rollback();
                    logger.warn("No rows deleted for order ID: {}", orderId);
                    return false;
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Database error while cancelling order with ID: {}", orderId, e);
            throw new RemoteException("Failed to cancel order due to database error", e);
        }
    }
   
    
    @Override
    public List<OrderDTO> getAllOrders() throws RemoteException {
        logger.info("Retrieving all orders");
        
        List<OrderDTO> orders = new ArrayList<>();
        String sql = """
            SELECT o.id, o.customer_id, c.name as customer_name, 
                   o.branch_id, b.name as branch_name, 
                   o.order_time, o.status, o.total_amount
            FROM orders o
            LEFT JOIN customers c ON o.customer_id = c.id
            LEFT JOIN branches b ON o.branch_id = b.id
            ORDER BY o.order_time DESC
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                OrderDTO order = new OrderDTO(
                    rs.getLong("id"),
                    rs.getLong("customer_id"),
                    rs.getString("customer_name"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getTimestamp("order_time").toLocalDateTime()
                );
                
                // Set additional fields separately
                order.setStatus(rs.getString("status"));
                order.setTotalAmount(rs.getBigDecimal("total_amount").doubleValue());
                
                orders.add(order);
            }
            return orders;
        } catch (SQLException e) {
            logger.error("Error retrieving all orders", e);
            throw new RemoteException("Error retrieving orders", e);
        }
    }
    
    @Override
    public OrderDTO createOrder(OrderDTO order, List<OrderItemDTO> items) throws RemoteException {
        try {
            // First insert the order
            String sql = "INSERT INTO orders (customer_id, branch_id, order_time, status, total_amount) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, order.getCustomerId());
            pstmt.setLong(2, order.getBranchId());
            pstmt.setTimestamp(3, Timestamp.valueOf(order.getOrderTime()));
            pstmt.setString(4, order.getStatus());
            pstmt.setDouble(5, order.getTotalAmount());
            
            pstmt.executeUpdate();
            
            // Get the generated order ID
            ResultSet rs = pstmt.getGeneratedKeys();
            long orderId = -1;
            if (rs.next()) {
                orderId = rs.getLong(1);
            }
            
            // Insert order items
            for (OrderItemDTO item : items) {
                String itemSql = "INSERT INTO order_items (order_id, drink_id, drink_name, quantity, unit_price) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement itemPstmt = connection.prepareStatement(itemSql);
                itemPstmt.setLong(1, orderId);
                itemPstmt.setLong(2, item.getDrinkId());
                itemPstmt.setString(3, item.getDrinkName());
                itemPstmt.setInt(4, item.getQuantity());
                itemPstmt.setBigDecimal(5, item.getUnitPrice());
                
                itemPstmt.executeUpdate();
            }
            
            // Return the created order with the generated ID
            order.setId(orderId);
            return order;
        } catch (SQLException e) {
            logger.error("Failed to create order", e);
            throw new RemoteException("Failed to create order due to database error", e);
        }
    }
    
    @Override
    public void updateOrderStatus(Long orderId, String status) throws RemoteException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?")) {
            pstmt.setString(1, status);
            pstmt.setLong(2, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update order status", e);
            throw new RemoteException("Failed to update order status due to database error", e);
        }
    }
    
    @Override
    public List<OrderDTO> getBranchOrders(Long branchId) throws RemoteException {
        logger.info("Retrieving orders for branch ID: {}", branchId);
        
        List<OrderDTO> orders = new ArrayList<>();
        String sql = """
            SELECT o.id, o.customer_id, c.name as customer_name, 
                   o.branch_id, b.name as branch_name, 
                   o.order_time, o.status, o.total_amount
            FROM orders o
            LEFT JOIN customers c ON o.customer_id = c.id
            LEFT JOIN branches b ON o.branch_id = b.id
            WHERE o.branch_id = ?
            ORDER BY o.order_time DESC
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                OrderDTO order = new OrderDTO(
                    rs.getLong("id"),
                    rs.getLong("customer_id"),
                    rs.getString("customer_name"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getTimestamp("order_time").toLocalDateTime()
                );
                
                // Set additional fields separately
                order.setStatus(rs.getString("status"));
                order.setTotalAmount(rs.getBigDecimal("total_amount").doubleValue());
                
                orders.add(order);
            }
            return orders;
        } catch (SQLException e) {
            logger.error("Error retrieving orders for branch ID: {}", branchId, e);
            throw new RemoteException("Error retrieving orders", e);
        }
    }
}
