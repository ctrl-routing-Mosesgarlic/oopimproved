import org.mindrot.jbcrypt.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        String password = "password123";
        String hash = "$2a$10$8K1p/a0dL2LkqvQOuiLigOeVFh5rtVNWjr2TU.iWZWY8xO7qPJLWG";
        
        System.out.println("Testing BCrypt password verification:");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("Match: " + BCrypt.checkpw(password, hash));
        
        // Also test generating a new hash for comparison
        String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("New hash: " + newHash);
        System.out.println("New hash matches: " + BCrypt.checkpw(password, newHash));
    }
}
