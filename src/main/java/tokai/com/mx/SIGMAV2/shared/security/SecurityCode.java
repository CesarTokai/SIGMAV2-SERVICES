package tokai.com.mx.SIGMAV2.shared.security;
import java.security.SecureRandom;

public class SecurityCode {
    public static String generate() {
        return String.valueOf(new SecureRandom().nextInt(999999 - 100000) + 100000);
    }

    public static String generateAlphanumeric() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";
        String alphaNumeric = upperCase + lowerCase + numbers;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = new SecureRandom().nextInt(alphaNumeric.length());
            sb.append(alphaNumeric.charAt(index));
        }
        return sb.toString();
    }
}
