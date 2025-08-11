package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.OrderItemDTO;
import com.drinks.rmi.dto.UserDTO;
// import com.drinks.rmi.interfaces.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;

/**
 * Controller for Customer Support Dashboard
 * Handles customer support operations: manage customer accounts, view orders, handle customer issues
 */
public class CustomerSupportDashboardController extends BaseDashboardController implements DashboardController, Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerSupportDashboardController.class);
    
    // Customer Management
    @FXML private TableView<UserDTO> customersTable;
    @FXML private TableColumn<UserDTO, Long> customerIdColumn;
    @FXML private TableColumn<UserDTO, String> customerNameColumn;
    @FXML private TableColumn<UserDTO, String> customerUsernameColumn;
    
    // Order Management
    @FXML private TableView<OrderDTO> ordersTable;
    @FXML private TableColumn<OrderDTO, Long> orderIdColumn;
    @FXML private TableColumn<OrderDTO, String> orderCustomerColumn;
    @FXML private TableColumn<OrderDTO, String> orderDateColumn;
    @FXML private TableColumn<OrderDTO, String> orderStatusColumn;
    @FXML private TableColumn<OrderDTO, Double> orderTotalColumn;
    
    // Customer Support Tools
    @FXML private TextArea customerNotesArea;
    @FXML private TextField customerSearchField;
    @FXML private Button searchButton;
    @FXML private Button sendNotificationButton;
    @FXML private ComboBox<String> notificationTypeComboBox;
    @FXML private TextArea notificationMessageArea;
    
    // Order Details
    @FXML private TableView<OrderItemDTO> orderItemsTable;
    @FXML private TableColumn<OrderItemDTO, String> itemDrinkColumn;
    @FXML private TableColumn<OrderItemDTO, Integer> itemQuantityColumn;
    @FXML private TableColumn<OrderItemDTO, Double> itemPriceColumn;
    @FXML private TableColumn<OrderItemDTO, Double> itemTotalColumn;
    
    // Data
    private ObservableList<UserDTO> customersData = FXCollections.observableArrayList();
    private ObservableList<OrderDTO> ordersData = FXCollections.observableArrayList();
    private ObservableList<OrderItemDTO> orderItemsData = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Customer Support Dashboard");
        
        // Initialize tables
        setupCustomersTable();
        setupOrdersTable();
        setupOrderItemsTable();
        
        // Initialize notification types
        notificationTypeComboBox.getItems().addAll("INFO", "WARNING", "ORDER_UPDATE", "PROMOTION");
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
    }
    
    private void setupCustomersTable() {
        customerIdColumn.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerUsernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        
        customersTable.setItems(customersData);
        
        // Add selection listener
        customersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadCustomerOrders(newSelection.getCustomerId());
            }
        });
    }
    
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        ordersTable.setItems(ordersData);
        
        // Add selection listener
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                loadOrderItems(newSelection.getId());
            }
        });
    }
    
    private void setupOrderItemsTable() {
        itemDrinkColumn.setCellValueFactory(new PropertyValueFactory<>("drinkName"));
        itemQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        
        orderItemsTable.setItems(orderItemsData);
    }
    
    private void setupButtonActions() {
        searchButton.setOnAction(event -> searchCustomers());
        sendNotificationButton.setOnAction(event -> sendCustomerNotification());
        logoutButton.setOnAction(event -> handleLogout());
    }
    
    @Override
    public void initializeDashboard() {
        // Load all customers
        loadCustomers();
    }
    
    private void loadCustomers() {
        progressIndicator.setVisible(true);
        
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                return authService.getAllUsers(currentUser);
            }
            
            @Override
            protected void succeeded() {
                List<UserDTO> users = getValue();
                // Filter only customer users
                List<UserDTO> customers = users.stream()
                    .filter(user -> "customer".equals(user.getRole()))
                    .toList();
                
                customersData.clear();
                customersData.addAll(customers);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + customers.size() + " customers");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load customers", exception);
                showError("Failed to load customers: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadCustomerOrders(Long customerId) {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getOrdersByCustomer(currentUser, customerId);
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                ordersData.clear();
                ordersData.addAll(orders);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + orders.size() + " orders for customer");
                
                // Clear order items when loading new customer orders
                orderItemsData.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load customer orders", exception);
                showError("Failed to load customer orders: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadOrderItems(Long orderId) {
        progressIndicator.setVisible(true);
        
        Task<List<OrderItemDTO>> task = new Task<>() {
            @Override
            protected List<OrderItemDTO> call() throws Exception {
                return orderService.getOrderItems(orderId);
            }
            
            @Override
            protected void succeeded() {
                List<OrderItemDTO> items = getValue();
                orderItemsData.clear();
                orderItemsData.addAll(items);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + items.size() + " items for order #" + orderId);
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load order items", exception);
                showError("Failed to load order items: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void searchCustomers() {
        String searchTerm = customerSearchField.getText().trim();
        if (searchTerm.isEmpty()) {
            loadCustomers();
            return;
        }
        
        // Filter customers by name or username
        customersData.clear();
        
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                List<UserDTO> allUsers = authService.getAllUsers(currentUser);
                return allUsers.stream()
                    .filter(user -> "customer".equals(user.getRole()) &&
                                  (user.getUsername().toLowerCase().contains(searchTerm.toLowerCase()) ||
                                   (user.getCustomerName() != null && 
                                    user.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))))
                    .toList();
            }
            
            @Override
            protected void succeeded() {
                List<UserDTO> filteredCustomers = getValue();
                customersData.addAll(filteredCustomers);
                statusLabel.setText("Found " + filteredCustomers.size() + " matching customers");
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to search customers", exception);
                showError("Failed to search customers: " + exception.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    private void sendCustomerNotification() {
        UserDTO selectedCustomer = customersTable.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null) {
            showError("Please select a customer to send notification");
            return;
        }
        
        String message = notificationMessageArea.getText().trim();
        if (message.isEmpty()) {
            showError("Please enter a notification message");
            return;
        }
        
        String type = notificationTypeComboBox.getValue();
        if (type == null) {
            showError("Please select a notification type");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                NotificationDTO notification = new NotificationDTO(
                    "Customer Support Message",
                    message,
                    NotificationDTO.NotificationType.valueOf(type)
                );
                
                notificationService.sendNotification(selectedCustomer.getId(), notification);
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Notification sent to " + selectedCustomer.getCustomerName());
                notificationMessageArea.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to send notification", exception);
                showError("Failed to send notification: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    @Override
    protected void handleNotification(NotificationDTO notification) {
        super.handleNotification(notification);
        
        // Additional handling specific to customer support
        if (notification.getType() == NotificationDTO.NotificationType.CUSTOMER_ISSUE) {
            Platform.runLater(() -> {
                statusLabel.setText("⚠️ Customer issue reported: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                
                // Show alert for urgent customer issues
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Customer Issue");
                alert.setHeaderText("New Customer Issue Reported");
                alert.setContentText(notification.getMessage());
                alert.show();
            });
        }
    }
}
