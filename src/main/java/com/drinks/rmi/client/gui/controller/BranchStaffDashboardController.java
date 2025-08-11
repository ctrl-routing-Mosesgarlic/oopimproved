package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.OrderItemDTO;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
// import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Branch Staff Dashboard
 * Handles branch staff operations: order processing, customer service, inventory checking
 */
public class BranchStaffDashboardController extends BaseDashboardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(BranchStaffDashboardController.class);
    
    // Branch Info
    @FXML private Label branchNameLabel;
    
    // Order Processing
    @FXML private TableView<OrderDTO> pendingOrdersTable;
    @FXML private TableColumn<OrderDTO, Long> orderIdColumn;
    @FXML private TableColumn<OrderDTO, String> customerNameColumn;
    @FXML private TableColumn<OrderDTO, String> orderDateColumn;
    @FXML private TableColumn<OrderDTO, Double> totalAmountColumn;
    @FXML private TableColumn<OrderDTO, String> statusColumn;
    @FXML private Button processOrderButton;
    @FXML private Button completeOrderButton;
    @FXML private Button cancelOrderButton;
    
    // Order Details
    @FXML private TableView<OrderItemDTO> orderItemsTable;
    @FXML private TableColumn<OrderItemDTO, String> itemDrinkColumn;
    @FXML private TableColumn<OrderItemDTO, Integer> itemQuantityColumn;
    @FXML private TableColumn<OrderItemDTO, Double> itemPriceColumn;
    @FXML private TableColumn<OrderItemDTO, Double> itemTotalColumn;
    
    // Inventory Checking
    @FXML private TableView<StockDTO> stockTable;
    @FXML private TableColumn<StockDTO, String> drinkNameColumn;
    @FXML private TableColumn<StockDTO, Integer> quantityColumn;
    @FXML private TableColumn<StockDTO, Double> unitPriceColumn;
    @FXML private Button refreshStockButton;
    @FXML private TextField searchDrinkField;
    @FXML private Button searchButton;
    
    // Customer Service
    @FXML private TextArea customerNoteArea;
    @FXML private TextField customerIdField;
    @FXML private Button sendCustomerMessageButton;
    
    // Create Order
    @FXML private ComboBox<String> drinkComboBox;
    @FXML private TextField quantityField;
    @FXML private Button addToCartButton;
    @FXML private ComboBox<String> customerComboBox;
    @FXML private TableView<OrderItemDTO> cartTable;
    @FXML private TableColumn<OrderItemDTO, String> cartDrinkColumn;
    @FXML private TableColumn<OrderItemDTO, Integer> cartQuantityColumn;
    @FXML private TableColumn<OrderItemDTO, Double> cartPriceColumn;
    @FXML private TableColumn<OrderItemDTO, Double> cartTotalColumn;
    @FXML private Button removeFromCartButton;
    @FXML private Button createOrderButton;
    @FXML private Label totalPriceLabel;
    
    // Data
    private ObservableList<OrderDTO> pendingOrdersData = FXCollections.observableArrayList();
    private ObservableList<OrderItemDTO> orderItemsData = FXCollections.observableArrayList();
    private ObservableList<StockDTO> stockData = FXCollections.observableArrayList();
    private ObservableList<OrderItemDTO> cartData = FXCollections.observableArrayList();
    private ObservableList<String> drinkNames = FXCollections.observableArrayList();
    private ObservableList<String> customerNames = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Branch Staff Dashboard");
        
        // Initialize tables
        setupPendingOrdersTable();
        setupOrderItemsTable();
        setupStockTable();
        setupCartTable();
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
    }
    
    private void setupPendingOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        pendingOrdersTable.setItems(pendingOrdersData);
        
        // Add selection listener
        pendingOrdersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
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
    
    private void setupStockTable() {
        drinkNameColumn.setCellValueFactory(new PropertyValueFactory<>("drinkName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        
        stockTable.setItems(stockData);
    }
    
    private void setupCartTable() {
        cartDrinkColumn.setCellValueFactory(new PropertyValueFactory<>("drinkName"));
        cartQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        cartPriceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        cartTotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        
        cartTable.setItems(cartData);
    }
    
    private void setupButtonActions() {
        processOrderButton.setOnAction(event -> processOrder());
        completeOrderButton.setOnAction(event -> completeOrder());
        cancelOrderButton.setOnAction(event -> cancelOrder());
        refreshStockButton.setOnAction(event -> loadStockData());
        searchButton.setOnAction(event -> searchDrink());
        sendCustomerMessageButton.setOnAction(event -> sendCustomerMessage());
        addToCartButton.setOnAction(event -> handleAddToCart());
        removeFromCartButton.setOnAction(event -> removeFromCart());
        createOrderButton.setOnAction(event -> createOrder());
        logoutButton.setOnAction(event -> handleLogout());
    }
    
    @Override
    public void initializeDashboard() {
        // Set branch name
        branchNameLabel.setText(currentUser.getBranchName() + " Branch");
        
        // Load initial data
        loadPendingOrders();
        loadStockData();
        loadDrinks();
        loadCustomers();
        
        // Update cart total
        updateCartTotal();
    }
    
    private void loadPendingOrders() {
        progressIndicator.setVisible(true);
        
        Task<List<OrderDTO>> task = new Task<>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                List<OrderDTO> allOrders = orderService.getBranchOrders(currentUser.getBranchId());
                return allOrders.stream()
                    .filter(order -> "PENDING".equals(order.getStatus()) || "PROCESSING".equals(order.getStatus()))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void succeeded() {
                List<OrderDTO> orders = getValue();
                pendingOrdersData.clear();
                pendingOrdersData.addAll(orders);
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + orders.size() + " pending orders");
                
                // Clear order items when loading new orders
                orderItemsData.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load pending orders", exception);
                showError("Failed to load pending orders: " + exception.getMessage());
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
                List<OrderItemDTO> orderItems = orderService.getOrderItems(orderId);
                return orderItems;
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
    
    private void loadCustomers() {
        Task<List<UserDTO>> task = new Task<>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                List<UserDTO> allUsers = authService.getAllUsers(currentUser);
                return allUsers.stream()
                    .filter(user -> "customer".equals(user.getRole()))
                    .collect(Collectors.toList());
            }
            
            @Override
            protected void succeeded() {
                List<UserDTO> customers = getValue();
                customerNames.clear();
                
                // Create mapping of "customerId:customerName"
                customers.forEach(customer -> {
                    if (customer.getCustomerName() != null) {
                        customerNames.add(customer.getCustomerId() + ":" + customer.getCustomerName());
                    }
                });
                
                // Update customer combo box
                customerComboBox.setItems(customerNames);
                if (!customerNames.isEmpty()) {
                    customerComboBox.setValue(customerNames.get(0));
                }
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load customers", exception);
                showError("Failed to load customers: " + exception.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    private void processOrder() {
        OrderDTO selectedOrder = pendingOrdersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showError("Please select an order to process");
            return;
        }
        
        if (!"PENDING".equals(selectedOrder.getStatus())) {
            showError("Only PENDING orders can be processed");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Update order status
                orderService.updateOrderStatus(selectedOrder.getId(), "PROCESSING");
                
                // Send notification to customer
                NotificationDTO notification = new NotificationDTO(
                    "Order Status Updated",
                    "Your order #" + selectedOrder.getId() + " is now being processed",
                    NotificationDTO.NotificationType.ORDER_UPDATE
                );
                
                notificationService.sendNotification(selectedOrder.getCustomerId(), notification);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Order #" + selectedOrder.getId() + " is now being processed");
                
                // Refresh orders
                loadPendingOrders();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to process order", exception);
                showError("Failed to process order: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void completeOrder() {
        OrderDTO selectedOrder = pendingOrdersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showError("Please select an order to complete");
            return;
        }
        
        if (!"PROCESSING".equals(selectedOrder.getStatus())) {
            showError("Only PROCESSING orders can be completed");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Update order status
                orderService.updateOrderStatus(selectedOrder.getId(), "COMPLETED");
                
                // Send notification to customer
                NotificationDTO notification = new NotificationDTO(
                    "Order Completed",
                    "Your order #" + selectedOrder.getId() + " has been completed",
                    NotificationDTO.NotificationType.ORDER_CONFIRMED
                );
                
                notificationService.sendNotification(selectedOrder.getCustomerId(), notification);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Order #" + selectedOrder.getId() + " has been completed");
                
                // Refresh orders
                loadPendingOrders();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to complete order", exception);
                showError("Failed to complete order: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void cancelOrder() {
        OrderDTO selectedOrder = pendingOrdersTable.getSelectionModel().getSelectedItem();
        if (selectedOrder == null) {
            showError("Please select an order to cancel");
            return;
        }
        
        if ("COMPLETED".equals(selectedOrder.getStatus()) || "CANCELLED".equals(selectedOrder.getStatus())) {
            showError("Cannot cancel completed or already cancelled orders");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Update order status
                orderService.updateOrderStatus(selectedOrder.getId(), "CANCELLED");
                
                // Send notification to customer
                NotificationDTO notification = new NotificationDTO(
                    "Order Cancelled",
                    "Your order #" + selectedOrder.getId() + " has been cancelled",
                    NotificationDTO.NotificationType.ORDER_UPDATE
                );
                
                notificationService.sendNotification(selectedOrder.getCustomerId(), notification);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Order #" + selectedOrder.getId() + " has been cancelled");
                
                // Refresh orders
                loadPendingOrders();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to cancel order", exception);
                showError("Failed to cancel order: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void searchDrink() {
        String searchTerm = searchDrinkField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            loadStockData();
            return;
        }
        
        // Filter stock by drink name
        List<StockDTO> filteredStock = stockData.stream()
            .filter(stock -> stock.getDrinkName().toLowerCase().contains(searchTerm))
            .collect(Collectors.toList());
        
        stockData.clear();
        stockData.addAll(filteredStock);
        statusLabel.setText("Found " + filteredStock.size() + " matching drinks");
    }
    
    private void sendCustomerMessage() {
        String customerId = customerIdField.getText().trim();
        String message = customerNoteArea.getText().trim();
        
        if (customerId.isEmpty() || message.isEmpty()) {
            showError("Please enter customer ID and message");
            return;
        }
        
        long customerIdLong;
        try {
            customerIdLong = Long.parseLong(customerId);
        } catch (NumberFormatException e) {
            showError("Invalid customer ID format");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Create notification
                NotificationDTO notification = new NotificationDTO(
                    "Message from " + currentUser.getBranchName() + " Branch",
                    message,
                    NotificationDTO.NotificationType.INFO
                );
                
                // Send to customer
                notificationService.sendNotification(customerIdLong, notification);
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressIndicator.setVisible(false);
                statusLabel.setText("Message sent to customer #" + customerId);
                customerNoteArea.clear();
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to send customer message", exception);
                showError("Failed to send customer message: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleAddToCart() {
        DrinkDTO selectedDrink = null;
        try {
            selectedDrink = drinkService.getDrinkByName(drinkComboBox.getValue());
            if (selectedDrink == null) {
                showError("Please select a drink");
                return;
            }
        } catch (RemoteException e) {
            showError("Failed to communicate with server: " + e.getMessage());
            return;
        }
        
        try {
            addToCart(selectedDrink);
            statusLabel.setText("Added " + selectedDrink.getName() + " to cart");
        } catch (Exception e) {
            showError("Error adding to cart: " + e.getMessage());
        }
    }
    
    private void addToCart(DrinkDTO drink) {
        OrderItemDTO item = new OrderItemDTO(
            null, 
            drink.getId(),
            drink.getName(),
            1,
            drink.getPrice(),
            drink.getPrice()
        );
        
        cartData.add(item);
        updateCartTotal();
    }
    
    private void updateCartTotal() {
        BigDecimal total = cartData.stream()
            .map(OrderItemDTO::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        totalPriceLabel.setText(String.format("Total: KES %.2f", total));
    }

    // private void handleQuantityChange(TableColumn.CellEditEvent<OrderItemDTO, Integer> event) {
    //     OrderItemDTO item = event.getRowValue();
    //     int newQuantity = event.getNewValue();
        
    //     if (newQuantity <= 0) {
    //         cartData.remove(item);
    //     } else {
    //         item.setQuantity(newQuantity);
    //         item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)));
    //     }
        
    //     updateCartTotal();
    // }
    
    private void removeFromCart() {
        OrderItemDTO selectedItem = cartTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Please select an item to remove");
            return;
        }
        
        cartData.remove(selectedItem);
        updateCartTotal();
        statusLabel.setText("Removed " + selectedItem.getDrinkName() + " from cart");
    }
    
    private void createOrder() {
        if (cartData.isEmpty()) {
            showError("Cart is empty");
            return;
        }
        
        String customerSelection = customerComboBox.getValue();
        if (customerSelection == null) {
            showError("Please select a customer");
            return;
        }
        
        // Extract customer ID from selection (format: "id:name")
        long customerId;
        try {
            customerId = Long.parseLong(customerSelection.split(":")[0]);
        } catch (Exception e) {
            showError("Invalid customer selection");
            return;
        }
        
        progressIndicator.setVisible(true);
        
        Task<OrderDTO> task = new Task<>() {
            @Override
            protected OrderDTO call() throws Exception {
                // Create order
                OrderDTO newOrder = new OrderDTO();
                newOrder.setCustomerId(customerId);
                newOrder.setBranchId(currentUser.getBranchId());
                newOrder.setOrderTime(LocalDateTime.now());
                newOrder.setStatus("PENDING");
                
                // Calculate total using BigDecimal
                BigDecimal subtotal = cartData.stream()
                    .map(OrderItemDTO::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                newOrder.setTotalAmount(subtotal.doubleValue());
                
                // Convert cart items to list
                List<OrderItemDTO> items = new ArrayList<>(cartData);
                
                try {
                    OrderDTO createdOrder = orderService.createOrder(newOrder, items);
                    return createdOrder;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create order", e);
                }
            }
            
            @Override
            protected void succeeded() {
                OrderDTO createdOrder = getValue();
                progressIndicator.setVisible(false);
                statusLabel.setText("Order #" + createdOrder.getId() + " created successfully");
                
                // Clear cart
                cartData.clear();
                updateCartTotal();
                
                // Refresh orders and stock
                loadPendingOrders();
                loadStockData();
                
                // Send notification to customer
                sendOrderConfirmation(createdOrder.getId(), createdOrder.getCustomerId());
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to create order", exception);
                showError("Failed to create order: " + exception.getMessage());
                progressIndicator.setVisible(false);
            }
        };
        
        new Thread(task).start();
    }
    
    private void sendOrderConfirmation(Long orderId, Long customerId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Create notification
                NotificationDTO notification = new NotificationDTO(
                    "Order Confirmation",
                    "Your order #" + orderId + " has been placed at " + currentUser.getBranchName() + " branch",
                    NotificationDTO.NotificationType.ORDER_CONFIRMED
                );
                
                // Send to customer
                notificationService.sendNotification(customerId, notification);
                
                return null;
            }
            
            @Override
            protected void failed() {
                logger.error("Failed to send order confirmation", getException());
            }
        };
        
        new Thread(task).start();
    }
    
    @Override
    protected void handleNotification(NotificationDTO notification) {
        super.handleNotification(notification);
        
        // Additional handling specific to branch staff
        if (notification.getType() == NotificationDTO.NotificationType.ORDER_UPDATE) {
            Platform.runLater(() -> {
                statusLabel.setText("🛒 Order update: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: blue;");
                
                // Refresh orders
                loadPendingOrders();
            });
        } else if (notification.getType() == NotificationDTO.NotificationType.STOCK_UPDATE) {
            Platform.runLater(() -> {
                statusLabel.setText("📦 Stock update: " + notification.getMessage());
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Refresh stock
                loadStockData();
            });
        }
    }
}
