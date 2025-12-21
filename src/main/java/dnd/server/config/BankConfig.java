package dnd.server.config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Configuration cho Bank settings (MB Bank accounts, HMAC secret)
 * Load từ file bank.properties
 */
public class BankConfig {
    private static final Logger logger = Logger.getLogger(BankConfig.class.getName());
    
    private static BankConfig instance;
    private Properties properties;
    
    private String hmacSecret;
    private List<BankAccount> accounts;
    
    private BankConfig() {
        loadProperties();
    }
    
    public static synchronized BankConfig getInstance() {
        if (instance == null) {
            instance = new BankConfig();
        }
        return instance;
    }
    
    /**
     * Load configuration từ bank.properties
     */
    private void loadProperties() {
        properties = new Properties();
        accounts = new ArrayList<>();
        
        // Thử load từ bank.properties trong thư mục hiện tại hoặc server/
        String[] paths = {"bank.properties", "../bank.properties", "server/bank.properties"};
        
        boolean loaded = false;
        for (String path : paths) {
            try (InputStream input = new java.io.FileInputStream(path)) {
                properties.load(input);
                loaded = true;
                logger.info("Loaded bank.properties from: " + path);
                break;
            } catch (java.io.IOException e) {
                // Tiếp tục thử path tiếp theo
            }
        }
        
        if (!loaded) {
            logger.warning("bank.properties not found, using defaults");
        }
        
        try {
            
            // Load HMAC secret
            hmacSecret = properties.getProperty("bank.hmac.secret", "");
            if (hmacSecret.isEmpty()) {
                // Fallback to environment variable
                hmacSecret = System.getenv("HMAC_SECRET") != null 
                    ? System.getenv("HMAC_SECRET")
                    : System.getProperty("hmac.secret", "");
            }
            
            // Load bank accounts
            // Format: bank.account.1.number=181816092003
            //         bank.account.1.name=NGUYEN DUC DUY
            //         bank.account.2.number=0386278203
            //         bank.account.2.name=TRAN VAN THUY
            int accountIndex = 1;
            while (true) {
                String number = properties.getProperty("bank.account." + accountIndex + ".number");
                String name = properties.getProperty("bank.account." + accountIndex + ".name");
                
                if (number == null || number.isEmpty() || name == null || name.isEmpty()) {
                    break;
                }
                
                accounts.add(new BankAccount(number.trim(), name.trim()));
                accountIndex++;
            }
            
            // Fallback to default accounts nếu không có trong properties
            if (accounts.isEmpty()) {
                logger.warning("No bank accounts found in bank.properties, using defaults");
                accounts.add(new BankAccount("181816092003", "NGUYEN DUC DUY"));
                accounts.add(new BankAccount("0386278203", "TRAN VAN THUY"));
            }
            
        } catch (Exception e) {
            logger.warning("Failed to load bank.properties: " + e.getMessage() + ", using defaults");
            // Use defaults
            hmacSecret = System.getenv("HMAC_SECRET") != null 
                ? System.getenv("HMAC_SECRET")
                : System.getProperty("hmac.secret", "");
            accounts.add(new BankAccount("181816092003", "NGUYEN DUC DUY"));
            accounts.add(new BankAccount("0386278203", "TRAN VAN THUY"));
        }
    }
    
    public String getHmacSecret() {
        return hmacSecret;
    }
    
    public List<BankAccount> getAccounts() {
        return new ArrayList<>(accounts);
    }
    
    /**
     * Bank account info
     */
    public static class BankAccount {
        private final String number;
        private final String name;
        
        public BankAccount(String number, String name) {
            this.number = number;
            this.name = name;
        }
        
        public String getNumber() {
            return number;
        }
        
        public String getName() {
            return name;
        }
    }
}

