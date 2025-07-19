package fr.nhsoul.dynamo.paper.config;

import fr.nhsoul.dynamo.common.config.NatsConfig;
import fr.nhsoul.dynamo.common.config.TopicsConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PaperConfigManager {
    private final JavaPlugin plugin;
    private final FileConfiguration config;
    
    public PaperConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Sauvegarder la configuration par défaut
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    public NatsConfig getNatsConfig() {
        return new NatsConfig(
            config.getString("nats.url", "nats://localhost:4222"),
            config.getInt("nats.connection-timeout", 5000),
            config.getInt("nats.reconnect-timeout", 2000),
            config.getInt("nats.max-reconnect-attempts", 10)
        );
    }
    
    public TopicsConfig getTopicsConfig() {
        return new TopicsConfig(
            config.getString("topics.register", "minecraft.server.register"),
            config.getString("topics.heartbeat", "minecraft.server.heartbeat"),
            config.getString("topics.unregister", "minecraft.server.unregister")
        );
    }
    
    public String getServerName() {
        return config.getString("server.name", "paper-server-" + System.currentTimeMillis());
    }
    
    public List<String> getServerGroups() {
        return config.getStringList("server.groups");
    }
    
    public int getServerPort() {
        Integer configPort = config.getInt("server.port");
        if (configPort == 0) {
            return plugin.getServer().getPort();
        }
        return config.getInt("server.port", 25565);
    }
    
    public String getServerHost() {
        String configHost = config.getString("server.host", "");
        if (configHost.isEmpty()) {
            return plugin.getServer().getIp();
        }
        return configHost;
    }
    
    public int getHeartbeatInterval() {
        return config.getInt("server.heartbeat-interval", 10);
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
    }
}

