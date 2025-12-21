package dnd.server.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class cho HMAC-SHA256 operations
 * Tương thích với Node.js crypto.createHmac("sha256", secret).update(payload, "utf8").digest("base64")
 */
public class HmacUtils {
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Tính HMAC-SHA256 và trả về Base64 (NO_WRAP)
     * @param secret Secret key
     * @param payload Payload string
     * @return Base64 encoded HMAC
     */
    public static String hmacSha256Base64(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }

    /**
     * Timing-safe string comparison để tránh timing attacks
     * @param a First string
     * @param b Second string
     * @return true if strings are equal
     */
    public static boolean timingSafeEqual(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}

