package com.drinks.rmi.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * RMI Service for load balancing across branch servers
 * Provides smart branch selection for optimal performance
 */
public interface LoadBalancerService extends Remote {
    
    /**
     * Get the best available branch for a customer order
     * Uses round-robin or least-loaded algorithm
     * @return BranchInfo for the selected branch
     * @throws RemoteException if RMI communication fails
     */
    BranchInfo getBestAvailableBranch() throws RemoteException;
    
    /**
     * Get the best branch for a specific drink order
     * Considers stock availability and server load
     * @param drinkId ID of the drink to order
     * @param quantity Quantity needed
     * @return BranchInfo for the selected branch, or null if no branch has sufficient stock
     * @throws RemoteException if RMI communication fails
     */
    BranchInfo getBestBranchForDrink(Long drinkId, int quantity) throws RemoteException;
    
    /**
     * Get all available branches with their current status
     * @return List of all branch information
     * @throws RemoteException if RMI communication fails
     */
    List<BranchInfo> getAllBranches() throws RemoteException;
    
    /**
     * Register a branch server with the load balancer
     * @param branchInfo Branch information to register
     * @throws RemoteException if RMI communication fails
     */
    void registerBranch(BranchInfo branchInfo) throws RemoteException;
    
    /**
     * Unregister a branch server from the load balancer
     * @param branchName Name of the branch to unregister
     * @throws RemoteException if RMI communication fails
     */
    void unregisterBranch(String branchName) throws RemoteException;
    
    /**
     * Update branch server statistics
     * @param branchName Name of the branch
     * @param stats Updated statistics
     * @throws RemoteException if RMI communication fails
     */
    void updateBranchStats(String branchName, BranchStats stats) throws RemoteException;
    
    /**
     * Get current load distribution across all branches
     * @return Map of branch names to their current load percentages
     * @throws RemoteException if RMI communication fails
     */
    Map<String, Double> getLoadDistribution() throws RemoteException;
    
    /**
     * Force a specific branch selection (for testing or manual override)
     * @param branchName Name of the branch to select
     * @return BranchInfo for the specified branch
     * @throws RemoteException if RMI communication fails
     */
    BranchInfo selectBranch(String branchName) throws RemoteException;
    
    /**
     * Get health status of all branches
     * @return Map of branch names to their health status
     * @throws RemoteException if RMI communication fails
     */
    Map<String, BranchHealthStatus> getBranchHealthStatus() throws RemoteException;
    
    /**
     * Heartbeat signal from branch server to indicate it's alive
     * @param branchName Name of the branch sending heartbeat
     * @throws RemoteException if RMI communication fails
     */
    void heartbeat(String branchName) throws RemoteException;
    
    // Inner classes for data transfer
    
    /**
     * Branch information for load balancing
     */
    public static class BranchInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private String name;
        private String host;
        private int port;
        private boolean isActive;
        private BranchStats stats;
        private long lastHeartbeat;
        
        public BranchInfo() {}
        
        public BranchInfo(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
            this.isActive = true;
            this.lastHeartbeat = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
        
        public BranchStats getStats() { return stats; }
        public void setStats(BranchStats stats) { this.stats = stats; }
        
        public long getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }
        
        @Override
        public String toString() {
            return String.format("BranchInfo{name='%s', host='%s', port=%d, active=%s}", 
                name, host, port, isActive);
        }
    }
    
    /**
     * Branch statistics for load balancing decisions
     */
    public static class BranchStats implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        private int activeConnections;
        private double cpuUsage;
        private double memoryUsage;
        private int pendingOrders;
        private int totalStockItems;
        private long responseTimeMs;
        private int ordersProcessedToday;
        
        public BranchStats() {}
        
        // Getters and Setters
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
        
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public int getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }
        
        public int getTotalStockItems() { return totalStockItems; }
        public void setTotalStockItems(int totalStockItems) { this.totalStockItems = totalStockItems; }
        
        public long getResponseTimeMs() { return responseTimeMs; }
        public void setResponseTimeMs(long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
        
        public int getOrdersProcessedToday() { return ordersProcessedToday; }
        public void setOrdersProcessedToday(int ordersProcessedToday) { this.ordersProcessedToday = ordersProcessedToday; }
        
        /**
         * Calculate overall load score (0.0 to 1.0, lower is better)
         */
        public double getLoadScore() {
            double connectionLoad = Math.min(activeConnections / 100.0, 1.0);
            double cpuLoad = cpuUsage / 100.0;
            double memoryLoad = memoryUsage / 100.0;
            double orderLoad = Math.min(pendingOrders / 50.0, 1.0);
            
            return (connectionLoad + cpuLoad + memoryLoad + orderLoad) / 4.0;
        }
    }
    
    /**
     * Branch health status enumeration
     */
    public enum BranchHealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL,
        OFFLINE
    }
}
