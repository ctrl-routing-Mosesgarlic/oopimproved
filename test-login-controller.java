import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.InetSocketAddress;
import com.drinks.rmi.interfaces.AuthService;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Scanner;

/**
 * Exact replica of LoginController connection logic for debugging
 * This will help us identify and fix the connection issues step by step
 */
class TestLoginController {
    
    private static final Map<String, Object> authServicePool = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== LoginController Connection Debug Test ===");
        
        // Get server IP (same logic as LoginController)
        String serverHost = getServerHost();
        System.out.println("Resolved server host: " + serverHost);
        
        // Prompt for credentials
        System.out.print("Enter username (or press Enter for 'admin'): ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) username = "admin";
        
        System.out.print("Enter password (or press Enter for 'password123'): ");
        String password = scanner.nextLine().trim();
        if (password.isEmpty()) password = "password123";
        
        // Test the exact LoginController logic
        testLoginControllerLogic(serverHost, username, password);
        
        scanner.close();
    }
    
    private static String getServerHost() {
        // Exact same logic as LoginController
        String serverHost = System.getProperty("rmi.server.host");
        if (serverHost == null) {
            serverHost = System.getProperty("java.rmi.server.hostname");
        }
        if (serverHost == null) {
            serverHost = System.getProperty("server.host");
        }
        if (serverHost == null) {
            serverHost = System.getenv("RMI_SERVER_HOST");
        }
        if (serverHost == null) {
            serverHost = "localhost";
        }
        
        System.out.println("System property rmi.server.host: " + System.getProperty("rmi.server.host"));
        System.out.println("System property java.rmi.server.hostname: " + System.getProperty("java.rmi.server.hostname"));
        System.out.println("Environment RMI_SERVER_HOST: " + System.getenv("RMI_SERVER_HOST"));
        
        return serverHost;
    }
    
    private static void testLoginControllerLogic(String serverHost, String username, String password) {
        System.out.println("\n=== Testing LoginController Connection Logic ===");
        
        // Test HQ server connection (port 1099)
        int port = 1099;
        String serverType = "Headquarters (HQ)";
        
        System.out.println("Connecting to " + serverType + " on " + serverHost + ":" + port);
        
        try {
            // Test basic socket connection first
            System.out.println("Step 1: Testing network connectivity...");
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(serverHost, port), 10000); // Increased timeout
                System.out.println("✅ Basic socket connection successful");
            }

            // Test RMI registry connection with SSL
            System.out.println("\nStep 2: Testing RMI registry connection with SSL...");
            
            // Set SSL properties for RMI client
            System.setProperty("javax.net.ssl.keyStore", "config/hq-keystore.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "securepassword123");
            System.setProperty("javax.net.ssl.trustStore", "config/hq-keystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "securepassword123");
            
            Registry registry = LocateRegistry.getRegistry(serverHost, port, new SslRMIClientSocketFactory());
            String serviceName = "HQ_AuthService";
            System.out.println("Looking up service: " + serviceName);
            AuthService authService = (AuthService) registry.lookup(serviceName);
            System.out.println("✅ AuthService obtained: " + authService.getClass().getName());

            // Test login call with increased timeout
            System.out.println("\nStep 3: Testing login call...");
            testLogin(authService, username, password);
            System.out.println("✅ LOGIN CONTROLLER TEST SUCCESSFUL");
            System.out.println("Connection and authentication working correctly!");
            
        } catch (Exception e) {
            System.out.println("\n❌ LOGIN CONTROLLER TEST FAILED");
            System.out.println("Error: " + e.getMessage());
            System.out.println("\nFull stack trace:");
            e.printStackTrace();
            
            // Provide specific troubleshooting based on error type
            provideTroubleshooting(e, serverHost, port);
        }
    }
    
