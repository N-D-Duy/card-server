package dnd.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session manager để lưu trữ tạm thời dữ liệu authentication giữa các bước
 * Singleton để share giữa các endpoints
 */
public class SessionManager {
    private static SessionManager instance;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 phút
    
    private SessionManager() {}
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void putSession(String sessionId, AuthSession session) {
        sessions.put(sessionId, session);
    }
    
    public AuthSession getSession(String sessionId) {
        AuthSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        
        // Kiểm tra timeout
        if (System.currentTimeMillis() - session.createdAt > SESSION_TIMEOUT) {
            sessions.remove(sessionId);
            return null;
        }
        
        return session;
    }
    
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    public static class AuthSession {
        public final String cardId;
        public final byte[] staticKey;
        public final byte[] publicKeyBytes;
        public final byte[] challengeServer;
        public final long createdAt;
        
        public AuthSession(String cardId, byte[] staticKey, byte[] publicKeyBytes, byte[] challengeServer) {
            this.cardId = cardId;
            this.staticKey = staticKey;
            this.publicKeyBytes = publicKeyBytes;
            this.challengeServer = challengeServer;
            this.createdAt = System.currentTimeMillis();
        }
    }
}

