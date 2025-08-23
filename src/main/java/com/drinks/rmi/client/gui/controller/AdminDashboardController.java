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
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Enumeration;

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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import com.drinks.rmi.util.UserRoleUpdater;

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
    @FXML private Button updateRolesButton;
    @FXML private ComboBox<String> fromRoleComboBox;
    @FXML private ComboBox<String> toRoleComboBox;
    
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
    
    // Dashboard Stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalDrinksLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Button refreshStatsButton;
    
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
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("admin", "customer_support", "auditor", "globalmanager", 
                                          "branchmanager", "branch_staff", "customer");
        }
        if (branchComboBox != null) {
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
        // Disable add user by default until a branch is selected
        addUserButton.setDisable(true);
        reportTypeComboBox.getItems().addAll("Sales Report", "Stock Report", "Customer Report", "Drink Popularity Report");
        
        // Initialize role combo boxes
        fromRoleComboBox.getItems().addAll("manager", "branchmanager", "staff", "branch_staff");
        toRoleComboBox.getItems().addAll("branchmanager", "globalmanager", "customer_support", "auditor","branch_staff");
        
        // Set up button actions
        setupButtonActions();
        
        progressIndicator.setVisible(false);
        }
    }
    
    private void setupDrinksTable() {
        if (drinkIdColumn != null && drinkNameColumn != null && drinkPriceColumn != null && drinksTable != null) {
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
    }
    
    private void setupUsersTable() {
        if (userIdColumn != null && usernameColumn != null && roleColumn != null && usersTable != null) {
            userIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
            
            usersTable.setItems(usersData);
            
            // Selection listener
            usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (deleteUserButton != null) {
                    deleteUserButton.setDisable(newSelection == null);
                }
            });
        }
    }
    
    /**
     * Setup payments table with columns
     */
    private void setupPaymentsTable() {
        // Initialize payment status filter
        if (paymentStatusFilter != null) {
            paymentStatusFilter.getItems().addAll("All", "SUCCESS", "FAILED", "PENDING");
            paymentStatusFilter.setValue("All");
            paymentStatusFilter.setOnAction(e -> filterPayments());
        }
        
        // Setup table columns
        if (paymentIdColumn != null && paymentOrderIdColumn != null && paymentCustomerIdColumn != null && 
            paymentAmountColumn != null && paymentMethodColumn != null && paymentStatusColumn != null && paymentDateColumn != null) {
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
        refreshStatsButton.setOnAction(e -> refreshDashboardStats());
        updateRolesButton.setOnAction(e -> handleUpdateRoles());
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
    
    private void connectToServices() {
        try {
            // Use robust SSL connection logic
            String serverHost = getHQServerHost();
            Registry registry = getHQRegistry(serverHost, 1099);
            
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
    
    private String getHQServerHost() {
        // Check system property first
        String host = System.getProperty("hq.server.ip");
        if (host != null && !host.trim().isEmpty()) {
            logger.info("Using HQ server IP from system property: {}", host);
            return host.trim();
        }
        
        // Check environment variable
        host = System.getenv("HQ_SERVER_IP");
        if (host != null && !host.trim().isEmpty()) {
            logger.info("Using HQ server IP from environment variable: {}", host);
            return host.trim();
        }
        
        // Auto-detect network IP
        host = detectHQServerIP();
        if (host != null) {
            logger.info("Using auto-detected HQ server IP: {}", host);
            return host;
        }
        
        // Fallback to localhost
        logger.warn("No HQ server IP found, falling back to localhost");
        return "localhost";
    }
    
    private String detectHQServerIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        logger.debug("Found potential HQ server IP: {}", ip);
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to detect HQ server IP", e);
        }
        return null;
    }
    
    private Registry getHQRegistry(String host, int port) throws RemoteException {
        logger.info("Attempting to connect to HQ registry at {}:{}", host, port);
        
        // Try SSL connection first
        try {
            RMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
            Registry registry = LocateRegistry.getRegistry(host, port, socketFactory);
            
            // Test the registry connection
            registry.list();
            logger.info("Successfully connected to SSL RMI registry");
            return registry;
            
        } catch (Exception sslException) {
            logger.warn("SSL registry connection failed: {}, trying fallback", sslException.getMessage());
            
            // Fallback: try without SSL socket factory
            try {
                Registry registry = LocateRegistry.getRegistry(host, port);
                registry.list(); // Test connection
                logger.info("Connected to registry without SSL socket factory");
                return registry;
            } catch (Exception fallbackException) {
                logger.error("Both SSL and non-SSL registry connections failed");
                throw new RemoteException("Failed to connect to HQ registry", sslException);
            }
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
        loadDashboardStatistics();
        
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
                    // Update dashboard statistics
                    if (totalDrinksLabel != null) {
                        totalDrinksLabel.setText(String.valueOf(getValue().size()));
                    }
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
                    // Update dashboard statistics
                    if (totalUsersLabel != null) {
                        totalUsersLabel.setText(String.valueOf(getValue().size()));
                    }
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
    
    /**
     * Load dashboard statistics (orders and revenue)
     */
    private void loadDashboardStatistics() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Get sales report for last 30 days to calculate totals
                    LocalDate endDate = LocalDate.now();
                    LocalDate startDate = endDate.minusDays(30);
                    
                    Map<Long, SalesReportDTO> salesReports = reportService.generateAllBranchesSalesReport(
                        startDate, endDate
                    );
                    
                    // Calculate totals from all branches
                    int totalOrders = 0;
                    double totalRevenue = 0.0;
                    
                    for (SalesReportDTO report : salesReports.values()) {
                        totalOrders += report.getTotalOrders();
                        totalRevenue += report.getTotalRevenue();
                    }
                    
                    final int finalTotalOrders = totalOrders;
                    final double finalTotalRevenue = totalRevenue;
                    
                    Platform.runLater(() -> {
                        if (totalOrdersLabel != null) {
                            totalOrdersLabel.setText(String.valueOf(finalTotalOrders));
                        }
                        if (totalRevenueLabel != null) {
                            totalRevenueLabel.setText(String.format("$%.2f", finalTotalRevenue));
                        }
                    });
                    
                } catch (Exception e) {
                    logger.warn("Could not load dashboard statistics: " + e.getMessage());
                    Platform.runLater(() -> {
                        if (totalOrdersLabel != null) {
                            totalOrdersLabel.setText("N/A");
                        }
                        if (totalRevenueLabel != null) {
                            totalRevenueLabel.setText("N/A");
                        }
                    });
                }
                return null;
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
        BranchDTO selectedBranch = branchComboBox.getSelectionModel().getSelectedItem();
        if (selectedBranch == null) {
            showError("Please select a branch");
            return;
        }
        Long branchId = selectedBranch.getId();
        
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
        alert.setContentText("User: " + selected.getUsername() + " (Role: " + selected.getRole() + ")");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                statusLabel.setText("Deleting user...");
                progressIndicator.setVisible(true);
                
                Task<Boolean> deleteTask = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return authService.deleteUser(selected.getId(), currentUser);
                    }
                    
                    @Override
                    protected void succeeded() {
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            if (getValue()) {
                                statusLabel.setText("✅ User deleted successfully");
                                statusLabel.setStyle("-fx-text-fill: green;");
                                
                                // Show success message
                                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                                successAlert.setTitle("Success");
                                successAlert.setHeaderText(null);
                                successAlert.setContentText("User '" + selected.getUsername() + "' has been deleted successfully.");
                                successAlert.showAndWait();
                                
                                // Refresh the users table
                                loadUsers();
                            } else {
                                statusLabel.setText("❌ Failed to delete user");
                                statusLabel.setStyle("-fx-text-fill: red;");
                                showError("Failed to delete user. Please try again.");
                            }
                        });
                    }
                    
                    @Override
                    protected void failed() {
                        Platform.runLater(() -> {
                            progressIndicator.setVisible(false);
                            statusLabel.setText("❌ Error deleting user");
                            statusLabel.setStyle("-fx-text-fill: red;");
                            
                            Throwable exception = getException();
                            String errorMessage = "Failed to delete user";
                            if (exception != null) {
                                errorMessage += ": " + exception.getMessage();
                            }
                            showError(errorMessage);
                        });
                    }
                };
                
                Thread deleteThread = new Thread(deleteTask);
                deleteThread.setDaemon(true);
                deleteThread.start();
                
            } catch (Exception e) {
                progressIndicator.setVisible(false);
                statusLabel.setText("❌ Error deleting user");
                statusLabel.setStyle("-fx-text-fill: red;");
                showError("Failed to delete user: " + e.getMessage());
                logger.error("Error deleting user", e);
            }
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
                        // Default to last 30 days if no date range specified
                        LocalDate salesEndDate = LocalDate.now();
                        LocalDate salesStartDate = salesEndDate.minusDays(30);
                        
                        Map<Long, SalesReportDTO> salesReports = reportService.generateAllBranchesSalesReport(
                            salesStartDate, salesEndDate
                        );
                        return formatSalesReport(salesReports.values().iterator().next());
                    case "Stock Report":
                        Map<Long, StockReportDTO> stockReports = reportService.generateAllBranchesStockReport();
                        return formatStockReport(stockReports.values().iterator().next());
                    case "Customer Report":
                        // Check if a customer user is selected for individual report
                        UserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
                        
                        // Default to last 30 days if no date range specified
                        LocalDate reportEndDate = LocalDate.now();
                        LocalDate reportStartDate = reportEndDate.minusDays(30);
                        
                        if (selectedUser != null && selectedUser.getCustomerId() != null) {
                            // Generate individual customer report
                            CustomerReportDTO customerReport = reportService.generateCustomerReport(
                                selectedUser.getCustomerId(), reportStartDate, reportEndDate
                            );
                            return "=== INDIVIDUAL CUSTOMER REPORT ===\n" + 
                                   "Customer: " + selectedUser.getUsername() + "\n\n" +
                                   formatCustomerReport(customerReport);
                        } else {
                            // Generate all customers summary report
                            return AdminDashboardController.this.generateAllCustomersReport(reportStartDate, reportEndDate);
                        }
                    case "Drink Popularity Report":
                        // Default to last 30 days if no date range specified
                        LocalDate popularityEndDate = LocalDate.now();
                        LocalDate popularityStartDate = popularityEndDate.minusDays(30);
                        
                        DrinkPopularityReportDTO popularityReport = reportService.generateDrinkPopularityReport(
                            popularityStartDate, popularityEndDate
                        );
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
        
        if (report.getCustomerId() != null) {
            // Detailed individual customer report
            sb.append("Customer ID: ").append(report.getCustomerId()).append("\n");
            sb.append("Name: ").append(report.getCustomerName()).append("\n");
            sb.append("Email: ").append(report.getCustomerEmail()).append("\n");
            sb.append("Phone: ").append(report.getCustomerPhone()).append("\n\n");
            
            sb.append("=== ACTIVITY SUMMARY ===\n");
            sb.append("Total Orders: ").append(report.getTotalOrders()).append("\n");
            sb.append("Total Spent: ").append(report.getTotalSpent()).append("\n");
            sb.append("Login Count: ").append(report.getLoginCount()).append("\n\n");
            
            sb.append("=== RECENT LOGINS ===\n");
            if (!report.getLoginHistory().isEmpty()) {
                report.getLoginHistory().forEach(login -> 
                    sb.append("  ").append(login.getLoginTime())
                      .append(" from ").append(login.getIpAddress()).append("\n"));
            } else {
                sb.append("  No login history available\n");
            }
            sb.append("\n");
            
            sb.append("=== PAYMENT DETAILS ===\n");
            if (!report.getPayments().isEmpty()) {
                report.getPayments().forEach(payment -> 
                    sb.append("  ").append(payment.getAmount())
                      .append(" on ").append(payment.getCreatedAt()).append("\n"));
            } else {
                sb.append("  No payment history available\n");
            }
            sb.append("\n");
            
            sb.append("=== ORDER HISTORY ===\n");
            if (!report.getOrders().isEmpty()) {
                report.getOrders().forEach(order -> 
                    sb.append("  Order #").append(order.getId())
                      .append(": ").append(order.getTotalAmount())
                      .append(" on ").append(order.getOrderTime()).append("\n"));
            } else {
                sb.append("  No orders in this period\n");
            }
        } else {
            // Summary report for all customers
            sb.append("Total Customers: ").append(report.getTotalCustomers()).append("\n");
            sb.append("Active Customers: ").append(report.getActiveCustomers()).append("\n\n");
            sb.append("Customer Order Summary:\n");
            report.getCustomerOrderHistory().forEach((customer, orders) -> 
                sb.append("  ").append(customer).append(": ").append(orders.size()).append(" orders\n"));
        }
        
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
    
    /**
     * Generate a summary report for all customers
     */
    private String generateAllCustomersReport(LocalDate startDate, LocalDate endDate) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ALL CUSTOMERS SUMMARY REPORT ===\n");
            sb.append("Report Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");
            sb.append("💡 TIP: Select a specific customer from the Users Management tab for detailed individual reports\n\n");
            
            // Get all customer users
            List<UserDTO> allUsers = authService.getAllUsers(currentUser);
            List<UserDTO> customerUsers = allUsers.stream()
                .filter(user -> "customer".equals(user.getRole()) && user.getCustomerId() != null)
                .collect(Collectors.toList());
            
            sb.append("📊 SUMMARY STATISTICS:\n");
            sb.append("Total Customer Users: ").append(customerUsers.size()).append("\n\n");
            
            if (customerUsers.isEmpty()) {
                sb.append("No customer users found in the system.\n");
                return sb.toString();
            }
            
            sb.append("👥 CUSTOMER LIST:\n");
            for (UserDTO customer : customerUsers) {
                sb.append("  • ").append(customer.getUsername())
                  .append(" (ID: ").append(customer.getCustomerId()).append(")\n");
            }
            
            sb.append("\n📋 INDIVIDUAL REPORTS:\n");
            sb.append("To generate detailed reports for specific customers:\n");
            sb.append("1. Go to Users Management tab\n");
            sb.append("2. Select a customer from the table\n");
            sb.append("3. Return to Reports tab and generate Customer Report\n");
            
            return sb.toString();
            
        } catch (Exception e) {
            logger.error("Failed to generate all customers report", e);
            return "Error generating all customers report: " + e.getMessage();
        }
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
    
    @FXML
    private void handleUpdateRoles() {
        String fromRole = fromRoleComboBox.getValue();
        String toRole = toRoleComboBox.getValue();
        
        if (fromRole == null || toRole == null) {
            showError("Please select both 'From' and 'To' roles");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Update User Roles");
        confirmation.setHeaderText("This will update all '" + fromRole + "' roles to '" + toRole + "'");
        confirmation.setContentText("Are you sure you want to proceed?");
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Integer> updateTask = UserRoleUpdater.createUpdateTask(
                    authService,
                    currentUser,
                    fromRole,
                    toRole
                );
                
                updateTask.setOnSucceeded(e -> {
                    int count = updateTask.getValue();
                    showSuccess("Updated " + count + " user records");
                    loadUsers(); // Refresh user list
                });
                
                updateTask.setOnFailed(e -> {
                    showError("Failed to update roles: " + updateTask.getException().getMessage());
                });
                
                new Thread(updateTask).start();
            }
        });
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
    
    protected void showError(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: red;");
        }
        
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    protected void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void refreshDashboardStats() {
        statusLabel.setText("Refreshing dashboard statistics...");
        loadUsers();
        loadDrinks();
        loadDashboardStatistics();
    }
}
