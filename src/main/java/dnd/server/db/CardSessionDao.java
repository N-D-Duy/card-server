package dnd.server.db;

import java.sql.Timestamp;

/**
 * DAO cho card_sessions table trên server
 */
public class CardSessionDao {
    private final DbManager dbManager;
    
    public CardSessionDao(DbManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public Long insertSession(String cardId, String sessionId, byte[] challengeServer, 
                              byte[] challengeCard, Timestamp expiresAt) {
        String sql = "INSERT INTO card_sessions (card_id, session_id, challenge_server, challenge_card, expires_at) VALUES (?, ?, ?, ?, ?)";
        return dbManager.insertAndGetId(sql, cardId, sessionId, challengeServer, challengeCard, expiresAt);
    }
}

