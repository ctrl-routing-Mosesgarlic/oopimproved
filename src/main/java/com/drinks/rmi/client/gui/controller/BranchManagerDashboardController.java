package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.UserDTO;
// import com.drinks.rmi.interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Branch Manager Dashboard
 * Handles branch management operations: staff management, inventory control, local sales
 */
public class BranchManagerDashboardController extends BaseDashboardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(BranchManagerDashboardController.class);
    
    // Branch Info
    @FXML private Label branchNameLabel;
    
    // Staff Management
    @FXML private TableView<UserDTO> staffTable;
    @FXML private TableColumn<UserDTO, Long> staffIdColumn;
    @FXML private TableColumn<UserDTO, String> staffUsernameColumn;
    @FXML private TableColumn<UserDTO, String> staffRoleColumn;
    @FXML private TextField staffUsernameField;
    @FXML private PasswordField staffPasswordField;
    @FXML private Button addStaffButton;
    @FXML private Button removeStaffButton;
    
    // Inventory Management
    @FXML private TableView<StockDTO> stockTable;
    @FXML private TableColumn<StockDTO, String> drinkNameColumn;
    @FXML private TableColumn<StockDTO, Integer> quantityColumn;
    @FXML private TableColumn<StockDTO, Double> unitPriceColumn;
    @FXML private TableColumn<StockDTO, Double> totalValueColumn;
    @FXML private ComboBox<String> drinkComboBox;
    @FXML private TextField quantityField;
    @FXML private Button updateStockButton;
    @FXML private Button requestStockButton;
    
    // Orders Management
    @FXML private TableView<OrderDTO> ordersTable;
    @FXML private TableColumn<OrderDTO, Long> orderIdColumn;
    @FXML private TableColumn<OrderDTO, String> customerNameColumn;
    @FXML private TableColumn<OrderDTO, String> orderDateColumn;
    @FXML private TableColumn<OrderDTO, Double> totalAmountColumn;
    @FXML private TableColumn<OrderDTO, String> statusColumn;
    @FXML private ComboBox<String> orderStatusComboBox;
    @FXML private Button updateOrderStatusButton;
    
    // Sales Chart
    @FXML private BarChart<String, Number> salesChart;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button updateChartButton;
    
    // Communication
    @FXML private TextArea messageArea;
    @FXML private ComboBox<String> messageTypeComboBox;
    @FXML private Button sendToHQButton;
    
    // Data
    private ObservableList<UserDTO> staffData = FXCollections.observableArrayList();
    private ObservableList<StockDTO> stockData = FXCollections.observableArrayList();
    private ObservableList<OrderDTO> ordersData = FXCollections.observableArrayList();
    private ObservableList<String> drinkNames = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Branch Manager Dashboard");
        
        // Initialize tables
        setupStaffTable();
        setupStockTable();
        setupOrdersTable();
        
        // Initialize combo boxes
        messageTypeComboBox.getItems().addAll("INFO", "STOCK_REQUEST", "ISSUE", "REPORT");
        orderStatusComboBox.getItems().addAll("PENDING", "PROCESSING", "COMPLETED", "CANCELLED");
        
        // Initialize date pickers
        LocalDate now = LocalDate.now();
        endDatePicker.setValue(now);
        startDatePicker.setValue(now.minusMonths(1));
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
    }
    
    private void setupStaffTable() {
        staffIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        staffUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        staffRoleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        staffTable.setItems(staffData);
    }
    
    private void setupStockTable() {
        drinkNameColumn.setCellValueFactory(new PropertyValueFactory<>("drinkName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        totalValueColumn.setCellValueFactory(cellData -> {
            double value = cellData.getValue().getQuantity() * cellData.getValue().getUnitPrice();
            return new javafx.beans.property.SimpleDoubleProperty(value).asObject();
        });
        
        stockTable.setItems(stockData);
    }
    
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        ordersTable.setItems(ordersData);
    }
    
    private void setupButtonActions() {
        addStaffButton.setOnAction(event -> addStaffMember());
        removeStaffButton.setOnAction(event -> removeStaffMember());
        updateStockButton.setOnAction(event -> updateStock());
        requestStockButton.setOnAction(event -> requestStock());
        updateOrderStatusButton.setOnAction(event -> updateOrderStatus());
        updateChartButton.setOnAction(event -> updateSalesChart());
        sendToHQButton.setOnAction(event -> sendMessageToHQ());
        logoutButton.setOnAction(event -> handleLogout());
    }
    
    @Override
    public void initializeDashboard() {
        // Set branch name
        branchNameLabel.setText(currentUser.getBranchName() + " Branch");
        
        // Load initial data
        loadStaffMembers();
        loadStockData();
        loadOrders();
        loadDrinks();
        updateSalesChart();
    }
    
    private void loadStaffMembers() {
        progressIndicator.setVisible(true);
        
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                List<UserDTO> allUsers = authService.getAllUsers(currentUser);
                return allUsers.stream()
                    .filter(user -> currentUser.getBranchId().equals(user.getBranchId()) &&
                                  "branch_staff".equals(user.getRole()))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void succeeded() {
                List<UserDTO> staff = getValue();
                staffData.clear();
                staffData.addAll(staff);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + staff.size() + " staff members");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load staff members", exception);
                showError("Failed to load staff members: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadStockData() {
        progressIndicator.setVisible(true);
        
        Task<List<StockDTO>> task = new Task<>() {
            @Override
            protected List<StockDTO> call() throws Exception {
                return stockService.getBranchStock(currentUser.getBranchId());
            }
            
            @Override
            protected void succeeded() {
                List<StockDTO> stocks = getValue();
                stockData.clear();
                stockData.addAll(stocks);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded stock data for " + currentUser.getBranchName() + " branch");
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
    
    private void loadOrders() {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getBranchOrders(currentUser.getBranchId());
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                ordersData.clear();
                ordersData.addAll(orders);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + orders.size() + " orders for " + currentUser.getBranchName() + " branch");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load orders", exception);
                showError("Failed to load orders: " + exception.getMessage());
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
                drinkComboBox.setItems(drinkNames);
                if (!drinkNames.isEmpty()) {
                    drinkComboBox.setValue(drinkNames.get(0));
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
    
    private void addStaffMember() {
        String username = staffUsernameField.getText().trim();
        String password = staffPasswordField.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<UserDTO> task = new Task<>() {
            @Override
            protected UserDTO call() throws Exception {
                return authService.createStaffUser(username, password, "branch_staff", currentUser.getBranchId());
            }
            
            @Override
            protected void succeeded() {
                UserDTO newStaff = getValue();
                if (newStaff != null) {
                    staffData.add(newStaff);
                    staffUsernameField.clear();
                    staffPasswordField.clear();
                    statusLabel.setText("Added new staff member: " + username);
                } else {
                    showError("Failed to add staff member. Username may already exist.");
                }
                progressIndicator.setVisible(false);
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to add staff member", exception);
                showError("Failed to add staff member: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void removeStaffMember() {
        UserDTO selectedStaff = staffTable.getSelectionModel().getSelectedItem();
        if (selectedStaff == null) {
            showError("Please select a staff member to remove");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Removal");
        confirmation.setHeaderText("Remove Staff Member");
        confirmation.setContentText("Are you sure you want to remove " + selectedStaff.getUsername() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // In a real implementation, you would call a service method to remove the staff member
            // For now, we'll just remove from the local list
            staffData.remove(selectedStaff);
            statusLabel.setText("Removed staff member: " + selectedStaff.getUsername());
            
            // Note: In a real implementation, you would have:
            // authService.removeUser(selectedStaff.getId());
        }
    }
    
    private void updateStock() {
        String drinkName = drinkComboBox.getValue();
        String quantityText = quantityField.getText().trim();
        
        if (drinkName == null || quantityText.isEmpty()) {
            showError("Please select a drink and enter quantity");
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
                    .filter(d -> drinkName.equals(d.getName()))
                    .findFirst()
                    .orElse(null);
                
                if (selectedDrink == null) {
                    throw new Exception("Drink not found: " + drinkName);
                }
                
                // Update stock
                stockService.updateStock(selectedDrink.getId(), currentUser.getBranchName(), quantity);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Stock updated for " + drinkName);
                quantityField.clear();
                
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
    
    private void requestStock() {
        String drinkName = drinkComboBox.getValue();
        String quantityText = quantityField.getText().trim();
        
        if (drinkName == null || quantityText.isEmpty()) {
            showError("Please select a drink and enter quantity");
            return;
        }
        
        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                showError("Quantity must be positive");
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
                // Send notification to HQ
                NotificationDTO notification = new NotificationDTO(
                    "Stock Request from " + currentUser.getBranchName(),
                    "Request for " + quantity + " units of " + drinkName,
                    NotificationDTO.NotificationType.STOCK_REQUEST
                );
                
                // Get global managers
                List<UserDTO> users = authService.getAllUsers(currentUser);
                List<UserDTO> managers = users.stream()
                    .filter(user -> "global_manager".equals(user.getRole()))
                    .collect(Collectors.toList());
                
                // Send to all global managers
                for (UserDTO manager : managers) {
                    notificationService.sendNotification(manager.getId(), notification);
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Stock request sent for " + drinkName);
                quantityField.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to send stock request", exception);
                showError("Failed to send stock request: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void updateOrderStatus() {
        OrderDTO selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
        String newStatus = orderStatusComboBox.getValue();
        
        if (selectedOrder == null || newStatus == null) {
            showError("Please select an order and status");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Update order status
                orderService.updateOrderStatus(selectedOrder.getId(), newStatus);
                
                // Send notification to customer
                NotificationDTO notification = new NotificationDTO(
                    "Order Status Updated",
                    "Your order #" + selectedOrder.getId() + " is now " + newStatus,
                    NotificationDTO.NotificationType.ORDER_UPDATE
                );
                
                notificationService.sendNotification(selectedOrder.getCustomerId(), notification);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Order #" + selectedOrder.getId() + " updated to " + newStatus);
                
                // Refresh orders
                loadOrders();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to update order status", exception);
                showError("Failed to update order status: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void updateSalesChart() {
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
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getBranchOrders(currentUser.getBranchId());
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
                
                // Group by date
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                List<LocalDate> dateRange = new ArrayList<>();
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    dateRange.add(date);
                }
                
                // Clear previous data
                salesChart.getData().clear();
                
                // Create series for chart
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Daily Sales");
                
                // Add data points
                for (LocalDate date : dateRange) {
                    String dateStr = date.format(formatter);
                    double dailySales = filteredOrders.stream()
                        .filter(order -> order.getOrderDate().startsWith(dateStr))
                        .mapToDouble(OrderDTO::getTotalAmount)
                        .sum();
                    
                    series.getData().add(new XYChart.Data<>(dateStr, dailySales));
                }
                
                // Add series to chart
                salesChart.getData().add(series);
                
                progressIndicator.setVisible(false);
                statusLabel.setText("Sales chart updated");
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
    
    private void sendMessageToHQ() {
        String message = messageArea.getText().trim();
        String messageType = messageTypeComboBox.getValue();
        
        if (message.isEmpty() || messageType == null) {
            showError("Please enter a message and select type");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Create notification
                NotificationDTO notification = new NotificationDTO(
                    currentUser.getBranchName() + " Branch: " + messageType,
                    message,
                    NotificationDTO.NotificationType.valueOf(messageType)
                );
                
                // Get HQ users (admin, global manager, auditor, support)
                List<UserDTO> users = authService.getAllUsers(currentUser);
                List<UserDTO> hqUsers = users.stream()
                    .filter(user -> user.getBranchId() == null &&
                                  ("admin".equals(user.getRole()) ||
                                   "global_manager".equals(user.getRole()) ||
                                   "auditor".equals(user.getRole()) ||
                                   "customer_support".equals(user.getRole())))
                    .collect(Collectors.toList());
                
                // Send to all HQ users
                for (UserDTO user : hqUsers) {
                    notificationService.sendNotification(user.getId(), notification);
                }
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Message sent to HQ");
                messageArea.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to send message", exception);
                showError("Failed to send message: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    @Override
    protected void handleNotification(NotificationDTO notification) {
        super.handleNotification(notification);
        
        // Additional handling specific to branch manager
        if (notification.getType() == NotificationDTO.NotificationType.STOCK_UPDATE) {
            Platform.runLater(() -> {
                statusLabel.setText("📦 Stock update: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Refresh stock data
                loadStockData();
            });
        }
    }
}