    private static void testNetworkConnectivity(String host, int port) throws Exception {
        // Test basic socket connection
        java.net.Socket socket = null;
        try {
            socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(host, port), 5000); // 5 second timeout
            System.out.println("✅ Basic socket connection successful");
        } catch (Exception e) {
            throw new Exception("Network connectivity failed: " + e.getMessage(), e);
        } finally {
            if (socket != null) {
                try { socket.close(); } catch (Exception ignored) {}
            }
        }
    }
    
    private static Object getAuthService(String host, int port) throws Exception {
        String key = host + ":" + port;
        
        return authServicePool.computeIfAbsent(key, k -> {
            try {
                Registry registry;
                String serviceName;
                
                if (port == 1099) {
                    // HQ server uses SSL - exact same logic as LoginController
                    RMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
                    registry = LocateRegistry.getRegistry(host, port, socketFactory);
                    serviceName = "HQ_AuthService";
                } else {
                    // Branch servers use regular RMI
                    registry = LocateRegistry.getRegistry(host, port);
                    String branchName = getBranchNameByPort(port);
                    serviceName = branchName.toUpperCase() + "_AuthService";
                }
                
                System.out.println("Looking up service: " + serviceName);
                return registry.lookup(serviceName);
                
            } catch (Exception e) {
                throw new RuntimeException("Failed to create AuthService connection", e);
            }
        });
    }
    
    private static void testLogin(Object authService, String username, String password) throws Exception {
        // Use reflection to call login method (since we don't have the interface)
        java.lang.reflect.Method loginMethod = authService.getClass().getMethod("login", String.class, String.class);
        Object result = loginMethod.invoke(authService, username, password);
        
        if (result != null) {
            System.out.println("✅ Login successful! User object: " + result.getClass().getName());
            
            // Try to get username from result using reflection
            try {
                java.lang.reflect.Method getUsernameMethod = result.getClass().getMethod("getUsername");
                String resultUsername = (String) getUsernameMethod.invoke(result);
                System.out.println("✅ Logged in as: " + resultUsername);
            } catch (Exception e) {
                System.out.println("✅ Login result received (couldn't extract username)");
            }
        } else {
            throw new Exception("Login returned null - invalid credentials");
        }
    }
    
    private static String getBranchNameByPort(int port) {
        switch (port) {
            case 1100: return "Nakuru";
            case 1101: return "Mombasa";
            case 1102: return "Kisumu";
            case 1103: return "Nairobi";
            default: return "Branch";
        }
    }
    
    private static void provideTroubleshooting(Exception e, String host, int port) {
        System.out.println("\n🔧 TROUBLESHOOTING SUGGESTIONS:");
        
        String errorMsg = e.getMessage().toLowerCase();
        
        if (errorMsg.contains("connection timed out")) {
            System.out.println("1. ⚠️  CONNECTION TIMEOUT ISSUE:");
            System.out.println("   - Server at " + host + ":" + port + " is not responding");
            System.out.println("   - Check if HQ server is actually running on " + host);
            System.out.println("   - Verify firewall allows connections on port " + port);
            System.out.println("   - Test: telnet " + host + " " + port);
        }
        
        if (errorMsg.contains("connection refused")) {
            System.out.println("2. ⚠️  CONNECTION REFUSED:");
            System.out.println("   - No service listening on " + host + ":" + port);
            System.out.println("   - HQ server is not running or not bound to external interface");
            System.out.println("   - Check server startup logs");
        }
        
        if (errorMsg.contains("ssl") || errorMsg.contains("pkix")) {
            System.out.println("3. ⚠️  SSL CERTIFICATE ISSUE:");
            System.out.println("   - SSL handshake failed");
            System.out.println("   - Verify truststore configuration");
            System.out.println("   - Check keystore files exist and passwords are correct");
        }
        
        if (errorMsg.contains("unknown host")) {
            System.out.println("4. ⚠️  DNS/HOST RESOLUTION:");
            System.out.println("   - Cannot resolve hostname: " + host);
            System.out.println("   - Check network connectivity");
            System.out.println("   - Try using IP address instead of hostname");
        }
        
        System.out.println("\n📋 IMMEDIATE ACTIONS:");
        System.out.println("1. Verify HQ server is running: ps aux | grep HQServer");
        System.out.println("2. Check port: netstat -tlnp | grep " + port);
        System.out.println("3. Test connectivity: ping " + host);
        System.out.println("4. Test port: telnet " + host + " " + port);
    }
}
