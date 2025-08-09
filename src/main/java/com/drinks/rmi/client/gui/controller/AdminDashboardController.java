package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.interfaces.*;
import com.drinks.rmi.client.gui.NotificationCallbackImpl;
import com.drinks.rmi.dto.BranchDTO;
import com.drinks.rmi.dto.CustomerReportDTO;
import com.drinks.rmi.dto.DrinkPopularityReportDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.NotificationDTO;
import com.drinks.rmi.dto.PaymentDTO;
import com.drinks.rmi.dto.SalesReportDTO;
import com.drinks.rmi.dto.StockReportDTO;
import com.drinks.rmi.dto.UserDTO;
import java.rmi.registry.Registry;

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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for Admin Dashboard
 * Handles admin operations: manage drinks, users, view reports
 */
public class AdminDashboardController implements DashboardController, Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    
    // User Info
    @FXML private Label welcomeLabel;
    @FXML private Label serverInfoLabel;
    @FXML private Button logoutButton;
    
    // Drinks Management
    @FXML private TableView<DrinkDTO> drinksTable;
    @FXML private TableColumn<DrinkDTO, Long> drinkIdColumn;
    @FXML private TableColumn<DrinkDTO, String> drinkNameColumn;
    @FXML private TableColumn<DrinkDTO, Double> drinkPriceColumn;
    @FXML private TextField drinkNameField;
    @FXML private TextField drinkPriceField;
    @FXML private Button addDrinkButton;
    @FXML private Button updateDrinkButton;
    @FXML private Button deleteDrinkButton;
    
    // Users Management
    @FXML private TableView<UserDTO> usersTable;
    @FXML private TableColumn<UserDTO, Long> userIdColumn;
    @FXML private TableColumn<UserDTO, String> usernameColumn;
    @FXML private TableColumn<UserDTO, String> roleColumn;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<BranchDTO> branchComboBox;
    @FXML private Button addUserButton;
    @FXML private Button deleteUserButton;
    
    // Reports
    @FXML private TextArea reportTextArea;
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private Button generateReportButton;
    
    // Payments Management
    @FXML private TableView<PaymentDTO> paymentsTable;
    @FXML private TableColumn<PaymentDTO, Long> paymentIdColumn;
    @FXML private TableColumn<PaymentDTO, Long> paymentOrderIdColumn;
    @FXML private TableColumn<PaymentDTO, String> paymentCustomerIdColumn;
    @FXML private TableColumn<PaymentDTO, BigDecimal> paymentAmountColumn;
    @FXML private TableColumn<PaymentDTO, String> paymentMethodColumn;
    @FXML private TableColumn<PaymentDTO, String> paymentStatusColumn;
    @FXML private TableColumn<PaymentDTO, LocalDateTime> paymentDateColumn;
    @FXML private Button refreshPaymentsButton;
    @FXML private ComboBox<String> paymentStatusFilter;
    
    // Status
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    
    // Data
    private UserDTO currentUser;
    private AuthService authService;
    private DrinkService drinkService;
    private ReportService reportService;
    private NotificationService notificationService;
    private LoadBalancerService loadBalancerService;
    private PaymentService paymentService;
    private NotificationCallbackImpl notificationCallback;
    private String serverInfo;
    
    private ObservableList<DrinkDTO> drinksData = FXCollections.observableArrayList();
    private ObservableList<UserDTO> usersData = FXCollections.observableArrayList();
    private ObservableList<PaymentDTO> paymentsData = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing Admin Dashboard");
        
        // Initialize tables
        setupDrinksTable();
        setupUsersTable();
        setupPaymentsTable();
        
        // Initialize combo boxes
        roleComboBox.getItems().addAll("admin", "customer_support", "auditor", "global_manager", 
                                      "branch_manager", "branch_staff", "customer");
        branchComboBox.setCellFactory(param -> new ListCell<BranchDTO>() {
            @Override
            protected void updateItem(BranchDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        branchComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Enable add user button
                addUserButton.setDisable(false);
            } else {
                addUserButton.setDisable(true);
            }
        });
        reportTypeComboBox.getItems().addAll("Sales Report", "Stock Report", "Customer Report", "Drink Popularity Report");
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
    }
    
    private void setupDrinksTable() {
        drinkIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        drinkNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        drinkPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        
        drinksTable.setItems(drinksData);
        
        // Selection listener
        drinksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                drinkNameField.setText(newSelection.getName());
                drinkPriceField.setText(String.valueOf(newSelection.getPrice()));
                updateDrinkButton.setDisable(false);
                deleteDrinkButton.setDisable(false);
            } else {
                updateDrinkButton.setDisable(true);
                deleteDrinkButton.setDisable(true);
            }
        });
    }
    
    private void setupUsersTable() {
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        usersTable.setItems(usersData);
        
        // Selection listener
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            deleteUserButton.setDisable(newSelection == null);
        });
    }
    
    /**
     * Setup payments table with columns
     */
    private void setupPaymentsTable() {
        // Initialize payment status filter
        paymentStatusFilter.getItems().addAll("All", "SUCCESS", "FAILED", "PENDING");
        paymentStatusFilter.setValue("All");
        paymentStatusFilter.setOnAction(e -> filterPayments());
        
        // Setup table columns
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        paymentOrderIdColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        paymentCustomerIdColumn.setCellValueFactory(new PropertyValueFactory<>("customerId"));
        paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Format date column
        paymentDateColumn.setCellFactory(column -> new TableCell<PaymentDTO, LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        // Format amount column
        paymentAmountColumn.setCellFactory(column -> new TableCell<PaymentDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("$%.2f", item));
                }
            }
        });
        
        // Set data
        paymentsTable.setItems(paymentsData);
    }
    
    private void setupButtonActions() {
        logoutButton.setOnAction(e -> handleLogout());
        addDrinkButton.setOnAction(e -> handleAddDrink());
        updateDrinkButton.setOnAction(e -> handleUpdateDrink());
        deleteDrinkButton.setOnAction(e -> handleDeleteDrink());
        addUserButton.setOnAction(e -> handleCreateStaffUser());
        deleteUserButton.setOnAction(e -> handleDeleteUser());
        generateReportButton.setOnAction(e -> handleGenerateReport());
        refreshPaymentsButton.setOnAction(e -> loadPayments());
    }
    
    @Override
    public void setUserData(UserDTO user, AuthService authService, String serverInfo) {
        this.currentUser = user;
        this.authService = authService;
        this.serverInfo = serverInfo;
        
        // Connect to other services
        connectToServices();
        
        // Initialize dashboard
        initializeDashboard();
    }
    
    private void connectToServices() {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            
            drinkService = (DrinkService) registry.lookup("HQ_DrinkService");
            reportService = (ReportService) registry.lookup("HQ_ReportService");
            notificationService = (NotificationService) registry.lookup("HQ_NotificationService");
            loadBalancerService = (LoadBalancerService) registry.lookup("HQ_LoadBalancerService");
            paymentService = (PaymentService) registry.lookup("HQ_PaymentService");
            
            logger.info("Successfully connected to all HQ services");
            
        } catch (Exception e) {
            logger.error("Failed to connect to services", e);
            showError("Failed to connect to services: " + e.getMessage());
        }
    }
    
    @Override
    public void initializeDashboard() {
        welcomeLabel.setText("Welcome, " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        serverInfoLabel.setText("Connected to: " + serverInfo);
        
        // Load data
        loadDrinks();
        loadUsers();
        loadBranches();
        loadPayments();
        
        // Setup notifications
        setupNotifications();
        
        // Update status
        statusLabel.setText("Dashboard initialized successfully");
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
                    statusLabel.setText("Drinks loaded successfully");
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
    
    private void loadUsers() {
        Task<List<UserDTO>> task = new Task<List<UserDTO>>() {
            @Override
            protected List<UserDTO> call() throws Exception {
                return authService.getAllUsers(currentUser);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    usersData.clear();
                    usersData.addAll(getValue());
                    statusLabel.setText("Users loaded successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Failed to load users", getException());
                    showError("Failed to load users: " + getException().getMessage());
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private void loadBranches() {
        Task<List<BranchDTO>> task = new Task<List<BranchDTO>>() {
            @Override
            protected List<BranchDTO> call() throws Exception {
                List<BranchDTO> branches = loadBalancerService.getAllBranches().stream()
                    .map(branchInfo -> new BranchDTO(
                        null, // branch ID not available in BranchInfo
                        branchInfo.getName(),
                        branchInfo.getHost(), // using host as location
                        branchInfo.getName() // using name as manager name
                    ))
                    .collect(Collectors.toList());
                return branches;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    branchComboBox.getItems().clear();
                    branchComboBox.getItems().addAll(getValue());
                    statusLabel.setText("Branches loaded successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Failed to load branches", getException());
                    showError("Failed to load branches: " + getException().getMessage());
                });
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Load payment records from the server
     */
    private void loadPayments() {
        statusLabel.setText("Loading payment records...");
        progressIndicator.setVisible(true);
        
        Task<List<PaymentDTO>> task = new Task<List<PaymentDTO>>() {
            @Override
            protected List<PaymentDTO> call() throws Exception {
                return paymentService.getAllPayments(currentUser);
            }
            
            @Override
            protected void succeeded() {
                List<PaymentDTO> payments = getValue();
                paymentsData.clear();
                paymentsData.addAll(payments);
                filterPayments();
                progressIndicator.setVisible(false);
                statusLabel.setText("Loaded " + payments.size() + " payment records");
                logger.info("Loaded {} payment records", payments.size());
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Failed to load payment records", exception);
                progressIndicator.setVisible(false);
                statusLabel.setText("Failed to load payment records");
                showError("Failed to load payment records: " + exception.getMessage());
            }
        };
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleAddDrink() {
        String name = drinkNameField.getText().trim();
        String priceText = drinkPriceField.getText().trim();
        
        if (name.isEmpty() || priceText.isEmpty()) {
            showError("Please enter drink name and price");
            return;
        }
        
        try {
            double price = Double.parseDouble(priceText);
            
            Task<DrinkDTO> task = new Task<DrinkDTO>() {
                @Override
                protected DrinkDTO call() throws Exception {
                    return drinkService.createDrink(currentUser, name, price);
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        drinksData.add(getValue());
                        drinkNameField.clear();
                        drinkPriceField.clear();
                        statusLabel.setText("Drink added successfully");
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        logger.error("Failed to add drink", getException());
                        showError("Failed to add drink: " + getException().getMessage());
                    });
                }
            };
            
            new Thread(task).start();
            
        } catch (NumberFormatException e) {
            showError("Please enter a valid price");
        }
    }
    
    @FXML
    private void handleUpdateDrink() {
        DrinkDTO selected = drinksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        String name = drinkNameField.getText().trim();
        String priceText = drinkPriceField.getText().trim();
        
        if (name.isEmpty() || priceText.isEmpty()) {
            showError("Please enter drink name and price");
            return;
        }
        
        try {
            double price = Double.parseDouble(priceText);
            
            Task<DrinkDTO> task = new Task<DrinkDTO>() {
                @Override
                protected DrinkDTO call() throws Exception {
                    return drinkService.updateDrink(currentUser, selected.getId(), name, price);
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        loadDrinks(); // Refresh table
                        statusLabel.setText("Drink updated successfully");
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        logger.error("Failed to update drink", getException());
                        showError("Failed to update drink: " + getException().getMessage());
                    });
                }
            };
            
            new Thread(task).start();
            
        } catch (NumberFormatException e) {
            showError("Please enter a valid price");
        }
    }
    
    @FXML
    private void handleDeleteDrink() {
        DrinkDTO selected = drinksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Drink");
        alert.setHeaderText("Are you sure you want to delete this drink?");
        alert.setContentText("Drink: " + selected.getName());
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> task = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return drinkService.deleteDrink(currentUser, selected.getId());
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        drinksData.remove(selected);
                        statusLabel.setText("Drink deleted successfully");
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        logger.error("Failed to delete drink", getException());
                        showError("Failed to delete drink: " + getException().getMessage());
                    });
                }
            };
            
            new Thread(task).start();
        }
    }
    
    @FXML
    private void handleCreateStaffUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        Long branchId = branchComboBox.getSelectionModel().getSelectedItem().getId();
        
        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError("Please fill in all required fields");
            return;
        }
        
        Task<UserDTO> task = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                return authService.createStaffUser(username, password, role, branchId);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    usersData.add(getValue());
                    usernameField.clear();
                    passwordField.clear();
                    roleComboBox.setValue(null);
                    branchComboBox.setValue(null);
                    statusLabel.setText("User created successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Failed to create user", getException());
                    showError("Failed to create user: " + getException().getMessage());
                });
            }
        };
        
        new Thread(task).start();
    }
    
    @FXML
    private void handleDeleteUser() {
        UserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete User");
        alert.setHeaderText("Are you sure you want to delete this user?");
        alert.setContentText("User: " + selected.getUsername());
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Note: We would need to implement deleteUser in AuthService
            showError("Delete user functionality not yet implemented in AuthService");
        }
    }
    
    @FXML
    private void handleGenerateReport() {
        String reportType = reportTypeComboBox.getValue();
        if (reportType == null) {
            showError("Please select a report type");
            return;
        }
        
        progressIndicator.setVisible(true);
        reportTextArea.clear();
        
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                switch (reportType) {
                    case "Sales Report":
                        Map<Long, SalesReportDTO> salesReports = reportService.generateAllBranchesSalesReport(null, null);
                        return formatSalesReport(salesReports.values().iterator().next());
                    case "Stock Report":
                        Map<Long, StockReportDTO> stockReports = reportService.generateAllBranchesStockReport();
                        return formatStockReport(stockReports.values().iterator().next());
                    case "Customer Report":
                        CustomerReportDTO customerReport = reportService.generateCustomerReport(null, null, null);
                        return formatCustomerReport(customerReport);
                    case "Drink Popularity Report":
                        DrinkPopularityReportDTO popularityReport = reportService.generateDrinkPopularityReport(null, null);
                        return formatPopularityReport(popularityReport);
                    default:
                        throw new IllegalArgumentException("Unknown report type: " + reportType);
                }
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    reportTextArea.setText(getValue());
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Report generated successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    logger.error("Failed to generate report", getException());
                    showError("Failed to generate report: " + getException().getMessage());
                    progressIndicator.setVisible(false);
                });
            }
        };
        
        new Thread(task).start();
    }
    
    private String formatSalesReport(SalesReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SALES REPORT ===\n\n");
        sb.append("Total Orders: ").append(report.getTotalOrders()).append("\n");
        sb.append("Total Revenue: $").append(String.format("%.2f", report.getTotalRevenue())).append("\n\n");
        sb.append("Sales by Drink:\n");
        report.getSalesByDrink().forEach((drink, amount) -> 
            sb.append("  ").append(drink).append(": $").append(String.format("%.2f", amount)).append("\n"));
        return sb.toString();
    }
    
    private String formatStockReport(StockReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== STOCK REPORT ===\n\n");
        sb.append("Total Stock Items: ").append(report.getTotalItems()).append("\n");
        sb.append("Low Stock Items: ").append(report.getLowStockCount()).append("\n");
        sb.append("Out of Stock Items: ").append(report.getOutOfStockCount()).append("\n\n");
        sb.append("Stock Details:\n");
        report.getStockItems().forEach(stock -> 
            sb.append("  ").append(stock.getDrinkName()).append(" (").append(stock.getBranchName())
              .append("): ").append(stock.getQuantity()).append(" units\n"));
        return sb.toString();
    }
    
    private String formatCustomerReport(CustomerReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CUSTOMER REPORT ===\n\n");
        sb.append("Total Customers: ").append(report.getTotalCustomers()).append("\n");
        sb.append("Active Customers: ").append(report.getActiveCustomers()).append("\n\n");
        sb.append("Customer Details:\n");
        report.getCustomerOrderHistory().forEach((customer, orders) -> 
            sb.append("  ").append(customer).append(": ").append(orders).append(" orders\n"));
        return sb.toString();
    }
    
    private String formatPopularityReport(DrinkPopularityReportDTO report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DRINK POPULARITY REPORT ===\n\n");
        sb.append("Most Popular Drinks:\n");
        report.getDrinkPopularity().forEach(drink -> 
            sb.append("  ").append(drink.getDrinkName()).append(": ").append(drink.getTotalOrders())
              .append(" orders, $").append(String.format("%.2f", drink.getTotalRevenue())).append(" revenue\n"));
        return sb.toString();
    }
    
    private void setupNotifications() {
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
                "Admin Connected",
                "You are now connected to real-time notifications",
                NotificationDTO.NotificationType.INFO
            );
            notificationService.sendNotification(currentUser.getId(), welcomeNotification);
            
            logger.info("Real-time notifications enabled for admin user");
            
        } catch (Exception e) {
            logger.error("Failed to setup notifications", e);
            statusLabel.setText("⚠️ Notifications unavailable");
        }
    }
    
    private void handleNotification(NotificationDTO notification) {
        logger.info("Received notification: {}", notification.getTitle());
        
        // Update UI based on notification type
        switch (notification.getType()) {
            case STOCK_LOW:
            case STOCK_OUT:
                // Refresh data if needed
                Platform.runLater(() -> {
                    statusLabel.setText("📦 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: orange;");
                });
                break;
            case USER_REGISTERED:
                // Refresh user list
                Platform.runLater(() -> {
                    loadUsers();
                    statusLabel.setText("👤 " + notification.getTitle());
                });
                break;
            case ORDER_CONFIRMED:
                Platform.runLater(() -> {
                    statusLabel.setText("🛒 " + notification.getTitle());
                    statusLabel.setStyle("-fx-text-fill: green;");
                });
                break;
            default:
                Platform.runLater(() -> {
                    statusLabel.setText("📢 " + notification.getTitle());
                });
                break;
        }
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
            
            logger.info("User logged out successfully");
            
        } catch (Exception e) {
            logger.error("Failed to logout", e);
            showError("Failed to logout: " + e.getMessage());
        }
    }
    

    
    /**
     * Filter payments based on selected status
     */
    private void filterPayments() {
        String selectedStatus = paymentStatusFilter.getValue();
        
        if (selectedStatus == null || selectedStatus.equals("All")) {
            paymentsTable.setItems(paymentsData);
        } else {
            ObservableList<PaymentDTO> filteredData = paymentsData.filtered(
                payment -> payment.getStatus().equals(selectedStatus)
            );
            paymentsTable.setItems(filteredData);
        }
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
