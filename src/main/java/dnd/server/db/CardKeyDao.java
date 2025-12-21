package dnd.server.db;

import java.util.HashMap;

/**
 * DAO cho card_keys table trên server
 */
public class CardKeyDao {
    private final DbManager dbManager;
    
    public CardKeyDao(DbManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public HashMap<String, Object> findByCardId(String cardId) {
        String sql = "SELECT * FROM card_keys WHERE card_id = ? AND status = 1";
        return dbManager.queryOne(sql, cardId);
    }
    
    public byte[][] getEncryptedStaticKey(String cardId) {
        HashMap<String, Object> card = findByCardId(cardId);
        if (card != null && card.containsKey("static_key_encrypted") && 
            card.containsKey("static_key_iv")) {
            byte[] encrypted = (byte[]) card.get("static_key_encrypted");
            byte[] iv = (byte[]) card.get("static_key_iv");
            return new byte[][] { encrypted, iv };
        }
        return null;
    }
    
    public int updateLastAuth(String cardId) {
        String sql = "UPDATE card_keys SET last_auth_at = CURRENT_TIMESTAMP WHERE card_id = ?";
        return dbManager.update(sql, cardId);
    }
}

