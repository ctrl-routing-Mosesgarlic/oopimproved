package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Global Manager Dashboard
 * Handles global management operations: monitor all branches, manage inventory, track sales
 */
public class GlobalManagerDashboardController extends BaseDashboardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalManagerDashboardController.class);
    
    // Branch Management
    @FXML private TableView<StockDTO> branchStockTable;
    @FXML private TableColumn<StockDTO, String> branchNameColumn;
    @FXML private TableColumn<StockDTO, String> drinkNameColumn;
    @FXML private TableColumn<StockDTO, Integer> stockQuantityColumn;
    @FXML private TableColumn<StockDTO, Double> stockValueColumn;
    @FXML private ComboBox<String> branchFilterComboBox;
    @FXML private Button refreshStockButton;
    
    // Sales Overview
    @FXML private TableView<OrderDTO> salesTable;
    @FXML private TableColumn<OrderDTO, Long> orderIdColumn;
    @FXML private TableColumn<OrderDTO, String> orderBranchColumn;
    @FXML private TableColumn<OrderDTO, String> orderCustomerColumn;
    @FXML private TableColumn<OrderDTO, String> orderDateColumn;
    @FXML private TableColumn<OrderDTO, Double> orderAmountColumn;
    @FXML private TableColumn<OrderDTO, String> orderStatusColumn;
    
    // Performance Charts
    @FXML private BarChart<String, Number> branchPerformanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ComboBox<String> chartTypeComboBox;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button updateChartButton;
    
    // Branch Management
    @FXML private ComboBox<String> targetBranchComboBox;
    @FXML private Button sendAlertButton;
    @FXML private TextArea alertMessageArea;
    @FXML private ComboBox<String> alertTypeComboBox;
    
    // Stock Management
    @FXML private ComboBox<String> stockBranchComboBox;
    @FXML private ComboBox<String> stockDrinkComboBox;
    @FXML private TextField stockQuantityField;
    @FXML private Button updateStockButton;
    
    // Data
    private ObservableList<StockDTO> stockData = FXCollections.observableArrayList();
    private ObservableList<OrderDTO> salesData = FXCollections.observableArrayList();
    private ObservableList<String> branchNames = FXCollections.observableArrayList();
    private ObservableList<String> drinkNames = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Global Manager Dashboard");
        
        // Initialize tables
        setupStockTable();
        setupSalesTable();
        
        // Initialize combo boxes
        branchFilterComboBox.getItems().add("All Branches");
        branchNames.addAll("Nakuru", "Mombasa", "Kisumu", "Nairobi");
        branchFilterComboBox.getItems().addAll(branchNames);
        branchFilterComboBox.setValue("All Branches");
        
        targetBranchComboBox.setItems(branchNames);
        stockBranchComboBox.setItems(branchNames);
        
        chartTypeComboBox.getItems().addAll("Sales", "Revenue", "Orders", "Stock Value");
        chartTypeComboBox.setValue("Revenue");
        
        alertTypeComboBox.getItems().addAll("INFO", "WARNING", "STOCK_UPDATE", "PROMOTION");
        alertTypeComboBox.setValue("INFO");
        
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
    
    private void setupStockTable() {
        branchNameColumn.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        drinkNameColumn.setCellValueFactory(new PropertyValueFactory<>("drinkName"));
        stockQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        stockValueColumn.setCellValueFactory(cellData -> {
            double value = cellData.getValue().getQuantity() * cellData.getValue().getUnitPrice();
            return new javafx.beans.property.SimpleDoubleProperty(value).asObject();
        });
        
        branchStockTable.setItems(stockData);
    }
    
    private void setupSalesTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderBranchColumn.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        orderCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        salesTable.setItems(salesData);
    }
    
    private void setupButtonActions() {
        refreshStockButton.setOnAction(event -> loadStockData());
        updateChartButton.setOnAction(event -> updatePerformanceChart());
        sendAlertButton.setOnAction(event -> sendBranchAlert());
        updateStockButton.setOnAction(event -> updateBranchStock());
        logoutButton.setOnAction(event -> handleLogout());
        
        branchFilterComboBox.setOnAction(event -> filterStockData());
    }
    
    @Override
    public void initializeDashboard() {
        // Load initial data
        loadStockData();
        loadSalesData();
        loadDrinks();
        updatePerformanceChart();
    }
    
    private void loadStockData() {
        progressIndicator.setVisible(true);
        
        Task<List<StockDTO>> task = new Task<>() {
            @Override
            protected List<StockDTO> call() throws Exception {
                return stockService.getAllStock();
            }
            
            @Override
            protected void succeeded() {
                List<StockDTO> stocks = getValue();
                stockData.clear();
                stockData.addAll(stocks);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded stock data for all branches");
                
                // Apply filter if needed
                filterStockData();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load stock data", exception);
                showError("Failed to load stock data: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void filterStockData() {
        String selectedBranch = branchFilterComboBox.getValue();
        if (selectedBranch == null || "All Branches".equals(selectedBranch)) {
            // No filtering needed
            return;
        }
        
        // Get all stock data
        Task<List<StockDTO>> task = new Task<>() {
            @Override
            protected List<StockDTO> call() throws Exception {
                return stockService.getAllStock();
            }
            
            @Override
            protected void succeeded() {
                List<StockDTO> allStocks = getValue();
                
                // Filter by selected branch
                List<StockDTO> filteredStocks = allStocks.stream()
                    .filter(stock -> selectedBranch.equals(stock.getBranchName()))
                    .collect(Collectors.toList());
                
                stockData.clear();
                stockData.addAll(filteredStocks);
                
                statusLabel.setText("Filtered stock data for " + selectedBranch);
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to filter stock data", exception);
                showError("Failed to filter stock data: " + exception.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadSalesData() {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getAllOrders();
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                salesData.clear();
                salesData.addAll(orders);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded sales data for all branches");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load sales data", exception);
                showError("Failed to load sales data: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadDrinks() {
        Task<List<DrinkDTO>> task = new Task<>() {
            @Override
            protected List<DrinkDTO> call() throws Exception {
                return drinkService.getAllDrinks();
            }
            
            @Override
            protected void succeeded() {
                List<DrinkDTO> drinks = getValue();
                drinkNames.clear();
                drinks.forEach(drink -> drinkNames.add(drink.getName()));
                
                // Update drink combo box
                stockDrinkComboBox.setItems(drinkNames);
                if (!drinkNames.isEmpty()) {
                    stockDrinkComboBox.setValue(drinkNames.get(0));
                }
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load drinks", exception);
                showError("Failed to load drinks: " + exception.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    private void updatePerformanceChart() {
        String chartType = chartTypeComboBox.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (chartType == null || startDate == null || endDate == null) {
            showError("Please select chart type and date range");
            return;
        }
        
        if (endDate.isBefore(startDate)) {
            showError("End date cannot be before start date");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getAllOrders();
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> allOrders = getValue();
                
                // Filter by date range
                List<OrderDTO> filteredOrders = allOrders.stream()
                    .filter(order -> {
                        try {
                            LocalDate orderDate = LocalDate.parse(
                                order.getOrderDate().split(" ")[0],
                                DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            );
                            return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
                
                // Clear previous data
                branchPerformanceChart.getData().clear();
                
                // Create series for chart
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(chartType + " by Branch");
                
                // Calculate metrics by branch
                for (String branch : branchNames) {
                    double value = 0;
                    
                    switch (chartType) {
                        case "Sales":
                            // Count of sales
                            value = filteredOrders.stream()
                                .filter(order -> branch.equals(order.getBranchName()))
                                .count();
                            break;
                        case "Revenue":
                            // Sum of order amounts
                            value = filteredOrders.stream()
                                .filter(order -> branch.equals(order.getBranchName()))
                                .mapToDouble(OrderDTO::getTotalAmount)
                                .sum();
                            break;
                        case "Orders":
                            // Count of orders
                            value = filteredOrders.stream()
                                .filter(order -> branch.equals(order.getBranchName()))
                                .count();
                            break;
                        case "Stock Value":
                            // Calculate from stock data
                            value = stockData.stream()
                                .filter(stock -> branch.equals(stock.getBranchName()))
                                .mapToDouble(stock -> stock.getQuantity() * stock.getUnitPrice())
                                .sum();
                            break;
                    }
                    
                    series.getData().add(new XYChart.Data<>(branch, value));
                }
                
                // Add series to chart
                branchPerformanceChart.getData().add(series);
                
                // Update axis labels
                xAxis.setLabel("Branch");
                switch (chartType) {
                    case "Sales":
                    case "Orders":
                        yAxis.setLabel("Count");
                        break;
                    case "Revenue":
                    case "Stock Value":
                        yAxis.setLabel("Amount (KES)");
                        break;
                }
                
                progressIndicator.setVisible(false);
                statusLabel.setText("Chart updated: " + chartType + " by branch");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to update chart", exception);
                showError("Failed to update chart: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void sendBranchAlert() {
        String branch = targetBranchComboBox.getValue();
        String message = alertMessageArea.getText().trim();
        String alertType = alertTypeComboBox.getValue();
        
        if (branch == null || message.isEmpty() || alertType == null) {
            showError("Please select branch, alert type, and enter message");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Get branch users (branch manager and staff)
                List<UserDTO> users = authService.getAllUsers(currentUser);
                List<UserDTO> branchUsers = users.stream()
                    .filter(user -> branch.equals(user.getBranchName()))
                    .collect(Collectors.toList());
                
                // Create notification
                NotificationDTO notification = new NotificationDTO(
                    "HQ Alert: " + alertType,
                    message,
                    NotificationDTO.NotificationType.valueOf(alertType)
                );
                
                // Send to all branch users
                for (UserDTO user : branchUsers) {
                    notificationService.sendNotification(user.getId(), notification);
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Alert sent to " + branch + " branch");
                alertMessageArea.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to send alert", exception);
                showError("Failed to send alert: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void updateBranchStock() {
        String branch = stockBranchComboBox.getValue();
        String drink = stockDrinkComboBox.getValue();
        String quantityText = stockQuantityField.getText().trim();
        
        if (branch == null || drink == null || quantityText.isEmpty()) {
            showError("Please select branch, drink, and enter quantity");
            return;
        }
        
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity < 0) {
                showError("Quantity cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid quantity format");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Get drink ID
                List<DrinkDTO> drinks = drinkService.getAllDrinks();
                DrinkDTO selectedDrink = drinks.stream()
                    .filter(d -> drink.equals(d.getName()))
                    .findFirst()
                    .orElse(null);
                
                if (selectedDrink == null) {
                    throw new Exception("Drink not found: " + drink);
                }
                
                // Update stock
                stockService.updateStock(selectedDrink.getId(), branch, quantity);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Stock updated for " + drink + " at " + branch + " branch");
                stockQuantityField.clear();
                
                // Refresh stock data
                loadStockData();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to update stock", exception);
                showError("Failed to update stock: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    @Override
    protected void handleNotification(NotificationDTO notification) {
        super.handleNotification(notification);
        
        // Additional handling specific to global manager
        if (notification.getType() == NotificationDTO.NotificationType.STOCK_LOW || 
            notification.getType() == NotificationDTO.NotificationType.STOCK_OUT) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ Stock alert: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: orange;");
                
                // Show alert for critical stock issues
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Stock Alert");
                alert.setHeaderText("Stock Level Warning");
                alert.setContentText(notification.getMessage());
                alert.show();
                
                // Refresh stock data
                loadStockData();
            });
        }
    }
    
}
