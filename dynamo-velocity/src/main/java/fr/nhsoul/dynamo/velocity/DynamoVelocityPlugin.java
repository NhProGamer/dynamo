package fr.nhsoul.dynamo.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.nhsoul.dynamo.velocity.config.VelocityConfigManager;
import fr.nhsoul.dynamo.velocity.service.NatsService;
import fr.nhsoul.dynamo.velocity.service.ServerDiscoveryService;
import fr.nhsoul.dynamo.velocity.service.LoadBalancingService;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "dynamo-velocity",
    name = "Dynamo Velocity",
    version = "1.0.0",
    authors = {"Dynamo"}
)
public class DynamoVelocityPlugin {
    
    @Inject
    private Logger logger;
    
    @Inject
    private ProxyServer server;
    
    @Inject
    @DataDirectory
    private Path dataDirectory;
    
    private VelocityConfigManager configManager;
    private NatsService natsService;
    private ServerDiscoveryService discoveryService;
    private LoadBalancingService loadBalancingService;
    private VelocityEventRegistrar eventRegistrar;
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initialisation du plugin Dynamo Velocity...");
        
        // Initialiser le gestionnaire de configuration
        configManager = new VelocityConfigManager(dataDirectory, logger);
        
        // Initialiser le service NATS
        natsService = new NatsService(configManager, logger);
        
        // Initialiser le service de découverte de serveurs
        discoveryService = new ServerDiscoveryService(server, natsService, configManager, logger);
        
        // Initialiser le service de load balancing
        loadBalancingService = new LoadBalancingService(discoveryService, configManager, logger);
        
        // Initialiser le registrar d'événements
        eventRegistrar = new VelocityEventRegistrar(this, server, logger);
        
        // Démarrer les services
        if (natsService.start()) {
            discoveryService.start();
            loadBalancingService.start();
            
            // Enregistrer les événements et commandes
            eventRegistrar.registerEvents();
            eventRegistrar.registerCommands();
            
            logger.info("Plugin Dynamo Velocity initialisé avec succès !");
        } else {
            logger.error("Impossible de se connecter à NATS. Le plugin ne fonctionnera pas correctement.");
        }
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("Arrêt du plugin Dynamo Velocity...");
        
        // Arrêter les services dans l'ordre inverse
        if (loadBalancingService != null) {
            loadBalancingService.stop();
        }
        
        if (discoveryService != null) {
            discoveryService.stop();
        }
        
        if (natsService != null) {
            natsService.stop();
        }
        
        logger.info("Plugin Dynamo Velocity arrêté.");
    }
    
    public VelocityConfigManager getConfigManager() {
        return configManager;
    }
    
    public NatsService getNatsService() {
        return natsService;
    }
    
    public ServerDiscoveryService getDiscoveryService() {
        return discoveryService;
    }
    
    public LoadBalancingService getLoadBalancingService() {
        return loadBalancingService;
    }

    public ProxyServer getProxy() {
        return this.server;
    }


}

