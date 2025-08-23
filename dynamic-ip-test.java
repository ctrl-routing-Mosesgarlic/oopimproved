import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Scanner;

/**
 * Dynamic IP test - prompts user for server IP and tests connection
 * This mimics the exact connection logic used in LoginController
 */
class DynamicIPTest {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Dynamic RMI Connection Test ===");
        
        // Prompt for server IP
        System.out.print("Enter server IP address (or press Enter for localhost): ");
        String serverIP = scanner.nextLine().trim();
        if (serverIP.isEmpty()) {
            serverIP = "localhost";
        }
        
        System.out.print("Enter server port (or press Enter for 1099): ");
        String portInput = scanner.nextLine().trim();
        int port = portInput.isEmpty() ? 1099 : Integer.parseInt(portInput);
        
        // Set environment variable and system properties
        System.setProperty("rmi.server.host", serverIP);
        System.setProperty("java.rmi.server.hostname", serverIP);
        
        // Test the connection
        testConnection(serverIP, port);
        
        scanner.close();
    }
    
    private static void testConnection(String serverHost, int port) {
        System.out.println("\n=== Connection Test ===");
        System.out.println("Testing connection to: " + serverHost + ":" + port);
        
        // Test system properties
        System.out.println("\nSystem Properties:");
        System.out.println("  rmi.server.host: " + System.getProperty("rmi.server.host"));
        System.out.println("  java.rmi.server.hostname: " + System.getProperty("java.rmi.server.hostname"));
        System.out.println("  javax.net.ssl.trustStore: " + System.getProperty("javax.net.ssl.trustStore"));
        
        try {
            // Use same connection logic as LoginController
            Registry registry;
            RMIClientSocketFactory socketFactory = new SslRMIClientSocketFactory();
            registry = LocateRegistry.getRegistry(serverHost, port, socketFactory);
            
            System.out.println("✅ Registry connection established");
            
            // Try to lookup the service
            String serviceName = "HQ_AuthService";
            Object service = registry.lookup(serviceName);
            
            System.out.println("✅ Service lookup successful: " + serviceName);
            System.out.println("✅ Service object: " + service.getClass().getName());
            
            System.out.println("\n🎉 CONNECTION TEST PASSED!");
            System.out.println("Server at " + serverHost + ":" + port + " is accessible and ready for login.");
            
        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            System.out.println("\nError details:");
            e.printStackTrace();
            
            System.out.println("\n🔧 Troubleshooting suggestions:");
            System.out.println("1. Verify HQ server is running on " + serverHost + ":" + port);
            System.out.println("2. Check firewall settings on " + serverHost);
            System.out.println("3. Verify SSL keystore is properly configured");
            System.out.println("4. Test network connectivity: ping " + serverHost);
            
            if (e.getMessage().contains("PKIX") || e.getMessage().contains("SSL")) {
                System.out.println("5. SSL Certificate issue - ensure truststore is configured:");
                System.out.println("   -Djavax.net.ssl.trustStore=config/hq-keystore.jks");
                System.out.println("   -Djavax.net.ssl.trustStorePassword=securepassword123");
            }
        }
    }
}
