package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.CustomerReportDTO;
import com.drinks.rmi.dto.DrinkPopularityReportDTO;
import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.StockDTO;
// import com.drinks.rmi.interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

/**
 * Controller for Auditor Dashboard
 * Handles auditing operations: financial reports, sales analysis, system logs
 */
public class AuditorDashboardController extends BaseDashboardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditorDashboardController.class);
    
    // Reports Section
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> branchComboBox;
    @FXML private Button generateReportButton;
    @FXML private TextArea reportTextArea;
    
    // Financial Data
    @FXML private TableView<OrderDTO> financialTable;
    @FXML private TableColumn<OrderDTO, String> financialBranchColumn;
    @FXML private TableColumn<OrderDTO, String> financialPeriodColumn;
    @FXML private TableColumn<OrderDTO, Double> financialRevenueColumn;
    @FXML private TableColumn<OrderDTO, Double> financialCostColumn;
    @FXML private TableColumn<OrderDTO, Double> financialProfitColumn;
    @FXML private TableColumn<OrderDTO, Double> financialGrowthColumn;
    
    // Charts
    @FXML private BarChart<String, Number> salesChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    
    // System Logs
    @FXML private TableView<String> logsTable;
    @FXML private TableColumn<String, String> logEntryColumn;
    @FXML private Button refreshLogsButton;
    
    // Drink Popularity Report
    @FXML
    private TableView<DrinkPopularityReportDTO.DrinkPopularityItem> drinkPopularityTable;
    @FXML
    private Label drinkPopularityLabel;
    @FXML
    private DatePicker drinkPopularityStartDatePicker;
    @FXML
    private DatePicker drinkPopularityEndDatePicker;
    @FXML
    private Button generateDrinkPopularityReportButton;
    
    // Customer Report
    @FXML
    private TableView<CustomerReportDTO.CustomerOrderSummary> customerTable;
    @FXML
    private Label customerReportLabel;
    @FXML
    private DatePicker customerReportStartDatePicker;
    @FXML
    private DatePicker customerReportEndDatePicker;
    @FXML
    private Button generateCustomerReportButton;
    
    // Data
    private ObservableList<OrderDTO> financialData = FXCollections.observableArrayList();
    private ObservableList<String> logsData = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Auditor Dashboard");
        
        // Initialize tables
        setupFinancialTable();
        setupLogsTable();
        
        // Initialize combo boxes
        reportTypeComboBox.getItems().addAll(
            "Sales Report", 
            "Revenue Report", 
            "Drink Popularity Report", 
            "Customer Activity Report",
            "Inventory Audit Report"
        );
        
        branchComboBox.getItems().addAll(
            "All Branches", 
            "Nakuru", 
            "Mombasa", 
            "Kisumu", 
            "Nairobi"
        );
        branchComboBox.setValue("All Branches");
        
        // Initialize date pickers
        LocalDate now = LocalDate.now();
        endDatePicker.setValue(now);
        startDatePicker.setValue(now.minusMonths(1));
        
        // Set up button actions
        setupButtonActions();
        
        // Initialize chart
        xAxis.setLabel("Branch");
        yAxis.setLabel("Revenue (KES)");
        
        progressIndicator.setVisible(false);
    }
    
    private void setupFinancialTable() {
        financialBranchColumn.setCellValueFactory(new PropertyValueFactory<>("branch"));
        financialPeriodColumn.setCellValueFactory(new PropertyValueFactory<>("period"));
        financialRevenueColumn.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        financialCostColumn.setCellValueFactory(new PropertyValueFactory<>("cost"));
        financialProfitColumn.setCellValueFactory(new PropertyValueFactory<>("profit"));
        financialGrowthColumn.setCellValueFactory(new PropertyValueFactory<>("growth"));
        
        financialTable.setItems(financialData);
    }
    
    private void setupLogsTable() {
        logEntryColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue()));
        logsTable.setItems(logsData);
    }
    
    private void setupButtonActions() {
        generateReportButton.setOnAction(event -> generateReport());
        refreshLogsButton.setOnAction(event -> loadSystemLogs());
        logoutButton.setOnAction(event -> handleLogout());
        generateDrinkPopularityReportButton.setOnAction(event -> generateDrinkPopularityReport());
        generateCustomerReportButton.setOnAction(event -> generateCustomerReport());
    }
    
    @Override
    public void initializeDashboard() {
        // Load initial data
        loadFinancialData();
        loadSystemLogs();
        updateSalesChart();
    }
    
    private void loadFinancialData() {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                // Get all orders for auditing
                return orderService.getAllOrders();
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                financialData.clear();
                financialData.addAll(orders);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + orders.size() + " financial transactions");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load financial data", exception);
                showError("Failed to load financial data: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadSystemLogs() {
        progressIndicator.setVisible(true);
        
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                // Simulated system logs - in a real implementation, this would come from a log service
                return List.of(
                    "INFO [2023-07-15 08:30:22] User admin logged in",
                    "INFO [2023-07-15 09:15:43] New drink 'Tropical Punch' added by admin",
                    "WARNING [2023-07-15 10:22:15] Low stock alert for 'Orange Juice' at Nairobi branch",
                    "INFO [2023-07-15 11:05:33] Customer order #1245 placed at Mombasa branch",
                    "ERROR [2023-07-15 12:30:18] Failed database connection attempt from Kisumu branch",
                    "INFO [2023-07-15 13:45:27] Stock replenished for 'Mango Smoothie' at Nakuru branch",
                    "INFO [2023-07-15 14:20:55] User branch_manager_nairobi generated sales report",
                    "WARNING [2023-07-15 15:10:02] Multiple failed login attempts for user customer_support",
                    "INFO [2023-07-15 16:35:40] System backup completed successfully",
                    "INFO [2023-07-15 17:50:12] User auditor exported financial report"
                );
            }
            
            @Override
            protected void succeeded() {
                List<String> logs = getValue();
                logsData.clear();
                logsData.addAll(logs);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + logs.size() + " system log entries");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load system logs", exception);
                showError("Failed to load system logs: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void updateSalesChart() {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getAllOrders();
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                
                // Clear previous data
                salesChart.getData().clear();
                
                // Create series for chart
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Revenue by Branch");
                
                // Group by branch and sum revenue
                double nairobiTotal = orders.stream()
                    .filter(o -> "Nairobi".equals(o.getBranchName()))
                    .mapToDouble(OrderDTO::getTotalAmount)
                    .sum();
                
                double mombasaTotal = orders.stream()
                    .filter(o -> "Mombasa".equals(o.getBranchName()))
                    .mapToDouble(OrderDTO::getTotalAmount)
                    .sum();
                
                double kisumuTotal = orders.stream()
                    .filter(o -> "Kisumu".equals(o.getBranchName()))
                    .mapToDouble(OrderDTO::getTotalAmount)
                    .sum();
                
                double nakuruTotal = orders.stream()
                    .filter(o -> "Nakuru".equals(o.getBranchName()))
                    .mapToDouble(OrderDTO::getTotalAmount)
                    .sum();
                
                // Add data to series
                series.getData().add(new XYChart.Data<>("Nairobi", nairobiTotal));
                series.getData().add(new XYChart.Data<>("Mombasa", mombasaTotal));
                series.getData().add(new XYChart.Data<>("Kisumu", kisumuTotal));
                series.getData().add(new XYChart.Data<>("Nakuru", nakuruTotal));
                
                // Add series to chart
                salesChart.getData().add(series);
                
                progressIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to update sales chart", exception);
                showError("Failed to update sales chart: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void generateReport() {
        String reportType = reportTypeComboBox.getValue();
        if (reportType == null) {
            showError("Please select a report type");
            return;
        }
        
        String branch = branchComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showError("Please select start and end dates");
            return;
        }
        
        if (endDate.isBefore(startDate)) {
            showError("End date cannot be before start date");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String formattedStartDate = startDate.format(formatter);
                String formattedEndDate = endDate.format(formatter);
                
                StringBuilder report = new StringBuilder();
                report.append("=== ").append(reportType).append(" ===\n");
                report.append("Period: ").append(formattedStartDate).append(" to ").append(formattedEndDate).append("\n");
                report.append("Branch: ").append(branch).append("\n\n");
                
                switch (reportType) {
                    case "Sales Report":
                        // Get sales data from report service
                        List<OrderDTO> orders = orderService.getAllOrders();
                        
                        // Filter by date and branch
                        List<OrderDTO> filteredOrders = orders.stream()
                            .filter(o -> {
                                LocalDate orderDate = LocalDate.parse(o.getOrderDate().split(" ")[0]);
                                return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                            })
                            .filter(o -> "All Branches".equals(branch) || branch.equals(o.getBranchName()))
                            .toList();
                        
                        // Generate report
                        report.append("Total Orders: ").append(filteredOrders.size()).append("\n");
                        report.append("Total Revenue: KES ").append(String.format("%.2f", 
                            filteredOrders.stream().mapToDouble(OrderDTO::getTotalAmount).sum())).append("\n\n");
                        
                        report.append("Order Breakdown:\n");
                        filteredOrders.forEach(o -> 
                            report.append("- Order #").append(o.getId())
                                  .append(" (").append(o.getOrderDate()).append("): KES ")
                                  .append(String.format("%.2f", o.getTotalAmount()))
                                  .append(" - ").append(o.getStatus())
                                  .append("\n")
                        );
                        break;
                        
                    case "Revenue Report":
                        // Similar to sales report but with more financial details
                        report.append("Revenue analysis will be displayed here\n");
                        report.append("Including profit margins, costs, and revenue trends\n");
                        break;
                        
                    case "Drink Popularity Report":
                        // Get popularity data
                        DrinkPopularityReportDTO reportDTO = reportService.getDrinkPopularityReport(
                            LocalDate.now().minusMonths(1),
                            LocalDate.now()
                        );
                        report.append("Top Selling Drinks:\n");
                        reportDTO.getTopDrinks().forEach(drink -> 
                            report.append("- ").append(drink.getDrinkName())
                                  .append(": ").append(drink.getTotalOrders()).append(" orders, KES ")
                                  .append(String.format("%.2f", drink.getTotalRevenue())).append(" revenue\n")
                        );
                        break;
                        
                    case "Customer Activity Report":
                        // Get customer report
                        CustomerReportDTO customerReportDTO = reportService.getCustomerReport(
                            LocalDate.now().minusMonths(1),
                            LocalDate.now()
                        );
                        report.append("Customer Activity:\n");
                        customerReportDTO.getTopCustomers().forEach(customer -> 
                            report.append("- ").append(customer.getCustomerName())
                                  .append(": ").append(customer.getTotalOrders()).append(" orders, KES ")
                                  .append(String.format("%.2f", customer.getTotalSpent())).append(" spent\n")
                        );
                        break;
                        
                    case "Inventory Audit Report":
                        // Get inventory data
                        List<StockDTO> stocks = stockService.getAllStock();
                        
                        // Filter by branch
                        List<StockDTO> filteredStocks = stocks.stream()
                            .filter(s -> "All Branches".equals(branch) || branch.equals(s.getBranchName()))
                            .toList();
                        
                        report.append("Inventory Status:\n");
                        filteredStocks.forEach(stock -> 
                            report.append("- ").append(stock.getDrinkName())
                                  .append(" at ").append(stock.getBranchName())
                                  .append(": ").append(stock.getQuantity()).append(" units available\n")
                        );
                        
                        // Calculate inventory value
                        double totalValue = filteredStocks.stream()
                            .mapToDouble(s -> s.getQuantity() * s.getUnitPrice())
                            .sum();
                        
                        report.append("\nTotal Inventory Value: KES ").append(String.format("%.2f", totalValue));
                        break;
                }
                
                return report.toString();
            }
            
            @Override
            protected void succeeded() {
                String reportContent = getValue();
                reportTextArea.setText(reportContent);
                progressIndicator.setVisible(false);
                statusLabel.setText("Report generated successfully");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to generate report", exception);
                showError("Failed to generate report: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void generateDrinkPopularityReport() {
        LocalDate startDate = drinkPopularityStartDatePicker.getValue();
        LocalDate endDate = drinkPopularityEndDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert("Please select both start and end dates");
            return;
        }
        
        try {
            DrinkPopularityReportDTO report = reportService.getDrinkPopularityReport(startDate, endDate);
            populateDrinkPopularityTable(report);
        } catch (RemoteException e) {
            showAlert("Error generating report: " + e.getMessage());
        }
    }
    
    private void generateCustomerReport() {
        LocalDate startDate = customerReportStartDatePicker.getValue();
        LocalDate endDate = customerReportEndDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            showAlert("Please select both start and end dates");
            return;
        }
        
        try {
            CustomerReportDTO report = reportService.getCustomerReport(startDate, endDate);
            populateCustomerReportTable(report);
        } catch (RemoteException e) {
            showAlert("Error generating report: " + e.getMessage());
        }
    }
    
    private void populateDrinkPopularityTable(DrinkPopularityReportDTO report) {
        // Clear previous data
        drinkPopularityTable.getItems().clear();
        
        // Add data to table
        report.getTopDrinks().forEach(drink -> 
            drinkPopularityTable.getItems().add(drink)
        );
    }
    
    private void populateCustomerReportTable(CustomerReportDTO report) {
        // Clear previous data
        customerTable.getItems().clear();
        
        // Add data to table
        report.getTopCustomers().forEach(customer -> 
            customerTable.getItems().add(customer)
        );
    }
    
    @Override
    protected void handleNotification(NotificationDTO notification) {
        super.handleNotification(notification);
        
        // Additional handling specific to auditor
        if (notification.getType() == NotificationDTO.NotificationType.SYSTEM_ALERT) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ System alert: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                
                // Add to logs
                String logEntry = "ALERT [" + 
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + 
                    "] " + notification.getMessage();
                logsData.add(0, logEntry);
                
                // Show alert for critical system issues
                if (notification.getMessage().contains("critical") || 
                    notification.getMessage().contains("security")) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("System Alert");
                    alert.setHeaderText("Critical System Alert");
                    alert.setContentText(notification.getMessage());
                    alert.show();
                }
            });
        }
    }
    
    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
