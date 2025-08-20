package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.interfaces.LoadBalancerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of LoadBalancerService for smart branch selection
 * Uses round-robin and load-based algorithms for optimal distribution
 */
public class LoadBalancerServiceImpl extends UnicastRemoteObject implements LoadBalancerService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerServiceImpl.class);
    private static final long serialVersionUID = 1L;
    
    // Registered branches
    private final Map<String, BranchInfo> branches = new ConcurrentHashMap<>();
    
    // Round-robin counter
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);
    
    // Health check scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Default branch configurations
    private static final Map<String, Integer> DEFAULT_BRANCH_PORTS = Map.of(
        "Nakuru", 1100,
        "Mombasa", 1101,
        "Kisumu", 1102,
        "Nairobi", 1103
    );
    
    // Heartbeat monitoring
    private final Map<String, Long> lastHeartbeats = new ConcurrentHashMap<>();
    private ScheduledExecutorService heartbeatChecker;

    public LoadBalancerServiceImpl() throws RemoteException {
        super();
        logger.info("LoadBalancerService initialized");
        
        // Initialize default branches
        initializeDefaultBranches();
        
        // Start health check task
        scheduler.scheduleAtFixedRate(this::performHealthChecks, 30, 30, TimeUnit.SECONDS);
        
        // Start statistics update task
        scheduler.scheduleAtFixedRate(this::updateBranchStatistics, 60, 60, TimeUnit.SECONDS);
        
        // Start heartbeat monitoring
        this.heartbeatChecker = Executors.newSingleThreadScheduledExecutor();
        this.heartbeatChecker.scheduleAtFixedRate(this::checkHeartbeats, 5, 5, TimeUnit.SECONDS);
    }
    
    @Override
    public BranchInfo getBestAvailableBranch() throws RemoteException {
        List<BranchInfo> activeBranches = getActiveBranches();
        
        if (activeBranches.isEmpty()) {
            logger.warn("No active branches available");
            return null;
        }
        
        // Use load-based selection if we have statistics
        BranchInfo bestBranch = selectByLowestLoad(activeBranches);
        if (bestBranch != null) {
            logger.debug("Selected branch by load: {}", bestBranch.getName());
            return bestBranch;
        }
        
        // Fallback to round-robin
        bestBranch = selectByRoundRobin(activeBranches);
        logger.debug("Selected branch by round-robin: {}", bestBranch.getName());
        return bestBranch;
    }
    
    @Override
    public BranchInfo getBestBranchForDrink(Long drinkId, int quantity) throws RemoteException {
        if (drinkId == null || quantity <= 0) {
            return getBestAvailableBranch();
        }
        
        List<BranchInfo> branchesWithStock = getBranchesWithStock(drinkId, quantity);
        
        if (branchesWithStock.isEmpty()) {
            logger.warn("No branches have sufficient stock for drink {} (quantity: {})", drinkId, quantity);
            return null;
        }
        
        // Select the branch with lowest load among those with stock
        BranchInfo bestBranch = selectByLowestLoad(branchesWithStock);
        if (bestBranch == null) {
            bestBranch = selectByRoundRobin(branchesWithStock);
        }
        
        logger.debug("Selected branch {} for drink {} (quantity: {})", 
            bestBranch.getName(), drinkId, quantity);
        return bestBranch;
    }
    
    @Override
    public List<BranchInfo> getAllBranches() throws RemoteException {
        return new ArrayList<>(branches.values());
    }
    
    @Override
    public synchronized void registerBranch(BranchInfo branchInfo) throws RemoteException {
        if (branchInfo == null || branchInfo.getName() == null) {
            throw new IllegalArgumentException("Branch info and name cannot be null");
        }
        
        branchInfo.setActive(true);
        branchInfo.setLastHeartbeat(System.currentTimeMillis());
        
        branches.put(branchInfo.getName(), branchInfo);
        lastHeartbeats.put(branchInfo.getName(), System.currentTimeMillis());
        logger.info("Registered branch: {}", branchInfo);
    }
    
    @Override
    public void unregisterBranch(String branchName) throws RemoteException {
        if (branchName != null) {
            BranchInfo removed = branches.remove(branchName);
            if (removed != null) {
                logger.info("Unregistered branch: {}", branchName);
            }
            lastHeartbeats.remove(branchName);
        }
    }
    
    @Override
    public void updateBranchStats(String branchName, BranchStats stats) throws RemoteException {
        BranchInfo branch = branches.get(branchName);
        if (branch != null) {
            branch.setStats(stats);
            branch.setLastHeartbeat(System.currentTimeMillis());
            logger.debug("Updated stats for branch: {}", branchName);
        }
    }
    
    @Override
    public Map<String, Double> getLoadDistribution() throws RemoteException {
        Map<String, Double> distribution = new HashMap<>();
        
        for (BranchInfo branch : branches.values()) {
            if (branch.isActive() && branch.getStats() != null) {
                distribution.put(branch.getName(), branch.getStats().getLoadScore());
            } else {
                distribution.put(branch.getName(), 0.0);
            }
        }
        
        return distribution;
    }
    
    @Override
    public BranchInfo selectBranch(String branchName) throws RemoteException {
        BranchInfo branch = branches.get(branchName);
        if (branch != null && branch.isActive()) {
            return branch;
        }
        return null;
    }
    
    @Override
    public Map<String, BranchHealthStatus> getBranchHealthStatus() throws RemoteException {
        Map<String, BranchHealthStatus> healthStatus = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        
        for (BranchInfo branch : branches.values()) {
            BranchHealthStatus status = calculateHealthStatus(branch, currentTime);
            healthStatus.put(branch.getName(), status);
        }
        
        return healthStatus;
    }
    
    @Override
    public void heartbeat(String branchName) throws RemoteException {
        BranchInfo branch = branches.get(branchName);
        if (branch != null) {
            branch.setLastHeartbeat(System.currentTimeMillis());
            branch.setActive(true);
            lastHeartbeats.put(branchName, System.currentTimeMillis());
        }
    }
    
    // Helper methods
    
    private void initializeDefaultBranches() {
        for (Map.Entry<String, Integer> entry : DEFAULT_BRANCH_PORTS.entrySet()) {
            String branchName = entry.getKey();
            int port = entry.getValue();
            
            // Use configurable host from system property, default to localhost for backward compatibility
            String branchHost = System.getProperty("java.rmi.server.hostname", "localhost");
            BranchInfo branchInfo = new BranchInfo(branchName, branchHost, port);
            branchInfo.setStats(new BranchStats()); // Initialize with default stats
            branches.put(branchName, branchInfo);
            lastHeartbeats.put(branchName, System.currentTimeMillis());
        }
        
        logger.info("Initialized {} default branches", DEFAULT_BRANCH_PORTS.size());
    }
    
    private List<BranchInfo> getActiveBranches() {
        return branches.values().stream()
            .filter(BranchInfo::isActive)
            .toList();
    }
    
    private BranchInfo selectByLowestLoad(List<BranchInfo> availableBranches) {
        return availableBranches.stream()
            .filter(branch -> branch.getStats() != null)
            .min(Comparator.comparingDouble(branch -> branch.getStats().getLoadScore()))
            .orElse(null);
    }
    
    private BranchInfo selectByRoundRobin(List<BranchInfo> availableBranches) {
        if (availableBranches.isEmpty()) {
            return null;
        }
        
        int index = roundRobinCounter.getAndIncrement() % availableBranches.size();
        return availableBranches.get(index);
    }
    
    private List<BranchInfo> getBranchesWithStock(Long drinkId, int quantity) {
        List<BranchInfo> branchesWithStock = new ArrayList<>();
        
        String sql = "SELECT b.name FROM branches b " +
                    "JOIN stocks s ON b.id = s.branch_id " +
                    "WHERE s.drink_id = ? AND s.quantity >= ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, drinkId);
            stmt.setInt(2, quantity);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String branchName = rs.getString("name");
                    BranchInfo branch = branches.get(branchName);
                    if (branch != null && branch.isActive()) {
                        branchesWithStock.add(branch);
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.error("Failed to check stock availability for drink: {}", drinkId, e);
        }
        
        return branchesWithStock;
    }
    
    private BranchHealthStatus calculateHealthStatus(BranchInfo branch, long currentTime) {
        if (!branch.isActive()) {
            return BranchHealthStatus.OFFLINE;
        }
        
        // Check heartbeat (consider offline if no heartbeat for 2 minutes)
        long timeSinceHeartbeat = currentTime - branch.getLastHeartbeat();
        if (timeSinceHeartbeat > 120000) { // 2 minutes
            return BranchHealthStatus.OFFLINE;
        }
        
        BranchStats stats = branch.getStats();
        if (stats == null) {
            return BranchHealthStatus.WARNING;
        }
        
        double loadScore = stats.getLoadScore();
        
        if (loadScore > 0.8) {
            return BranchHealthStatus.CRITICAL;
        } else if (loadScore > 0.6) {
            return BranchHealthStatus.WARNING;
        } else {
            return BranchHealthStatus.HEALTHY;
        }
    }
    
    private void performHealthChecks() {
        long currentTime = System.currentTimeMillis();
        List<String> offlineBranches = new ArrayList<>();
        
        for (BranchInfo branch : branches.values()) {
            long timeSinceHeartbeat = currentTime - branch.getLastHeartbeat();
            
            if (timeSinceHeartbeat > 120000 && branch.isActive()) { // 2 minutes
                branch.setActive(false);
                offlineBranches.add(branch.getName());
            }
        }
        
        if (!offlineBranches.isEmpty()) {
            logger.warn("Marked {} branches as offline due to missed heartbeats: {}", 
                offlineBranches.size(), offlineBranches);
        }
    }
    
    private void updateBranchStatistics() {
        // Update statistics from database for each branch
        for (BranchInfo branch : branches.values()) {
            if (branch.isActive()) {
                updateBranchStatsFromDatabase(branch);
            }
        }
    }
    
    private void updateBranchStatsFromDatabase(BranchInfo branch) {
        String branchName = branch.getName();
        BranchStats stats = branch.getStats();
        if (stats == null) {
            stats = new BranchStats();
            branch.setStats(stats);
        }
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            // Update total stock items
            String stockSql = "SELECT COUNT(*) FROM stocks s JOIN branches b ON s.branch_id = b.id WHERE b.name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(stockSql)) {
                stmt.setString(1, branchName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.setTotalStockItems(rs.getInt(1));
                    }
                }
            }
            
            // Update orders processed today
            String orderSql = "SELECT COUNT(*) FROM orders o JOIN branches b ON o.branch_id = b.id " +
                             "WHERE b.name = ? AND DATE(o.order_time) = CURDATE()";
            try (PreparedStatement stmt = conn.prepareStatement(orderSql)) {
                stmt.setString(1, branchName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        stats.setOrdersProcessedToday(rs.getInt(1));
                    }
                }
            }
            
            // Simulate other metrics (in a real system, these would come from system monitoring)
            stats.setCpuUsage(Math.random() * 100);
            stats.setMemoryUsage(Math.random() * 100);
            stats.setActiveConnections((int) (Math.random() * 50));
            stats.setPendingOrders((int) (Math.random() * 10));
            stats.setResponseTimeMs((long) (Math.random() * 1000));
            
        } catch (SQLException e) {
            logger.error("Failed to update statistics for branch: {}", branchName, e);
        }
    }
    
    private void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();
        lastHeartbeats.forEach((branchName, lastBeat) -> {
            if (currentTime - lastBeat > 10000) { // 10 second timeout
                logger.warn("Branch {} heartbeat timeout - marking as offline", branchName);
                BranchInfo branch = branches.get(branchName);
                if (branch != null) {
                    branch.setActive(false);
                }
            }
        });
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("LoadBalancerService shutdown completed");
    }
}
