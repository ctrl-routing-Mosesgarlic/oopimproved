import org.mindrot.jbcrypt.BCrypt;

public class GenerateHashes {
    public static void main(String[] args) {
        String password = "password123";
        
        System.out.println("Generating BCrypt hash for password: " + password);
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("Generated hash: " + hash);
        System.out.println("Verification: " + BCrypt.checkpw(password, hash));
    }
}
