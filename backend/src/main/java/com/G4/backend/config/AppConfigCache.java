package com.G4.backend.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * PATTERN: Singleton (Creational)
 * 
 * PROBLEM SOLVED:
 * Application configuration and cached data need to be accessed from multiple places
 * without creating new instances each time. Without Singleton, we'd have redundant
 * objects consuming memory and potential inconsistency issues.
 * 
 * HOW IT WORKS:
 * Private constructor prevents external instantiation.
 * Static instance ensures only one object exists in the JVM.
 * Global access point through getInstance() method.
 * Thread-safe implementation using synchronized block.
 * 
 * REAL-WORLD EXAMPLE:
 * Database connection pools, logging services, configuration managers,
 * and cache managers commonly use Singleton pattern.
 * Runtime.getRuntime() in Java is a classic Singleton example.
 * 
 * USE CASE IN THIS PROJECT:
 * AppConfigCache stores application-wide configuration and cached data
 * that should be shared across all components without duplication.
 * 
 * NOTE: Spring's @Component already creates a singleton bean by default,
 * but this demonstrates the explicit Singleton pattern implementation.
 */
@Component
public class AppConfigCache {
    
    // Singleton instance (Spring manages this as singleton via @Component)
    private static volatile AppConfigCache instance;
    
    // Configuration cache
    private final Map<String, Object> configCache = new HashMap<>();
    
    // Private constructor (enforced by Spring's dependency injection)
    private AppConfigCache() {
        // Initialize with default configurations
        initializeDefaults();
    }
    
    /**
     * Get singleton instance (for non-Spring usage)
     * @return The singleton instance
     */
    public static AppConfigCache getInstance() {
        if (instance == null) {
            synchronized (AppConfigCache.class) {
                if (instance == null) {
                    instance = new AppConfigCache();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize default configuration values
     */
    private void initializeDefaults() {
        configCache.put("maxUploadSize", 5242880); // 5MB
        configCache.put("allowedRoles", new String[]{"client", "technician", "admin"});
        configCache.put("sessionTimeout", 1800000); // 30 minutes
        configCache.put("maxLoginAttempts", 5);
    }
    
    /**
     * Get configuration value
     * @param key Configuration key
     * @return Configuration value
     */
    public Object getConfig(String key) {
        return configCache.get(key);
    }
    
    /**
     * Set configuration value
     * @param key Configuration key
     * @param value Configuration value
     */
    public void setConfig(String key, Object value) {
        configCache.put(key, value);
    }
    
    /**
     * Get integer configuration value
     * @param key Configuration key
     * @return Integer value
     */
    public Integer getIntConfig(String key) {
        Object value = configCache.get(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    /**
     * Clear all cached configurations
     */
    public void clearCache() {
        configCache.clear();
        initializeDefaults();
    }
}
