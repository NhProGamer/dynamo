package fr.nhsoul.dynamo.velocity.config;

import fr.nhsoul.dynamo.common.config.NatsConfig;
import fr.nhsoul.dynamo.common.config.TopicsConfig;
import fr.nhsoul.dynamo.common.model.LoadBalancingStrategy;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VelocityConfigManager {
    private final Path dataDirectory;
    private final Logger logger;
    private Map<String, Object> config;
    
    public VelocityConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            // Créer le répertoire de données s'il n'existe pas
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            
            Path configFile = dataDirectory.resolve("config.yml");
            
            // Copier la configuration par défaut si elle n'existe pas
            if (!Files.exists(configFile)) {
                try (InputStream defaultConfig = getClass().getResourceAsStream("/config.yml")) {
                    if (defaultConfig != null) {
                        Files.copy(defaultConfig, configFile);
                        logger.info("Configuration par défaut créée.");
                    }
                }
            }
            
            // Charger la configuration
            if (Files.exists(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(Files.newInputStream(configFile));
                logger.info("Configuration chargée avec succès.");
            } else {
                config = new HashMap<>();
                logger.warn("Aucune configuration trouvée, utilisation des valeurs par défaut.");
            }
            
        } catch (IOException e) {
            logger.error("Erreur lors du chargement de la configuration", e);
            config = new HashMap<>();
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(String path, T defaultValue) {
        String[] parts = path.split("\\.");
        Object current = config;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return defaultValue;
            }
        }
        
        return current != null ? (T) current : defaultValue;
    }
    
    public NatsConfig getNatsConfig() {
        return new NatsConfig(
            getConfigValue("nats.url", "nats://localhost:4222"),
            getConfigValue("nats.connection-timeout", 5000),
            getConfigValue("nats.reconnect-timeout", 2000),
            getConfigValue("nats.max-reconnect-attempts", 10)
        );
    }
    
    public TopicsConfig getTopicsConfig() {
        return new TopicsConfig(
            getConfigValue("topics.register", "minecraft.server.register"),
            getConfigValue("topics.heartbeat", "minecraft.server.heartbeat"),
            getConfigValue("topics.unregister", "minecraft.server.unregister")
        );
    }
    
    public String getTopicsPattern() {
        return getConfigValue("topics.pattern", "minecraft.server.*");
    }
    
    public int getServerTimeout() {
        return getConfigValue("proxy.server-timeout", 30);
    }
    
    public int getCleanupInterval() {
        return getConfigValue("proxy.cleanup-interval", 15);
    }
    
    public Map<String, GroupConfig> getGroupsConfig() {
        Map<String, GroupConfig> groups = new HashMap<>();
        Map<String, Object> groupsData = getConfigValue("groups", new HashMap<>());
        
        for (Map.Entry<String, Object> entry : groupsData.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> groupData = (Map<String, Object>) entry.getValue();
                
                LoadBalancingStrategy strategy = LoadBalancingStrategy.valueOf(
                    ((String) groupData.getOrDefault("load-balancing", "ROUND_ROBIN")).toUpperCase()
                );
                
                String fallbackServer = (String) groupData.get("fallback-server");
                int priority = (Integer) groupData.getOrDefault("priority", 1);
                
                groups.put(entry.getKey(), new GroupConfig(strategy, fallbackServer, priority));
            }
        }
        
        return groups;
    }
    
    public String getDefaultGroup() {
        return getConfigValue("load-balancing.default-group", "lobby");
    }
    
    public boolean isAutoMigrate() {
        return getConfigValue("load-balancing.auto-migrate", true);
    }
    
    public int getMigrationThreshold() {
        return getConfigValue("load-balancing.migration-threshold", 90);
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    public static class GroupConfig {
        private final LoadBalancingStrategy strategy;
        private final String fallbackServer;
        private final int priority;
        
        public GroupConfig(LoadBalancingStrategy strategy, String fallbackServer, int priority) {
            this.strategy = strategy;
            this.fallbackServer = fallbackServer;
            this.priority = priority;
        }
        
        public LoadBalancingStrategy getStrategy() { return strategy; }
        public String getFallbackServer() { return fallbackServer; }
        public int getPriority() { return priority; }
    }
}

