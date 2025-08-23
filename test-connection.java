import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.rmi.server.RMIClientSocketFactory;

/**
 * Test class to verify RMI connection to remote server
 * This mimics the exact connection logic used in LoginController
 */
class TestConnection {
    
    public static void main(String[] args) {
        System.out.println("=== RMI Connection Test ===");
        
        // Test system properties and environment variables
        testSystemProperties();
        
        // Get server host using same logic as LoginController
        String serverHost = getServerHost();
        int port = 1099; // HQ server port
        
        System.out.println("\n=== Connection Test ===");
        System.out.println("Attempting to connect to: " + serverHost + ":" + port);
        
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
            System.out.println("The GUI client should be able to connect to the remote server.");
            
        } catch (Exception e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
            System.out.println("\nError details:");
            e.printStackTrace();
            
            System.out.println("\n🔧 Troubleshooting suggestions:");
            System.out.println("1. Verify HQ server is running on " + serverHost + ":1099");
            System.out.println("2. Check firewall settings on " + serverHost);
            System.out.println("3. Verify SSL keystore is properly configured");
            System.out.println("4. Test network connectivity: ping " + serverHost);
        }
    }
    
    private static void testSystemProperties() {
        System.out.println("=== System Properties & Environment Test ===");
        System.out.println("rmi.server.host: " + System.getProperty("rmi.server.host"));
        System.out.println("java.rmi.server.hostname: " + System.getProperty("java.rmi.server.hostname"));
        System.out.println("server.host: " + System.getProperty("server.host"));
        System.out.println("RMI_SERVER_HOST (env): " + System.getenv("RMI_SERVER_HOST"));
    }
    
    private static String getServerHost() {
        // Same logic as LoginController
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
        
        System.out.println("Resolved server host: " + serverHost);
        return serverHost;
    }
}
