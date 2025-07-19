package fr.nhsoul.dynamo.paper;

import fr.nhsoul.dynamo.paper.config.PaperConfigManager;
import fr.nhsoul.dynamo.paper.service.NatsService;
import fr.nhsoul.dynamo.paper.service.ServerRegistrationService;
import org.bukkit.plugin.java.JavaPlugin;

public class DynamoPaperPlugin extends JavaPlugin {
    
    private PaperConfigManager configManager;
    private NatsService natsService;
    private ServerRegistrationService registrationService;
    
    @Override
    public void onEnable() {
        getLogger().info("Activation du plugin Dynamo Paper...");
        
        // Initialiser le gestionnaire de configuration
        configManager = new PaperConfigManager(this);
        
        // Initialiser le service NATS
        natsService = new NatsService(configManager, getLogger());
        
        // Initialiser le service d'enregistrement
        registrationService = new ServerRegistrationService(this, natsService, configManager);
        
        // Démarrer les services
        if (natsService.start()) {
            registrationService.start();
            getLogger().info("Plugin Dynamo Paper activé avec succès !");
        } else {
            getLogger().severe("Impossible de se connecter à NATS. Désactivation du plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Désactivation du plugin Dynamo Paper...");
        
        // Arrêter les services dans l'ordre inverse
        if (registrationService != null) {
            registrationService.stop();
        }
        
        if (natsService != null) {
            natsService.stop();
        }
        
        getLogger().info("Plugin Dynamo Paper désactivé.");
    }
    
    public PaperConfigManager getConfigManager() {
        return configManager;
    }
    
    public NatsService getNatsService() {
        return natsService;
    }
    
    public ServerRegistrationService getRegistrationService() {
        return registrationService;
    }
}

