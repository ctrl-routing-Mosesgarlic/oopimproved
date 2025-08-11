package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.AuthService;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of AuthService for user authentication and management
 */
public class AuthServiceImpl extends UnicastRemoteObject implements AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    public AuthServiceImpl() throws RemoteException {
        super();
    }
    
    @Override
    public UserDTO login(String username, String password) throws RemoteException {
        logger.info("Login attempt for username: {}", username);
        
        String sql = """
            SELECT u.id, u.username, u.role, u.branch_id, b.name as branch_name, 
                   u.customer_id, c.name as customer_name
            FROM users u
            LEFT JOIN branches b ON u.branch_id = b.id
            LEFT JOIN customers c ON u.customer_id = c.id
            WHERE u.username = ?
            """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = getPasswordHash(username);
                if (storedHash != null && BCrypt.checkpw(password, storedHash)) {
                    UserDTO user = new UserDTO(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getObject("branch_id") == null ? null : rs.getLong("branch_id"),
                        rs.getString("branch_name"),
                        rs.getObject("customer_id") == null ? null : rs.getLong("customer_id"),
                        rs.getString("customer_name")
                    );
                    
                    logger.info("Login successful for user: {} with role: {}", username, user.getRole());
                    return user;
                }
            }
            
            logger.warn("Login failed for username: {}", username);
            return null;
            
        } catch (SQLException e) {
            logger.error("Database error during login for username: {}", username, e);
            throw new RemoteException("Authentication failed due to database error", e);
        }
    }
    
    @Override
    public UserDTO registerCustomer(String username, String password, String name, String email, String phone) throws RemoteException {
        logger.info("Customer registration attempt for username: {}", username);
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // First, create the customer record
                String customerSql = "INSERT INTO customers (name, email, phone) VALUES (?, ?, ?)";
                PreparedStatement customerStmt = conn.prepareStatement(customerSql, Statement.RETURN_GENERATED_KEYS);
                customerStmt.setString(1, name);
                customerStmt.setString(2, email);
                customerStmt.setString(3, phone);
                
                int customerRows = customerStmt.executeUpdate();
                if (customerRows == 0) {
                    throw new SQLException("Creating customer failed, no rows affected");
                }
                
                ResultSet customerKeys = customerStmt.getGeneratedKeys();
                if (!customerKeys.next()) {
                    throw new SQLException("Creating customer failed, no ID obtained");
                }
                
                Long customerId = customerKeys.getLong(1);
                
                // Then, create the user record
                String userSql = "INSERT INTO users (username, password_hash, role, customer_id) VALUES (?, ?, 'customer', ?)";
                PreparedStatement userStmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, username);
                userStmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                userStmt.setLong(3, customerId);
                
                int userRows = userStmt.executeUpdate();
                if (userRows == 0) {
                    throw new SQLException("Creating user failed, no rows affected");
                }
                
                ResultSet userKeys = userStmt.getGeneratedKeys();
                if (!userKeys.next()) {
                    throw new SQLException("Creating user failed, no ID obtained");
                }
                
                Long userId = userKeys.getLong(1);
                
                conn.commit();
                
                UserDTO user = new UserDTO(userId, username, "customer", null, null, customerId, name);
                logger.info("Customer registration successful for username: {}", username);
                return user;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            logger.error("Database error during customer registration for username: {}", username, e);
            throw new RemoteException("Customer registration failed due to database error", e);
        }
    }
    
    @Override
    public UserDTO createStaffUser(String username, String password, String role, Long branchId) throws RemoteException {
        logger.info("Staff user creation attempt for username: {} with role: {}", username, role);
        
        String sql = "INSERT INTO users (username, password_hash, role, branch_id) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, username);
            stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            stmt.setString(3, role);
            if (branchId != null) {
                stmt.setLong(4, branchId);
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Creating staff user failed, no rows affected");
            }
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (!keys.next()) {
                throw new SQLException("Creating staff user failed, no ID obtained");
            }
            
            Long userId = keys.getLong(1);
            
            // Get branch name if branchId is provided
            String branchName = null;
            if (branchId != null) {
                branchName = getBranchName(branchId);
            }
            
            UserDTO user = new UserDTO(userId, username, role, branchId, branchName, null, null);
            logger.info("Staff user creation successful for username: {} with role: {}", username, role);
            return user;
            
        } catch (SQLException e) {
            logger.error("Database error during staff user creation for username: {}", username, e);
            throw new RemoteException("Staff user creation failed due to database error", e);
        }
    }
    
    @Override
    public List<UserDTO> getAllUsers(UserDTO currentUser) throws RemoteException {
        logger.info("Fetching all users (requested by {} - {})", currentUser.getUsername(), currentUser.getRole());
        
        List<UserDTO> users = new ArrayList<>();
        String sql = """
            SELECT u.id, u.username, u.role, u.branch_id, b.name as branch_name,
                   u.customer_id, c.name as customer_name
            FROM users u
            LEFT JOIN branches b ON u.branch_id = b.id
            LEFT JOIN customers c ON u.customer_id = c.id
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                UserDTO user = new UserDTO(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getObject("branch_id", Long.class),
                    rs.getString("branch_name"),
                    rs.getObject("customer_id", Long.class),
                    rs.getString("customer_name")
                );
                users.add(user);
            }
        } catch (SQLException e) {
            logger.error("Error fetching users", e);
            throw new RemoteException("Error fetching users", e);
        }
        
        return users;
    }
    
    @Override
    public boolean deleteUser(Long userId, UserDTO currentUser) throws RemoteException {
        logger.info("Delete user attempt for user ID: {} (requested by {} - {})", userId, currentUser.getUsername(), currentUser.getRole());
        
        // Check if current user has admin privileges
        if (!"admin".equals(currentUser.getRole())) {
            logger.warn("Unauthorized delete user attempt by user: {} with role: {}", currentUser.getUsername(), currentUser.getRole());
            throw new RemoteException("Only admin users can delete users");
        }
        
        // Prevent admin from deleting themselves
        if (userId.equals(currentUser.getId())) {
            logger.warn("Admin user {} attempted to delete themselves", currentUser.getUsername());
            throw new RemoteException("You cannot delete your own account");
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // First, check if user exists and get their details
                String checkUserSql = "SELECT username, role, customer_id FROM users WHERE id = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkUserSql);
                checkStmt.setLong(1, userId);
                ResultSet rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    logger.warn("Attempted to delete non-existent user with ID: {}", userId);
                    throw new RemoteException("User not found");
                }
                
                String username = rs.getString("username");
                String role = rs.getString("role");
                Long customerId = rs.getObject("customer_id", Long.class);
                
                // If user is a customer, delete related customer record first
                if (customerId != null) {
                    String deleteCustomerSql = "DELETE FROM customers WHERE id = ?";
                    PreparedStatement deleteCustomerStmt = conn.prepareStatement(deleteCustomerSql);
                    deleteCustomerStmt.setLong(1, customerId);
                    deleteCustomerStmt.executeUpdate();
                    logger.info("Deleted customer record for customer ID: {}", customerId);
                }
                
                // Delete the user record
                String deleteUserSql = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSql);
                deleteUserStmt.setLong(1, userId);
                
                int rowsAffected = deleteUserStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    logger.info("Successfully deleted user: {} (ID: {}, Role: {})", username, userId, role);
                    return true;
                } else {
                    conn.rollback();
                    logger.warn("Failed to delete user with ID: {} - no rows affected", userId);
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                logger.error("Database error during user deletion for user ID: {}", userId, e);
                throw new RemoteException("Failed to delete user due to database error: " + e.getMessage(), e);
            }
            
        } catch (SQLException e) {
            logger.error("Database connection error during user deletion for user ID: {}", userId, e);
            throw new RemoteException("Failed to delete user due to database connection error", e);
        }
    }
    
    @Override
    public int updateUserRoles(String fromRole, String toRole, UserDTO currentUser) throws RemoteException {
        // Only admin can update user roles
        if (!"admin".equals(currentUser.getRole())) {
            throw new RemoteException("Only admin users can update user roles");
        }
        
        String sql = "UPDATE users SET role = ? WHERE role = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, toRole);
            stmt.setString(2, fromRole);
            
            int rowsUpdated = stmt.executeUpdate();
            logger.info("Updated {} users from role '{}' to '{}'", rowsUpdated, fromRole, toRole);
            return rowsUpdated;
            
        } catch (SQLException e) {
            logger.error("Database error while updating user roles from {} to {}", fromRole, toRole, e);
            throw new RemoteException("Failed to update user roles", e);
        }
    }
    
    private String getPasswordHash(String username) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("password_hash");
            }
            return null;
        }
    }
    
    private String getBranchName(Long branchId) throws SQLException {
        String sql = "SELECT name FROM branches WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name");
            }
            return null;
        }
    }
}
