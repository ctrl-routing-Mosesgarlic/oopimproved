package com.drinks.rmi.client;

import com.drinks.rmi.dto.OrderDTO;
import com.drinks.rmi.dto.SalesReportDTO;
import com.drinks.rmi.dto.StockDTO;
import com.drinks.rmi.dto.DrinkDTO;
import com.drinks.rmi.dto.UserDTO;
import com.drinks.rmi.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.util.*;

/**
 * Command-line client application for the Drink Business RMI system
 */
public class DrinkBusinessClient {
    
    private static final Logger logger = LoggerFactory.getLogger(DrinkBusinessClient.class);
    private static final Scanner scanner = new Scanner(System.in);
    
    // Server configurations
    private static final String HQ_HOST = "localhost:1099";
    private static final Map<String, String> BRANCH_HOSTS = Map.of(
        "Nakuru", "localhost:1100",
        "Mombasa", "localhost:1101", 
        "Kisumu", "localhost:1102",
        "Nairobi", "localhost:1103"
    );
    
    private UserDTO currentUser;
    private String currentServerType;
    private AuthService authService;
    private DrinkService drinkService;
    private StockService stockService;
    private OrderService orderService;
    private ReportService reportService;
    
    public static void main(String[] args) {
        DrinkBusinessClient client = new DrinkBusinessClient();
        client.start();
    }
    
