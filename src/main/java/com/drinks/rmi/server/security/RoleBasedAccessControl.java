package com.drinks.rmi.server.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drinks.rmi.dto.UserDTO;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Role-Based Access Control (RBAC) utility for RMI services
 * Enforces role-based security checks for service methods
 */
public class RoleBasedAccessControl {
    
    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAccessControl.class);
    
    // Define role hierarchies and permissions
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();
    
    static {
        // Admin has all permissions
        Set<String> adminPermissions = new HashSet<>(Arrays.asList(
            "user:create", "user:read", "user:update", "user:delete",
            "drink:create", "drink:read", "drink:update", "drink:delete",
            "stock:create", "stock:read", "stock:update", "stock:delete",
            "order:create", "order:read", "order:update", "order:delete",
            "report:create", "report:read", "report:update", "report:delete",
            "branch:create", "branch:read", "branch:update", "branch:delete",
            "notification:create", "notification:read", "notification:update", "notification:delete",
            "system:configure"
        ));
        ROLE_PERMISSIONS.put("admin", adminPermissions);
        
        // Global Manager permissions
        Set<String> globalManagerPermissions = new HashSet<>(Arrays.asList(
            "user:read",
            "drink:read", "drink:update",
            "stock:read",
            "order:read",
            "report:read", "report:create",
            "branch:read",
            "notification:read", "notification:create"
        ));
        ROLE_PERMISSIONS.put("global_manager", globalManagerPermissions);
        
        // Branch Manager permissions
        Set<String> branchManagerPermissions = new HashSet<>(Arrays.asList(
            "user:read",
            "drink:read",
            "stock:read", "stock:update",
            "order:read", "order:update",
            "report:read", "report:create",
            "notification:read", "notification:create"
        ));
        ROLE_PERMISSIONS.put("branch_manager", branchManagerPermissions);
        
        // Branch Staff permissions
        Set<String> branchStaffPermissions = new HashSet<>(Arrays.asList(
            "drink:read",
            "stock:read", "stock:update",
            "order:read", "order:update", "order:create",
            "notification:read"
        ));
        ROLE_PERMISSIONS.put("branch_staff", branchStaffPermissions);
        
        // Customer permissions
        Set<String> customerPermissions = new HashSet<>(Arrays.asList(
            "drink:read",
            "order:create", "order:read",
            "notification:read"
        ));
        ROLE_PERMISSIONS.put("customer", customerPermissions);
        
        // Auditor permissions
        Set<String> auditorPermissions = new HashSet<>(Arrays.asList(
            "user:read",
            "drink:read",
            "stock:read",
            "order:read",
            "report:read", "report:create",
            "branch:read"
        ));
        ROLE_PERMISSIONS.put("auditor", auditorPermissions);
        
        // Customer Support permissions
        Set<String> customerSupportPermissions = new HashSet<>(Arrays.asList(
            "user:read",
            "drink:read",
            "order:read", "order:update",
            "notification:read", "notification:create"
        ));
        ROLE_PERMISSIONS.put("customer_support", customerSupportPermissions);
    }
    
    /**
     * Check if a user has the required permission
     * 
     * @param user The user to check
     * @param requiredPermission The permission required
     * @return true if the user has the required permission, false otherwise
     */
    public static boolean hasPermission(UserDTO user, String requiredPermission) {
        if (user == null || user.getRole() == null) {
            logger.warn("Access denied: User or role is null");
            return false;
        }
        
        String role = user.getRole().toLowerCase();
        Set<String> permissions = ROLE_PERMISSIONS.get(role);
        
        if (permissions == null) {
            logger.warn("Access denied: Unknown role '{}'", role);
            return false;
        }
        
        boolean hasAccess = permissions.contains(requiredPermission);
        if (!hasAccess) {
            logger.warn("Access denied: User '{}' with role '{}' does not have permission '{}'", 
                user.getUsername(), role, requiredPermission);
        }
        
        return hasAccess;
    }
    
    /**
     * Check if a user has the required permission, throw RemoteException if not
     * 
     * @param user The user to check
     * @param requiredPermission The permission required
     * @throws RemoteException if the user does not have the required permission
     */
    public static void checkPermission(UserDTO user, String requiredPermission) throws RemoteException {
        if (!hasPermission(user, requiredPermission)) {
            String message = "Access denied: Insufficient permissions";
            logger.error(message);
            throw new RemoteException(message);
        }
    }
    
    /**
     * Check if a user belongs to a branch, throw RemoteException if not
     * Used for branch-specific operations
     * 
     * @param user The user to check
     * @param branchId The branch ID to check against
     * @throws RemoteException if the user does not belong to the branch
     */
    public static void checkBranchAccess(UserDTO user, Long branchId) throws RemoteException {
        // Admin and global managers have access to all branches
        if (user.getRole().equalsIgnoreCase("admin") || 
            user.getRole().equalsIgnoreCase("global_manager") ||
            user.getRole().equalsIgnoreCase("auditor")) {
            return;
        }
        
        // Other roles must belong to the branch
        if (user.getBranchId() == null || !user.getBranchId().equals(branchId)) {
            String message = "Access denied: User does not have access to this branch";
            logger.error(message);
            throw new RemoteException(message);
        }
    }
    
    /**
     * Check if a user has access to a specific order
     * Used for order-specific operations
     * 
     * @param user The user to check
     * @param orderId The order ID to check
     * @param orderBranchId The branch ID of the order
     * @param orderCustomerId The customer ID of the order
     * @throws RemoteException if the user does not have access to the order
     */
    public static void checkOrderAccess(UserDTO user, Long orderId, Long orderBranchId, Long orderCustomerId) throws RemoteException {
        String role = user.getRole().toLowerCase();
        
        // Admin, global managers, and auditors have access to all orders
        if (role.equals("admin") || role.equals("global_manager") || role.equals("auditor")) {
            return;
        }
        
        // Branch managers and staff have access to orders in their branch
        if ((role.equals("branch_manager") || role.equals("branch_staff") || role.equals("customer_support")) 
                && user.getBranchId() != null && user.getBranchId().equals(orderBranchId)) {
            return;
        }
        
        // Customers have access only to their own orders
        if (role.equals("customer") && user.getCustomerId() != null && user.getCustomerId().equals(orderCustomerId)) {
            return;
        }
        
        String message = "Access denied: User does not have access to this order";
        logger.error(message);
        throw new RemoteException(message);
    }
}
