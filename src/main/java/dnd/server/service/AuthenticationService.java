package dnd.server.service;

import dnd.server.crypto.CryptoUtils;
import dnd.server.db.CardKeyDao;
import dnd.server.db.CardSessionDao;
import dnd.server.util.HexUtils;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

/**
 * Authentication service trên server
 * Xử lý toàn bộ logic crypto và authentication
 */
public class AuthenticationService {
    private final CardKeyDao cardKeyDao;
    private final CardSessionDao cardSessionDao;
    
    // Master key để giải mã static keys từ DB
    // TODO: Load từ HSM hoặc secure key management system
    private static final byte[] MASTER_KEY = new byte[32];
    
    static {
        // Placeholder - production cần load từ HSM
        java.util.Arrays.fill(MASTER_KEY, (byte) 0x42);
    }
    
    private static final Duration SESSION_TTL = Duration.ofMinutes(15);
    
    public AuthenticationService(CardKeyDao cardKeyDao, CardSessionDao cardSessionDao) {
        this.cardKeyDao = cardKeyDao;
        this.cardSessionDao = cardSessionDao;
    }
    
    /**
     * Bước 1: Bắt đầu authentication
     * - Tìm thẻ trong DB
     * - Lấy public key và static key
     * - Tạo challenge từ server
     */
    public AuthStartResult startAuthentication(String cardId) {
        // Tìm thẻ trong database
        HashMap<String, Object> cardInfo = cardKeyDao.findByCardId(cardId);
        if (cardInfo == null) {
            throw new RuntimeException("Card not found: " + cardId);
        }
        
        // Lấy public key và static key
        byte[] publicKeyBytes = (byte[]) cardInfo.get("public_key_rsa");
        byte[][] encryptedStaticKey = cardKeyDao.getEncryptedStaticKey(cardId);
        if (encryptedStaticKey == null) {
            throw new RuntimeException("Static key not found for card: " + cardId);
        }
        
        // Giải mã static key
        byte[] staticKey = CryptoUtils.decryptStaticKey(
            encryptedStaticKey[0], 
            encryptedStaticKey[1], 
            MASTER_KEY
        );
        
        // Tạo challenge từ server
        byte[] challengeServer = CryptoUtils.generateChallenge();
        
        return new AuthStartResult(challengeServer, publicKeyBytes, staticKey);
    }
    
    /**
     * Bước 2: Verify signature và tính cryptogram
     */
    public AuthVerifyResult verifySignature(byte[] publicKeyBytes, byte[] challengeServer, 
                                           byte[] signature, byte[] challengeCard) {
        // Verify chữ ký RSA
        boolean signatureValid = CryptoUtils.verifyRSASignature(
            publicKeyBytes, 
            challengeServer, 
            signature
        );
        
        if (!signatureValid) {
            throw new RuntimeException("Invalid RSA signature - authentication failed");
        }
        
        // Tính cryptogram từ server (cần static key, sẽ được lưu tạm trong session)
        // Cryptogram sẽ được tính ở bước complete
        
        return new AuthVerifyResult(true);
    }
    
    /**
     * Bước 3: Hoàn tất authentication
     * - Tính cryptogram
     * - Derive session keys
     * - Lưu session vào DB
     */
    public AuthCompleteResult completeAuthentication(String cardId, byte[] staticKey, 
                                                     byte[] challengeServer, byte[] challengeCard) {
        // Tính cryptogram từ server
        byte[] cryptogram = CryptoUtils.computeServerCryptogram(staticKey, challengeCard);
        
        // Thiết lập session keys
        byte[][] sessionKeys = CryptoUtils.deriveSessionKeys(staticKey, challengeServer, challengeCard);
        byte[] sessionEncKey = sessionKeys[0];
        byte[] sessionMacKey = sessionKeys[1];
        
        // Tạo session ID
        String sessionId = generateSessionIdHex64();
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(SESSION_TTL));
        
        // Lưu session vào DB
        cardSessionDao.insertSession(cardId, sessionId, challengeServer, challengeCard, expiresAt);
        
        // Cập nhật last_auth_at
        cardKeyDao.updateLastAuth(cardId);
        
        return new AuthCompleteResult(cryptogram, sessionId, sessionEncKey, sessionMacKey);
    }
    
    private String generateSessionIdHex64() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(64);
        for (byte value : b) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }
    
    // Result classes
    public static class AuthStartResult {
        public final byte[] challengeServer;
        public final byte[] publicKeyBytes;
        public final byte[] staticKey; // Tạm thời, sẽ được lưu trong session
        
        public AuthStartResult(byte[] challengeServer, byte[] publicKeyBytes, byte[] staticKey) {
            this.challengeServer = challengeServer;
            this.publicKeyBytes = publicKeyBytes;
            this.staticKey = staticKey;
        }
    }
    
    public static class AuthVerifyResult {
        public final boolean valid;
        
        public AuthVerifyResult(boolean valid) {
            this.valid = valid;
        }
    }
    
    public static class AuthCompleteResult {
        public final byte[] cryptogram;
        public final String sessionId;
        public final byte[] sessionEncKey;
        public final byte[] sessionMacKey;
        
        public AuthCompleteResult(byte[] cryptogram, String sessionId, 
                                 byte[] sessionEncKey, byte[] sessionMacKey) {
            this.cryptogram = cryptogram;
            this.sessionId = sessionId;
            this.sessionEncKey = sessionEncKey;
            this.sessionMacKey = sessionMacKey;
        }
    }
}

