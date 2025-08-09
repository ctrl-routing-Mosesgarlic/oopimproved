package com.drinks.rmi.server;

import com.drinks.rmi.common.DatabaseConfig;
import com.drinks.rmi.dto.CustomerReportDTO;
import com.drinks.rmi.dto.DrinkPopularityReportDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.OrderItemDTO;
import com.drinks.rmi.dto.SalesReportDTO;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.dto.StockReportDTO;
import com.drinks.rmi.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ReportService for generating various reports
 */
public class ReportServiceImpl extends UnicastRemoteObject implements ReportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
    
    public ReportServiceImpl() throws RemoteException {
        super();
    }
    
    @Override
    public SalesReportDTO generateBranchSalesReport(Long branchId, LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating sales report for branch ID: {} from {} to {}", branchId, startDate, endDate);
        
        String branchName;
        try {
            branchName = getBranchName(branchId);
            if (branchName == null) {
                throw new RemoteException("Branch not found with ID: " + branchId);
            }
        } catch (SQLException e) {
            logger.error("Error getting branch name for ID: {}", branchId, e);
            throw new RemoteException("Failed to generate sales report due to database error", e);
        }
        
        SalesReportDTO report = new SalesReportDTO(branchId, branchName, startDate, endDate);
        
        try {
            // Get total orders and sales
            String totalSql = """
                SELECT COUNT(*) as total_orders, SUM(total_price) as total_sales
                FROM orders
                WHERE branch_id = ? AND order_date BETWEEN ? AND ?
                """;
                
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(totalSql)) {
                
                stmt.setLong(1, branchId);
                stmt.setDate(2, java.sql.Date.valueOf(startDate));
                stmt.setDate(3, java.sql.Date.valueOf(endDate));
                
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    report.setTotalOrders(rs.getInt("total_orders"));
                    report.setTotalSales(BigDecimal.valueOf(rs.getDouble("total_sales")));
                }
            }
        } catch (SQLException e) {
            logger.error("Error generating total sales data for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to generate sales report due to database error", e);
        }
        
        // Get sales by drink
        String drinkSql = """
            SELECT d.name as drink_name, SUM(oi.quantity * d.price) as drink_sales
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            JOIN drinks d ON oi.drink_id = d.id
            WHERE o.branch_id = ? AND DATE(o.order_time) BETWEEN ? AND ?
            GROUP BY d.id, d.name
            ORDER BY drink_sales DESC
            """;
        
        Map<String, BigDecimal> salesByDrink = new HashMap<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(drinkSql)) {
            
            stmt.setLong(1, branchId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                salesByDrink.put(rs.getString("drink_name"), rs.getBigDecimal("drink_sales"));
            }
            
            report.setSalesByDrink(salesByDrink);
            
        } catch (SQLException e) {
            logger.error("Error generating sales by drink data for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to generate sales report due to database error", e);
        }
        
        // Get sales by date
        String dateSql = """
            SELECT DATE(o.order_time) as order_date, SUM(oi.quantity * d.price) as daily_sales
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            JOIN drinks d ON oi.drink_id = d.id
            WHERE o.branch_id = ? AND DATE(o.order_time) BETWEEN ? AND ?
            GROUP BY DATE(o.order_time)
            ORDER BY order_date
            """;
        
        Map<LocalDate, BigDecimal> salesByDate = new HashMap<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(dateSql)) {
            
            stmt.setLong(1, branchId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                salesByDate.put(rs.getDate("order_date").toLocalDate(), rs.getBigDecimal("daily_sales"));
            }
            
            report.setSalesByDate(salesByDate);
            
        } catch (SQLException e) {
            logger.error("Error generating sales by date data for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to generate sales report due to database error", e);
        }
        
        logger.info("Generated sales report for branch: {} with total sales: {}", branchName, report.getTotalSales());
        return report;
    }
    
    @Override
    public Map<Long, SalesReportDTO> generateAllBranchesSalesReport(LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating sales report for all branches from {} to {}", startDate, endDate);
        
        Map<Long, SalesReportDTO> reports = new HashMap<>();
        
        // Get all branch IDs
        String branchSql = "SELECT id FROM branches";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(branchSql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Long branchId = rs.getLong("id");
                SalesReportDTO report = generateBranchSalesReport(branchId, startDate, endDate);
                reports.put(branchId, report);
            }
            
        } catch (SQLException e) {
            logger.error("Error generating all branches sales report", e);
            throw new RemoteException("Failed to generate all branches sales report due to database error", e);
        }
        
        logger.info("Generated sales reports for {} branches", reports.size());
        return reports;
    }
    
    @Override
    public StockReportDTO generateBranchStockReport(Long branchId) throws RemoteException {
        logger.info("Generating stock report for branch ID: {}", branchId);
        
        String branchName;
        try {
            branchName = getBranchName(branchId);
            if (branchName == null) {
                throw new RemoteException("Branch not found with ID: " + branchId);
            }
        } catch (SQLException e) {
            logger.error("Error getting branch name for ID: {}", branchId, e);
            throw new RemoteException("Failed to generate stock report due to database error", e);
        }
        
        StockReportDTO report = new StockReportDTO(branchId, branchName);
        
        // Get all stock items for the branch
        String stockSql = """
            SELECT s.id, s.branch_id, b.name as branch_name, s.drink_id, d.name as drink_name, 
                   s.quantity, COALESCE(s.threshold, 10) as threshold
            FROM stocks s
            JOIN branches b ON s.branch_id = b.id
            JOIN drinks d ON s.drink_id = d.id
            WHERE s.branch_id = ?
            ORDER BY d.name
            """;
        
        List<StockDTO> stockItems = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(stockSql)) {
            
            stmt.setLong(1, branchId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                StockDTO stock = new StockDTO(
                    rs.getLong("id"),
                    rs.getLong("branch_id"),
                    rs.getString("branch_name"),
                    rs.getLong("drink_id"),
                    rs.getString("drink_name"),
                    rs.getInt("quantity"),
                    rs.getInt("threshold")
                );
                stockItems.add(stock);
            }
            
            report.setStockItems(stockItems);
            
        } catch (SQLException e) {
            logger.error("Error generating stock report for branch ID: {}", branchId, e);
            throw new RemoteException("Failed to generate stock report due to database error", e);
        }
        
        logger.info("Generated stock report for branch: {} with {} items", branchName, stockItems.size());
        return report;
    }
    
    @Override
    public Map<Long, StockReportDTO> generateAllBranchesStockReport() throws RemoteException {
        logger.info("Generating stock report for all branches");
        
        Map<Long, StockReportDTO> reports = new HashMap<>();
        
        // Get all branch IDs
        String branchSql = "SELECT id FROM branches";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(branchSql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Long branchId = rs.getLong("id");
                StockReportDTO report = generateBranchStockReport(branchId);
                reports.put(branchId, report);
            }
            
        } catch (SQLException e) {
            logger.error("Error generating all branches stock report", e);
            throw new RemoteException("Failed to generate all branches stock report due to database error", e);
        }
        
        logger.info("Generated stock reports for {} branches", reports.size());
        return reports;
    }
    
    @Override
    public CustomerReportDTO generateCustomerReport(Long customerId, LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating customer report for customer ID: {} from {} to {}", customerId, startDate, endDate);
        
        // Get customer details
        String customerSql = "SELECT id, name, email, phone FROM customers WHERE id = ?";
        CustomerReportDTO report = null;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(customerSql)) {
            
            stmt.setLong(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                report = new CustomerReportDTO(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    startDate,
                    endDate
                );
            } else {
                throw new RemoteException("Customer not found with ID: " + customerId);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting customer details for ID: {}", customerId, e);
            throw new RemoteException("Failed to generate customer report due to database error", e);
        }
        
        // Get customer orders
        String orderSql = """
            SELECT o.id, o.customer_id, c.name as customer_name, o.branch_id, b.name as branch_name, o.order_time
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN branches b ON o.branch_id = b.id
            WHERE o.customer_id = ? AND DATE(o.order_time) BETWEEN ? AND ?
            ORDER BY o.order_time DESC
            """;
        
        List<OrderDTO> orders = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(orderSql)) {
            
            stmt.setLong(1, customerId);
            stmt.setDate(2, Date.valueOf(startDate));
            stmt.setDate(3, Date.valueOf(endDate));
            
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
                
                // Get order items and calculate total
                List<OrderItemDTO> items = getOrderItems(order.getId());
                order.setItems(items);
                
                double totalAmount = items.stream()
                    .mapToDouble(item -> item.getSubtotal().doubleValue())
                    .sum();
                order.setTotalAmount(totalAmount);
                
                orders.add(order);
            }
            
            report.setOrders(orders);
            
        } catch (SQLException e) {
            logger.error("Error getting customer orders for ID: {}", customerId, e);
            throw new RemoteException("Failed to generate customer report due to database error", e);
        }
        
        logger.info("Generated customer report for: {} with {} orders", report.getCustomerName(), orders.size());
        return report;
    }
    
    @Override
    public DrinkPopularityReportDTO generateDrinkPopularityReport(LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating drink popularity report from {} to {}", startDate, endDate);
        
        DrinkPopularityReportDTO report = new DrinkPopularityReportDTO(startDate, endDate);
        
        String sql = """
            SELECT d.id as drink_id, d.name as drink_name, 
                   SUM(oi.quantity) as total_quantity_sold,
                   SUM(oi.quantity * d.price) as total_revenue,
                   COUNT(DISTINCT o.id) as order_count
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            JOIN drinks d ON oi.drink_id = d.id
            WHERE DATE(o.order_time) BETWEEN ? AND ?
            GROUP BY d.id, d.name
            ORDER BY total_quantity_sold DESC, total_revenue DESC
            """;
        
        List<DrinkPopularityReportDTO.DrinkPopularityItem> popularityItems = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                DrinkPopularityReportDTO.DrinkPopularityItem item = new DrinkPopularityReportDTO.DrinkPopularityItem(
                    rs.getLong("drink_id"),
                    rs.getString("drink_name"),
                    rs.getInt("total_quantity_sold"),
                    rs.getBigDecimal("total_revenue"),
                    rs.getInt("order_count"),
                    0
                );
                popularityItems.add(item);
            }
            
            report.setDrinkPopularity(popularityItems);
            
        } catch (SQLException e) {
            logger.error("Error generating drink popularity report", e);
            throw new RemoteException("Failed to generate drink popularity report due to database error", e);
        }
        
        logger.info("Generated drink popularity report with {} drinks", popularityItems.size());
        return report;
    }
    
    @Override
    public CustomerReportDTO getCustomerReport(LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating customer report from {} to {}", startDate, endDate);
        
        CustomerReportDTO report = new CustomerReportDTO();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        
        String sql = """
            SELECT c.id, c.name, c.email, c.phone, 
                   COUNT(DISTINCT o.id) as order_count,
                   COALESCE(SUM(oi.quantity * d.price), 0) as total_spent
            FROM customers c
            LEFT JOIN orders o ON c.id = o.customer_id
            LEFT JOIN order_items oi ON o.id = oi.order_id
            LEFT JOIN drinks d ON oi.drink_id = d.id
            WHERE DATE(o.order_time) BETWEEN ? AND ? OR o.id IS NULL
            GROUP BY c.id, c.name, c.email, c.phone
            ORDER BY total_spent DESC
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                CustomerReportDTO.CustomerOrderSummary item = new CustomerReportDTO.CustomerOrderSummary(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("order_count"),
                    rs.getBigDecimal("total_spent")
                );
                report.getTopCustomers().add(item);
            }
        } catch (SQLException e) {
            logger.error("Error generating customer report", e);
            throw new RemoteException("Error generating customer report", e);
        }
        
        return report;
    }
    
    @Override
    public DrinkPopularityReportDTO getDrinkPopularityReport(LocalDate startDate, LocalDate endDate) throws RemoteException {
        logger.info("Generating drink popularity report from {} to {}", startDate, endDate);
        
        DrinkPopularityReportDTO report = new DrinkPopularityReportDTO(startDate, endDate);
        
        String sql = """
            SELECT d.id as drink_id, d.name as drink_name, 
                   SUM(oi.quantity) as total_quantity,
                   SUM(oi.quantity * oi.unit_price) as total_revenue,
                   COUNT(DISTINCT o.id) as order_count
            FROM drinks d
            JOIN order_items oi ON d.id = oi.drink_id
            JOIN orders o ON oi.order_id = o.id
            WHERE DATE(o.order_time) BETWEEN ? AND ?
            GROUP BY d.id, d.name
            ORDER BY total_quantity DESC
            """;
            
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                DrinkPopularityReportDTO.DrinkPopularityItem item = new DrinkPopularityReportDTO.DrinkPopularityItem(
                    rs.getLong("drink_id"),
                    rs.getString("drink_name"),
                    rs.getInt("total_quantity"),
                    rs.getBigDecimal("total_revenue"),
                    rs.getInt("order_count"),
                    0
                );
                report.getDrinkPopularity().add(item);
            }
        } catch (SQLException e) {
            logger.error("Error generating drink popularity report", e);
            throw new RemoteException("Error generating drink popularity report", e);
        }
        
        return report;
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
    
    private List<OrderItemDTO> getOrderItems(Long orderId) throws SQLException {
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
        }
        
        return items;
    }
}