    public void start() {
        System.out.println("=== Welcome to Drink Business RMI System ===");
        
        try {
            if (!login()) {
                System.out.println("Login failed. Exiting...");
                return;
            }
            
            mainMenu();
            
        } catch (Exception e) {
            logger.error("Client error", e);
            System.err.println("An error occurred: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
    
    private boolean login() {
        System.out.println("=== Login ===");
        
        try {
            String serverChoice = chooseServer();
            if (serverChoice == null) return false;
            
            if (!connectToServer(serverChoice)) return false;
            
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();
            
            currentUser = authService.login(username, password);
            
            if (currentUser != null) {
                System.out.println("\nLogin successful!");
                System.out.println("Welcome, " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                return true;
            } else {
                System.out.println("Invalid username or password.");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Login error", e);
            System.err.println("Login failed: " + e.getMessage());
            return false;
        }
    }
    
    private String chooseServer() {
        System.out.println("Choose server:");
        System.out.println("1. Headquarters (HQ)");
        System.out.println("2. Branch Server");
        System.out.print("Enter choice (1-2): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1": return "HQ";
            case "2": return chooseBranch();
            default: 
                System.out.println("Invalid choice.");
                return null;
        }
    }
    
    private String chooseBranch() {
        System.out.println("\nAvailable branches:");
        List<String> branches = new ArrayList<>(BRANCH_HOSTS.keySet());
        for (int i = 0; i < branches.size(); i++) {
            System.out.println((i + 1) + ". " + branches.get(i));
        }
        
        System.out.print("Enter branch choice: ");
        String choice = scanner.nextLine().trim();
        
        try {
            int index = Integer.parseInt(choice) - 1;
            if (index >= 0 && index < branches.size()) {
                return branches.get(index);
            }
        } catch (NumberFormatException e) {
            // Fall through to error
        }
        
        System.out.println("Invalid choice.");
        return null;
    }
    
    private boolean connectToServer(String serverChoice) {
        try {
            currentServerType = serverChoice;
            String host = "HQ".equals(serverChoice) ? HQ_HOST : BRANCH_HOSTS.get(serverChoice);
            String servicePrefix = "HQ".equals(serverChoice) ? "HQ_" : serverChoice.toUpperCase() + "_";
            
            // Use SSL RMI client socket factory for HQ connections
            if ("HQ".equals(serverChoice)) {
                // Parse host and port for SSL registry connection
                String[] hostParts = host.split(":");
                String hostname = hostParts[0];
                int port = Integer.parseInt(hostParts[1]);
                
                // Get SSL-enabled registry
                Registry registry = LocateRegistry.getRegistry(hostname, port, new SslRMIClientSocketFactory());
                
                // Lookup services using SSL registry
                authService = (AuthService) registry.lookup(servicePrefix + "AuthService");
                drinkService = (DrinkService) registry.lookup(servicePrefix + "DrinkService");
                stockService = (StockService) registry.lookup(servicePrefix + "StockService");
                orderService = (OrderService) registry.lookup(servicePrefix + "OrderService");
                reportService = (ReportService) registry.lookup(servicePrefix + "ReportService");
            } else {
                // Use regular RMI for branch servers (non-SSL)
                String baseUrl = "rmi://" + host + "/";
                authService = (AuthService) Naming.lookup(baseUrl + servicePrefix + "AuthService");
                drinkService = (DrinkService) Naming.lookup(baseUrl + servicePrefix + "DrinkService");
                stockService = (StockService) Naming.lookup(baseUrl + servicePrefix + "StockService");
                orderService = (OrderService) Naming.lookup(baseUrl + servicePrefix + "OrderService");
            }
            
            System.out.println("Connected to " + serverChoice + " server successfully.");
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to connect to server: {}", serverChoice, e);
            System.err.println("Failed to connect to " + serverChoice + " server: " + e.getMessage());
            return false;
        }
    }
    
    private void mainMenu() {
        while (true) {
            System.out.println("\n=== Main Menu ===");
            System.out.println("Server: " + currentServerType + " | User: " + currentUser.getUsername());
            
            showMenuOptions();
            
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine().trim();
            
            if ("0".equals(choice)) {
                System.out.println("Goodbye!");
                break;
            }
            
            handleMenuChoice(choice);
        }
    }
    
    private void showMenuOptions() {
        String role = currentUser.getRole().toLowerCase();
        
        switch (role) {
            case "admin":
                System.out.println("1. View All Drinks");
                System.out.println("2. Create New Drink");
                System.out.println("3. View All Users");
                if ("HQ".equals(currentServerType)) {
                    System.out.println("4. Generate Reports");
                }
                break;
                
            case "customer":
                System.out.println("1. View Available Drinks");
                System.out.println("2. Place Order");
                System.out.println("3. View My Orders");
                break;
                
            case "manager":
            case "staff":
                System.out.println("1. View Available Drinks");
                System.out.println("2. View Branch Stock");
                System.out.println("3. Update Stock");
                System.out.println("4. View Branch Orders");
                break;
                
            default:
                System.out.println("1. View Available Drinks");
                if ("HQ".equals(currentServerType)) {
                    System.out.println("2. Generate Reports");
                }
                break;
        }
        
        System.out.println("0. Exit");
    }
    
    private void handleMenuChoice(String choice) {
        try {
            String role = currentUser.getRole().toLowerCase();
            
            switch (choice) {
                case "1":
                    viewAllDrinks();
                    break;
                case "2":
                    if ("admin".equals(role)) {
                        createNewDrink();
                    } else if ("customer".equals(role)) {
                        placeOrder();
                    } else if ("manager".equals(role) || "staff".equals(role)) {
                        viewBranchStock();
                    } else if ("HQ".equals(currentServerType)) {
                        generateSimpleReport();
                    }
                    break;
                case "3":
                    if ("admin".equals(role)) {
                        viewAllUsers();
                    } else if ("customer".equals(role)) {
                        viewMyOrders();
                    } else if ("manager".equals(role) || "staff".equals(role)) {
                        updateStock();
                    }
                    break;
                case "4":
                    if ("admin".equals(role) && "HQ".equals(currentServerType)) {
                        generateSimpleReport();
                    } else if ("manager".equals(role) || "staff".equals(role)) {
                        viewBranchOrders();
                    }
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } catch (Exception e) {
            logger.error("Error handling menu choice", e);
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    private void viewAllDrinks() throws Exception {
        System.out.println("\n=== Available Drinks ===");
        List<DrinkDTO> drinks = drinkService.getAllDrinks();
        
        if (drinks.isEmpty()) {
            System.out.println("No drinks available.");
        } else {
            System.out.printf("%-5s %-20s %-10s%n", "ID", "Name", "Price");
            System.out.println("-------------------------------------");
            for (DrinkDTO drink : drinks) {
                System.out.printf("%-5d %-20s $%-9.2f%n", 
                    drink.getId(), drink.getName(), drink.getPrice().doubleValue());
            }
        }
    }
    
    private void createNewDrink() throws Exception {
        System.out.println("\n=== Create New Drink ===");
        System.out.print("Drink name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Price: $");
        try {
            double price = Double.parseDouble(scanner.nextLine().trim());
            
            DrinkDTO drink = drinkService.createDrink(currentUser, name, price);
            if (drink != null) {
                System.out.println("Drink created successfully: " + drink.getName());
            } else {
                System.out.println("Failed to create drink.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid price format.");
        }
    }
    
    private void viewAllUsers() throws Exception {
        System.out.println("\n=== All Users ===");
        List<UserDTO> users = authService.getAllUsers(currentUser);
        
        if (users.isEmpty()) {
            System.out.println("No users found.");
        } else {
            System.out.printf("%-5s %-20s %-15s%n", "ID", "Username", "Role");
            System.out.println("------------------------------------------");
            for (UserDTO user : users) {
                System.out.printf("%-5d %-20s %-15s%n", 
                    user.getId(), user.getUsername(), user.getRole());
            }
        }
    }
    
    private void placeOrder() throws Exception {
        if (currentUser.getCustomerId() == null) {
            System.out.println("Only customers can place orders.");
            return;
        }
        
        System.out.println("\n=== Place Order ===");
        viewAllDrinks();
        
        Map<Long, Integer> orderItems = new HashMap<>();
        
        while (true) {
            System.out.print("\nEnter drink ID (0 to finish): ");
            try {
                long drinkId = Long.parseLong(scanner.nextLine().trim());
                if (drinkId == 0) break;
                
                System.out.print("Enter quantity: ");
                int quantity = Integer.parseInt(scanner.nextLine().trim());
                
                orderItems.put(drinkId, quantity);
                System.out.println("Added to order.");
                
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
            }
        }
        
        if (orderItems.isEmpty()) {
            System.out.println("No items to order.");
            return;
        }
        
        Long branchId = getBranchIdForOrder();
        if (branchId == null) return;
        
        try {
            OrderDTO order = orderService.placeOrder(currentUser, currentUser.getCustomerId(), branchId, orderItems);
            if (order != null) {
                System.out.println("Order placed successfully! Order ID: " + order.getId());
            } else {
                System.out.println("Failed to place order.");
            }
        } catch (Exception e) {
            System.out.println("Order failed: " + e.getMessage());
        }
    }
    
    private Long getBranchIdForOrder() {
        if (!"HQ".equals(currentServerType)) {
            switch (currentServerType) {
                case "Nakuru": return 1L;
                case "Mombasa": return 2L;
                case "Kisumu": return 3L;
                case "Nairobi": return 4L;
                default: return 1L;
            }
        } else {
            System.out.println("Choose branch: 1=Nakuru, 2=Mombasa, 3=Kisumu, 4=Nairobi");
            System.out.print("Enter choice: ");
            
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= 4) {
                    return (long) choice;
                }
            } catch (NumberFormatException e) {
                // Fall through
            }
            
            System.out.println("Invalid choice.");
            return null;
        }
    }
    
    private void viewMyOrders() throws Exception {
        if (currentUser.getCustomerId() == null) {
            System.out.println("Only customers can view orders.");
            return;
        }
        
        System.out.println("\n=== My Orders ===");
        List<OrderDTO> orders = orderService.getOrdersByCustomer(currentUser, currentUser.getCustomerId());
        
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
        } else {
            for (OrderDTO order : orders) {
                System.out.printf("Order #%d - %s - $%.2f%n", 
                    order.getId(), order.getBranchName(), order.getTotalAmount());
            }
        }
    }
    
    private void viewBranchStock() throws Exception {
        Long branchId = getBranchIdFromServerType();
        if (branchId == null) return;
        
        System.out.println("\n=== Branch Stock ===");
        List<StockDTO> stockItems = stockService.getStockByBranch(branchId);
        
        if (stockItems.isEmpty()) {
            System.out.println("No stock items found.");
        } else {
            System.out.printf("%-20s %-10s%n", "Drink", "Quantity");
            System.out.println("------------------------------");
            for (StockDTO stock : stockItems) {
                System.out.printf("%-20s %-10d%n", stock.getDrinkName(), stock.getQuantity());
            }
        }
    }
    
    private void updateStock() throws Exception {
        Long branchId = getBranchIdFromServerType();
        if (branchId == null) return;
        
        System.out.println("\n=== Update Stock ===");
        viewAllDrinks();
        
        System.out.print("Enter drink ID: ");
        try {
            long drinkId = Long.parseLong(scanner.nextLine().trim());
            
            System.out.print("Enter new quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            
            StockDTO stock = stockService.updateStockQuantity(branchId, drinkId, quantity);
            if (stock != null) {
                System.out.println("Stock updated successfully: " + stock.getDrinkName() + " = " + stock.getQuantity());
            } else {
                System.out.println("Failed to update stock.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }
    
    private void viewBranchOrders() throws Exception {
        Long branchId = getBranchIdFromServerType();
        if (branchId == null) return;
        
        System.out.println("\n=== Branch Orders ===");
        List<OrderDTO> orders = orderService.getOrdersByBranch(currentUser, branchId);
        
        if (orders.isEmpty()) {
            System.out.println("No orders found.");
        } else {
            for (OrderDTO order : orders) {
                System.out.printf("Order #%d - %s - $%.2f%n", 
                    order.getId(), order.getCustomerName(), order.getTotalAmount());
            }
        }
    }
    
    private void generateSimpleReport() throws Exception {
        if (reportService == null) {
            System.out.println("Reports are only available at HQ.");
            return;
        }
        
        System.out.println("\n=== Simple Sales Report ===");
        java.time.LocalDate endDate = java.time.LocalDate.now();
        java.time.LocalDate startDate = endDate.minusDays(30);
        
        Map<Long, SalesReportDTO> reports = reportService.generateAllBranchesSalesReport(startDate, endDate);
        
        System.out.println("Last 30 days sales:");
        for (SalesReportDTO report : reports.values()) {
            System.out.printf("%s: %d orders, $%.2f%n", 
                report.getBranchName(), report.getTotalOrders(), report.getTotalSales().doubleValue());
        }
    }
    
    private Long getBranchIdFromServerType() {
        if ("HQ".equals(currentServerType)) {
            System.out.println("This operation is only available on branch servers.");
            return null;
        }
        
        switch (currentServerType) {
            case "Nakuru": return 1L;
            case "Mombasa": return 2L;
            case "Kisumu": return 3L;
            case "Nairobi": return 4L;
            default: return null;
        }
    }
}
