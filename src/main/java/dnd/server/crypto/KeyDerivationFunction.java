package dnd.server.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Key Derivation Function utilities cho server
 */
public class KeyDerivationFunction {
    
    /**
     * HKDF (HMAC-based Key Derivation Function)
     */
    public static byte[] hkdf(byte[] ikm, byte[] salt, byte[] info, int keyLength) {
        if (ikm == null || keyLength <= 0) {
            throw new IllegalArgumentException("Invalid parameters for HKDF");
        }
        
        try {
            byte[] prk;
            if (salt == null || salt.length == 0) {
                salt = new byte[32];
            }
            prk = hmacSha256(salt, ikm);
            
            return hkdfExpand(prk, info, keyLength);
            
        } catch (Exception e) {
            throw new RuntimeException("HKDF derivation failed", e);
        }
    }
    
    private static byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws Exception {
        if (length > 255 * 32) {
            throw new IllegalArgumentException("HKDF output length too large");
        }
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(prk, "HmacSHA256");
        mac.init(keySpec);
        
        int n = (length + 31) / 32;
        byte[] okm = new byte[length];
        byte[] t = new byte[0];
        
        for (int i = 0; i < n; i++) {
            mac.update(t);
            if (info != null) {
                mac.update(info);
            }
            mac.update((byte) (i + 1));
            t = mac.doFinal();
            
            int offset = i * 32;
            int copyLength = Math.min(32, length - offset);
            System.arraycopy(t, 0, okm, offset, copyLength);
        }
        
        return okm;
    }
    
    private static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(data);
    }
    
    /**
     * Derive session keys từ static key và challenges
     */
    public static byte[][] deriveSessionKeys(byte[] staticKey, byte[] challengeServer, byte[] challengeCard) {
        if (staticKey == null || staticKey.length != 32) {
            throw new IllegalArgumentException("Static key must be 32 bytes (AES-256)");
        }
        if (challengeServer == null || challengeCard == null) {
            throw new IllegalArgumentException("Challenges cannot be null");
        }
        
        byte[] combinedChallenges = new byte[challengeServer.length + challengeCard.length];
        System.arraycopy(challengeServer, 0, combinedChallenges, 0, challengeServer.length);
        System.arraycopy(challengeCard, 0, combinedChallenges, challengeServer.length, challengeCard.length);
        
        byte[] encKey = hkdf32(staticKey, combinedChallenges, new byte[]{'E','N','C',0x01});
        byte[] macKey = hkdf32(staticKey, combinedChallenges, new byte[]{'M','A','C',0x01});
        
        return new byte[][] { encKey, macKey };
    }
    
    private static byte[] hkdf32(byte[] ikm, byte[] salt, byte[] info4) {
        try {
            byte[] prk = hmacSha256(salt, ikm);
            return hmacSha256(prk, info4);
        } catch (Exception e) {
            throw new RuntimeException("HKDF32 derivation failed", e);
        }
    }
}

