package dnd.server.crypto;

import dnd.server.util.HexUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * Cryptographic utilities cho server
 * Copy từ dnd.crypto.CryptoUtils
 */
public class CryptoUtils {
    
    private static final String PROVIDER = "BC";
    private static final int AES_KEY_SIZE = 32;
    private static final int IV_SIZE = 16;
    private static final int CHALLENGE_SIZE = 32;
    
    static {
        if (Security.getProvider(PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Giải mã static key từ database
     */
    public static byte[] decryptStaticKey(byte[] encryptedKey, byte[] iv, byte[] masterKey) {
        if (encryptedKey == null || iv == null || masterKey == null) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        if (masterKey.length != AES_KEY_SIZE || iv.length != IV_SIZE) {
            throw new IllegalArgumentException("Invalid key or IV size");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(masterKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            return cipher.doFinal(encryptedKey);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt static key", e);
        }
    }
    
    /**
     * Tạo challenge ngẫu nhiên (32 bytes)
     */
    public static byte[] generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[CHALLENGE_SIZE];
        random.nextBytes(challenge);
        return challenge;
    }
    
    /**
     * Tính cryptogram từ server để xác minh
     * Cryptogram = HMAC-SHA256(K_static, Challenge_Card)
     */
    public static byte[] computeServerCryptogram(byte[] staticKey, byte[] challengeCard) {
        if (staticKey == null || staticKey.length != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Static key must be 32 bytes");
        }
        if (challengeCard == null || challengeCard.length != CHALLENGE_SIZE) {
            throw new IllegalArgumentException("Challenge must be 32 bytes");
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256", PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(staticKey, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(challengeCard);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute server cryptogram", e);
        }
    }
    
    /**
     * Xác minh chữ ký RSA
     */
    public static boolean verifyRSASignature(byte[] publicKeyBytes, byte[] data, byte[] signature) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", PROVIDER);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
            
            Signature sig = Signature.getInstance("SHA1withRSA", PROVIDER);
            sig.initVerify(publicKey);
            sig.update(data);
            
            return sig.verify(signature);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify RSA signature", e);
        }
    }
    
    /**
     * Derive session keys từ static key và challenges
     */
    public static byte[][] deriveSessionKeys(byte[] staticKey, byte[] challengeServer, byte[] challengeCard) {
        return KeyDerivationFunction.deriveSessionKeys(staticKey, challengeServer, challengeCard);
    }
    
    /**
     * Giải mã dữ liệu với session encryption key và IV riêng
     */
    public static byte[] decryptWithSession(byte[] sessionEncKey, byte[] iv, byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Invalid ciphertext");
        }
        if (sessionEncKey == null || sessionEncKey.length != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Invalid session key");
        }
        if (iv == null || iv.length != IV_SIZE) {
            throw new IllegalArgumentException("Invalid IV");
        }
        
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding", PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(sessionEncKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            return cipher.doFinal(ciphertext);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt with session key", e);
        }
    }
    
    /**
     * Tính MAC với session MAC key
     */
    public static byte[] computeMAC(byte[] data, byte[] sessionMacKey) {
        if (data == null || sessionMacKey == null || sessionMacKey.length != AES_KEY_SIZE) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256", PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(sessionMacKey, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(data);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute MAC", e);
        }
    }
}

