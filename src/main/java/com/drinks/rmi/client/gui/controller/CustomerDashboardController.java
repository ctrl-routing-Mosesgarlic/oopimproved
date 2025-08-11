package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.interfaces.*;
import com.drinks.rmi.client.gui.NotificationCallbackImpl;
import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.UserDTO;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
// import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Parent;
import javafx.stage.Modality;
import com.drinks.rmi.interfaces.PaymentService;
// import com.drinks.rmi.client.gui.controller.PaymentDialogController;
import java.util.ResourceBundle;
import javax.rmi.ssl.SslRMIClientSocketFactory;

/**
 * Controller for Customer Dashboard
 * Handles customer operations: browse drinks, place orders, view order history
 */
public class CustomerDashboardController extends BaseDashboardController implements DashboardController, Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerDashboardController.class);
    
    // User Info
    @FXML private Label welcomeLabel;
    @FXML private Label serverInfoLabel;
    @FXML private Button logoutButton;
    
    // Drinks Catalog
    @FXML private TableView<DrinkDTO> drinksTable;
    @FXML private TableColumn<DrinkDTO, String> drinkNameColumn;
    @FXML private TableColumn<DrinkDTO, Double> drinkPriceColumn;
    @FXML private Button refreshDrinksButton;
    
    // Order Placement
    @FXML private ComboBox<String> branchComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addToCartButton;
    @FXML private ListView<String> cartListView;
    @FXML private Label totalLabel;
    @FXML private Button placeOrderButton;
    @FXML private Button clearCartButton;
    
    // Order History
    @FXML private TableView<OrderDTO> ordersTable;
    @FXML private TableColumn<OrderDTO, Long> orderIdColumn;
    @FXML private TableColumn<OrderDTO, String> orderDateColumn;
    @FXML private TableColumn<OrderDTO, String> orderBranchColumn;
    @FXML private TableColumn<OrderDTO, Double> orderTotalColumn;
    @FXML private Button refreshOrdersButton;
    
    // Status
    @FXML private Label statusLabel;
    
    private PaymentService paymentService;
    @FXML private ProgressIndicator progressIndicator;
    
    // Data
    private UserDTO currentUser;
    // private AuthService authService;
    private DrinkService drinkService;
    private OrderService orderService;
    private NotificationService notificationService;
    // private LoadBalancerService loadBalancerService;
    private NotificationCallbackImpl notificationCallback;
    private String serverInfo;
    
    private ObservableList<DrinkDTO> drinksData = FXCollections.observableArrayList();
    private ObservableList<OrderDTO> ordersData = FXCollections.observableArrayList();
    private ObservableList<String> cartData = FXCollections.observableArrayList();
    
    private List<CartItem> cartItems = new ArrayList<>();
    // private double cartTotal = 0.0;
    
    // Helper class for cart items
    private static class CartItem {
        private DrinkDTO drink;
        private int quantity;
        private String branch;
        
        CartItem(DrinkDTO drink, int quantity, String branch) {
            this.drink = drink;
            this.quantity = quantity;
            this.branch = branch;
        }
        
        public DrinkDTO getDrink() {
            return drink;
        }

        public int getQuantity() {
            return quantity;
        }
        
        public BigDecimal getSubtotal() {
            return drink.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
        
        @Override
        public String toString() {
            return String.format("%s x%d @ %s - $%.2f", 
                drink.getName(), quantity, branch, getSubtotal());
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Customer Dashboard");
        
        // Initialize tables
        setupDrinksTable();
        setupOrdersTable();
        
        // Initialize combo boxes and spinners
        branchComboBox.getItems().addAll("Nakuru", "Mombasa", "Kisumu", "Nairobi");
        branchComboBox.setValue("Nakuru");
        
        quantitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        
        // Set up cart
        cartListView.setItems(cartData);
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
        updateCartDisplay();
    }
    
    private void setupDrinksTable() {
        drinkNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        drinkPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        
        drinksTable.setItems(drinksData);
        
        // Selection listener
        drinksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            addToCartButton.setDisable(newSelection == null);
        });
    }
    
    private void setupOrdersTable() {
        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        orderDateColumn.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        orderBranchColumn.setCellValueFactory(new PropertyValueFactory<>("branchName"));
        orderTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        
        ordersTable.setItems(ordersData);
    }
    
    private void setupButtonActions() {
        refreshDrinksButton.setOnAction(e -> loadDrinks());
        addToCartButton.setOnAction(e -> handleAddToCart());
        placeOrderButton.setOnAction(e -> handlePlaceOrder());
        clearCartButton.setOnAction(e -> handleClearCart());
        refreshOrdersButton.setOnAction(e -> loadOrders());
        logoutButton.setOnAction(e -> handleLogout());
    }
    
    @Override
    public void setUserData(UserDTO user, AuthService authService, String serverInfo) {
        this.currentUser = user;
        this.authService = authService;
        this.serverInfo = serverInfo;
        
        // Connect to services
        connectToServices();
        
        // Initialize dashboard
        initializeDashboard();
    }
    
    protected void connectToServices() {
        try {
            Registry registry;
            String servicePrefix;
            
            // Determine server type and connection details based on serverInfo
            if (serverInfo.contains("Headquarters") || serverInfo.contains("HQ")) {
                // HQ server uses SSL on port 1099
                SslRMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
                registry = LocateRegistry.getRegistry("localhost", 1099, socketFactory);
                servicePrefix = "HQ_";
                
                // Connect to HQ services
                drinkService = (DrinkService) registry.lookup(servicePrefix + "DrinkService");
                orderService = (OrderService) registry.lookup(servicePrefix + "OrderService");
                notificationService = (NotificationService) registry.lookup(servicePrefix + "NotificationService");
                loadBalancerService = (LoadBalancerService) registry.lookup(servicePrefix + "LoadBalancerService");
                
                logger.info("Connected to HQ services successfully");
            } else {
                // Branch server - determine port and branch name
                int port = getBranchPortFromServerInfo(serverInfo);
                String branchName = getBranchNameFromServerInfo(serverInfo);
                servicePrefix = branchName.toUpperCase() + "_";
                
                // Branch servers use regular RMI
                registry = LocateRegistry.getRegistry("localhost", port);
                
                // Connect to branch services (no LoadBalancerService for branches)
                drinkService = (DrinkService) registry.lookup(servicePrefix + "DrinkService");
                orderService = (OrderService) registry.lookup(servicePrefix + "OrderService");
                // Branch servers might not have notification service, so make it optional
                try {
                    notificationService = (NotificationService) registry.lookup(servicePrefix + "NotificationService");
                } catch (Exception e) {
                    logger.warn("NotificationService not available on branch server: {}", e.getMessage());
                    notificationService = null;
                }
                loadBalancerService = null; // Branches don't have load balancer
                
                logger.info("Connected to {} branch services successfully", branchName);
            }
            
            // Set up real-time notifications if available
            if (notificationService != null) {
                setupNotifications();
            }
            
        } catch (Exception e) {
            logger.error("Failed to connect to services", e);
            showError("Failed to connect to services: " + e.getMessage());
        }
    }
    
    @Override
    public void initializeDashboard() {
        welcomeLabel.setText("Welcome, " + currentUser.getUsername());
        serverInfoLabel.setText("Connected to: " + serverInfo);
        
        // Load initial data
        loadDrinks();
        loadOrders();
    }
    
    private void loadDrinks() {
        Task<List<DrinkDTO>> task = new Task<List<DrinkDTO>>() {
            @Override
            protected List<DrinkDTO> call() throws Exception {
                return drinkService.getAllDrinks();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    drinksData.clear();
                    drinksData.addAll(getValue());
                    statusLabel.setText("Drinks catalog loaded");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Failed to load drinks", getException());
                    showError("Failed to load drinks: " + getException().getMessage());
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadOrders() {
        // Validate currentUser first
        if (currentUser == null || currentUser.getCustomerId() == null) {
            showError("No authenticated customer found");
            return;
        }
        
        // Debug logging to track customer ID issues
        logger.info("Loading orders for user: {} (ID: {}, CustomerID: {})", 
                  currentUser.getUsername(), 
                  currentUser.getId(), 
                  currentUser.getCustomerId());
        
        Task<List<OrderDTO>> loadOrdersTask = new Task<List<OrderDTO>>() {
            @Override
            protected List<OrderDTO> call() throws Exception {
                return orderService.getOrdersByCustomer(currentUser, currentUser.getCustomerId());
            }

            @Override
            protected void succeeded() {
                ordersData.clear();
                ordersData.addAll(getValue());
                statusLabel.setText("Orders loaded successfully");
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                String errorMessage = exception.getMessage();
                
                // Handle specific business logic errors gracefully
                if (errorMessage != null && errorMessage.contains("You can only view your own orders")) {
                    logger.info("User attempted to view orders they don't have access to: {}", errorMessage);
                    Platform.runLater(() -> {
                        statusLabel.setText("📋 No orders found for your account");
                        statusLabel.setStyle("-fx-text-fill: #666;");
                        ordersData.clear(); // Clear any existing data
                    });
                } else if (errorMessage != null && errorMessage.contains("No orders found")) {
                    logger.info("No orders found for user: {}", currentUser.getUsername());
                    Platform.runLater(() -> {
                        statusLabel.setText("📋 No orders found - start shopping to see your orders here!");
                        statusLabel.setStyle("-fx-text-fill: #666;");
                        ordersData.clear();
                    });
                } else {
                    // Handle other errors with more detail
                    logger.error("Failed to load orders", exception);
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Failed to load orders");
                        statusLabel.setStyle("-fx-text-fill: red;");
                        showAlert("Connection Error", 
                            "Unable to load your orders. Please check your connection and try again.\n\n" +
                            "Details: " + (errorMessage != null ? errorMessage : "Unknown error"));
                    });
                }
            }
        };

        Thread loadOrdersThread = new Thread(loadOrdersTask);
        loadOrdersThread.setDaemon(true);
        loadOrdersThread.start();
    }
    
    @FXML
    private void handleAddToCart() {
        DrinkDTO selectedDrink = drinksTable.getSelectionModel().getSelectedItem();
        if (selectedDrink == null) {
            showError("Please select a drink");
            return;
        }
        
        String selectedBranch = branchComboBox.getValue();
        int quantity = quantitySpinner.getValue();
        
        CartItem cartItem = new CartItem(selectedDrink, quantity, selectedBranch);
        cartItems.add(cartItem);
        
        updateCartDisplay();
        statusLabel.setText("Added " + selectedDrink.getName() + " to cart");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handlePlaceOrder() {
        if (cartItems.isEmpty()) {
            showAlert("Error", "Cart is empty");
            return;
        }
        
        try {
            String selectedBranchName = branchComboBox.getValue();
            if (selectedBranchName == null) {
                showAlert("Error", "Please select a branch");
                return;
            }
            
            // Map branch names to IDs
            Long branchId = getBranchIdByName(selectedBranchName);
            if (branchId == null) {
                showAlert("Error", "Invalid branch selection");
                return;
            }
            
            Map<Long, Integer> orderItems = new HashMap<>();
            for (CartItem item : cartItems) {
                orderItems.put(item.getDrink().getId(), item.getQuantity());
            }
            
            // Calculate total amount for payment
            BigDecimal totalAmount = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // First create the order
            OrderDTO order = orderService.placeOrder(currentUser, currentUser.getCustomerId(), branchId, orderItems);
            Long orderId = order.getId();
            logger.info("Order created with ID: {}", orderId);
            
            // Now show payment dialog
            showPaymentDialog(orderId, selectedBranchName, cartItems.size(), totalAmount);
            
        } catch (RemoteException e) {
            logger.error("Failed to place order", e);
            showAlert("Error", "Failed to place order: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while placing order", e);
            showAlert("Error", "An unexpected error occurred. Please try again.");
        }
    }
    
    /**
     * Get or create the payment service
     */
    private PaymentService getOrCreatePaymentService() {
        if (paymentService == null) {
            try {
                // Get the registry that was already established in connectToServices()
                String host = "localhost";
                int port = 1099; // Default RMI port
                
                // Parse serverInfo if available
                if (serverInfo != null && serverInfo.contains(":")) {
                    String[] parts = serverInfo.split(":");
                    if (parts.length >= 2) {
                        host = parts[0];
                        port = Integer.parseInt(parts[1]);
                    }
                }
                
                Registry registry = LocateRegistry.getRegistry(host, port, new SslRMIClientSocketFactory());
                
                // Connect to payment service based on server info
                String prefix = "HQ_"; // Default to HQ
                
                // For branch-specific roles, use branch prefix
                if (currentUser.getBranchId() != null && 
                    (currentUser.getRole().equals("branch_manager") || 
                     currentUser.getRole().equals("branch_staff") ||
                     currentUser.getRole().equals("customer"))) {
                    prefix = currentUser.getBranchName().toUpperCase() + "_";
                }
                
                try {
                    paymentService = (PaymentService) registry.lookup(prefix + "PaymentService");
                    logger.info("Connected to {} payment service", prefix);
                } catch (NotBoundException nbe) {
                    // If branch payment service not found, try HQ as fallback
                    if (!prefix.equals("HQ_")) {
                        logger.info("Branch payment service not found, trying HQ payment service as fallback");
                        paymentService = (PaymentService) registry.lookup("HQ_PaymentService");
                        logger.info("Connected to HQ payment service as fallback");
                    } else {
                        throw nbe;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to connect to payment service", e);
                showAlert("Error", "Failed to connect to payment service: " + e.getMessage());
            }
        }
        return paymentService;
    }
    
    private void showPaymentDialog(Long orderId, String branchName, int itemCount, BigDecimal totalAmount) {
        try {
            // Load the payment dialog FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment_dialog.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set up the payment
            PaymentDialogController controller = loader.getController();
            controller.setupPayment(currentUser, getOrCreatePaymentService(), orderId, branchName, itemCount, totalAmount);
            
            // Create and show the dialog
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(totalLabel.getScene().getWindow());
            dialogStage.setTitle("Payment Checkout");
            dialogStage.setScene(new Scene(root));
            
            // Show the dialog and wait for payment result
            dialogStage.show();
            
            // Handle payment result asynchronously
            controller.getPaymentResultFuture().thenAccept(result -> {
                if (result != null && "SUCCESS".equals(result.getStatus())) {
                    Platform.runLater(() -> {
                        statusLabel.setText("✅ Payment successful! Order #" + orderId + " confirmed.");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        cartItems.clear();
                        updateCartTotal();
                        loadOrders(); // Refresh orders list
                    });
                } else if (result != null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("❌ Payment failed: " + result.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    });
                } else {
                    // Payment was cancelled
                    Platform.runLater(() -> {
                        statusLabel.setText("Payment cancelled. Your order is saved but not confirmed.");
                        statusLabel.setStyle("-fx-text-fill: #666;");
                    });
                }
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Payment error: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
                return null;
            });
            
        } catch (Exception e) {
            logger.error("Error showing payment dialog", e);
            showAlert("Error", "Could not open payment dialog: " + e.getMessage());
        }
    }
    
    
    @FXML
    private void handleClearCart() {
        cartItems.clear();
        updateCartDisplay();
        statusLabel.setText("Cart cleared");
    }
    
    private void updateCartDisplay() {
        cartData.clear();
        double total = calculateCartTotal();
        totalLabel.setText(String.format("$%.2f", total));
        placeOrderButton.setDisable(cartItems.isEmpty());
        clearCartButton.setDisable(cartItems.isEmpty());
        
        for (CartItem item : cartItems) {
            cartData.add(item.toString());
        }
    }
    
    private double calculateCartTotal() {
        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal().doubleValue();
        }
        return total;
    }
    
    protected void setupNotifications() {
        try {
            // Create notification callback
            notificationCallback = new NotificationCallbackImpl(
                this::handleNotification,
                statusLabel
            );
            
            // Register for notifications
            notificationService.registerCallback(currentUser.getId(), notificationCallback);
            
            // Send welcome notification
            NotificationDTO welcomeNotification = new NotificationDTO(
                "Welcome Customer",
                "You are now connected to real-time order updates",
                NotificationDTO.NotificationType.INFO
            );
            notificationService.sendNotification(currentUser.getId(), welcomeNotification);
            
            logger.info("Real-time notifications enabled for customer user");
            
        } catch (Exception e) {
            logger.error("Failed to setup notifications", e);
            statusLabel.setText("⚠️ Notifications unavailable");
        }
    }
    
    protected void handleNotification(NotificationDTO notification) {
        logger.info("Received notification: {}", notification.getTitle());
        
        // Update UI based on notification type
        switch (notification.getType()) {
            case ORDER_CONFIRMED:
                Platform.runLater(() -> {
                    statusLabel.setText("✅ " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: green;");
                    // Refresh order history
                    loadOrderHistory();
                });
                break;
            case ORDER_CANCELLED:
                Platform.runLater(() -> {
                    statusLabel.setText("❌ " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    loadOrderHistory();
                });
                break;
            case STOCK_LOW:
                Platform.runLater(() -> {
                    statusLabel.setText("📦 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: orange;");
                    // Refresh drinks list
                    loadDrinks();
                });
                break;
            case STOCK_OUT:
                Platform.runLater(() -> {
                    statusLabel.setText("🚫 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    loadDrinks();
                });
                break;
            default:
                Platform.runLater(() -> {
                    statusLabel.setText("📢 " + notification.getTitle());
                });
                break;
        }
    }
    
    private void loadOrderHistory() {
        try {
            if (currentUser == null || currentUser.getCustomerId() == null) {
                Platform.runLater(() -> showAlert("Error", "No authenticated customer found"));
                return;
            }
            List<OrderDTO> orders = orderService.getOrdersByCustomer(currentUser, currentUser.getCustomerId());
            Platform.runLater(() -> ordersTable.getItems().setAll(orders));
        } catch (RemoteException e) {
            Platform.runLater(() -> showAlert("Error", "Failed to load orders: " + e.getMessage()));
        }
    }
    
    private void updateCartTotal() {
        BigDecimal total = cartItems.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Platform.runLater(() -> 
            totalLabel.setText("Total: $" + String.format("%.2f", total))
        );
    }
    
    @Override
    public void handleLogout() {
        try {
            // Cleanup notifications
            if (notificationCallback != null) {
                notificationCallback.setActive(false);
                if (notificationService != null) {
                    notificationService.unregisterCallback(currentUser.getId());
                }
                notificationCallback.shutdown();
            }
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Scene loginScene = new Scene(loader.load(), 800, 600);
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setTitle("Drink Business RMI System - Login");
            stage.setScene(loginScene);
            stage.setMaximized(false);
            stage.centerOnScreen();
            
            logger.info("Customer logged out successfully");
            
        } catch (Exception e) {
            logger.error("Failed to logout", e);
            showError("Failed to logout: " + e.getMessage());
        }
    }
    
    protected void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private int getBranchPortFromServerInfo(String serverInfo) {
        // Extract branch name from serverInfo and map to port
        String branchName = getBranchNameFromServerInfo(serverInfo);
        switch (branchName.toLowerCase()) {
            case "nakuru": return 1100;
            case "mombasa": return 1101;
            case "kisumu": return 1102;
            case "nairobi": return 1103;
            default: return 1100; // Default to Nakuru port
        }
    }
    
    private String getBranchNameFromServerInfo(String serverInfo) {
        // Parse branch name from serverInfo string
        // Expected format: "Branch Server (BranchName)"
        if (serverInfo.contains("(") && serverInfo.contains(")")) {
            int start = serverInfo.indexOf("(") + 1;
            int end = serverInfo.indexOf(")");
            return serverInfo.substring(start, end);
        }
        // Fallback: check if serverInfo contains branch names directly
        String lowerServerInfo = serverInfo.toLowerCase();
        if (lowerServerInfo.contains("nakuru")) return "Nakuru";
        if (lowerServerInfo.contains("mombasa")) return "Mombasa";
        if (lowerServerInfo.contains("kisumu")) return "Kisumu";
        if (lowerServerInfo.contains("nairobi")) return "Nairobi";
        return "Nakuru"; // Default
    }
    
    private Long getBranchIdByName(String branchName) {
        // Map branch names to their corresponding IDs
        // These IDs should match the database branch IDs
        switch (branchName.toLowerCase()) {
            case "nakuru": return 1L;
            case "mombasa": return 2L;
            case "kisumu": return 3L;
            case "nairobi": return 4L;
            default: return null;
        }
    }
}
