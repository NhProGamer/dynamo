package fr.nhsoul.dynamo.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.nhsoul.dynamo.velocity.command.DynamoCommand;
import fr.nhsoul.dynamo.velocity.handler.PlayerConnectionHandler;
import fr.nhsoul.dynamo.velocity.handler.ServerPingHandler;
import org.slf4j.Logger;

public class VelocityEventRegistrar {
    private final DynamoVelocityPlugin plugin;
    private final ProxyServer proxyServer;
    private final Logger logger;
    
    public VelocityEventRegistrar(DynamoVelocityPlugin plugin, ProxyServer proxyServer, Logger logger) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        this.logger = logger;
    }
    
    public void registerEvents() {
        EventManager eventManager = proxyServer.getEventManager();
        
        // Enregistrer les handlers d'événements
        eventManager.register(plugin, new PlayerConnectionHandler(plugin, logger));
        eventManager.register(plugin, new ServerPingHandler(plugin));
        
        logger.info("Handlers d'événements enregistrés.");
    }
    
    public void registerCommands() {
        CommandManager commandManager = proxyServer.getCommandManager();
        
        // Enregistrer la commande dynamo
        commandManager.register("dynamo", new DynamoCommand(plugin));
        
        logger.info("Commandes enregistrées.");
    }
}