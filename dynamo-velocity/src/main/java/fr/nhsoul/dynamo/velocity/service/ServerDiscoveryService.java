// ========== ServerDiscoveryService.java ==========
package fr.nhsoul.dynamo.velocity.service;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import fr.nhsoul.dynamo.common.model.ServerEvent;
import fr.nhsoul.dynamo.velocity.config.VelocityConfigManager;
import io.nats.client.Dispatcher;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerDiscoveryService {
    private final ProxyServer proxyServer;
    private final NatsService natsService;
    private final VelocityConfigManager configManager;
    private final Logger logger;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private final Map<String, fr.nhsoul.dynamo.common.model.ServerInfo> discoveredServers = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private Dispatcher natsDispatcher;
    
    public ServerDiscoveryService(ProxyServer proxyServer, NatsService natsService, 
                                VelocityConfigManager configManager, Logger logger) {
        this.proxyServer = proxyServer;
        this.natsService = natsService;
        this.configManager = configManager;
        this.logger = logger;
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newScheduledThreadPool(2);
            
            // S'abonner aux événements de serveur
            subscribeToServerEvents();
            
            // Démarrer la tâche de nettoyage
            startCleanupTask();
            
            logger.info("Service de découverte de serveurs démarré.");
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (natsDispatcher != null) {
                String pattern = configManager.getTopicsPattern();
                natsDispatcher.unsubscribe(pattern);
            }
            
            if (scheduler != null) {
                scheduler.shutdown();
            }
            
            logger.info("Service de découverte de serveurs arrêté.");
        }
    }
    
    private void subscribeToServerEvents() {
        String pattern = configManager.getTopicsPattern();
        
        natsDispatcher = natsService.subscribe(pattern, this::handleServerEvent);
        logger.info("Abonnement aux événements serveur sur le pattern: {}", pattern);
    }
    
    private void handleServerEvent(ServerEvent event) {
        if (!running.get()) return;
        
        fr.nhsoul.dynamo.common.model.ServerInfo serverInfo = event.getServerInfo();
        String serverName = serverInfo.getName();
        
        switch (event.getType()) {
            case REGISTER:
                handleServerRegister(serverInfo);
                break;
                
            case HEARTBEAT:
                handleServerHeartbeat(serverInfo);
                break;
                
            case UNREGISTER:
                handleServerUnregister(serverName);
                break;
                
            case PLAYER_JOIN:
            case PLAYER_LEAVE:
                handlePlayerCountUpdate(serverInfo);
                break;
                
            default:
                logger.warn("Type d'événement serveur non géré: {}", event.getType());
        }
    }
    
    private void handleServerRegister(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        String serverName = serverInfo.getName();
        
        // Ajouter ou mettre à jour le serveur découvert
        discoveredServers.put(serverName, serverInfo);
        
        // Enregistrer le serveur dans Velocity
        registerServerInVelocity(serverInfo);
        
        logger.info("Serveur enregistré: {} ({}:{})", serverName, serverInfo.getHost(), serverInfo.getPort());
    }
    
    private void handleServerHeartbeat(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        String serverName = serverInfo.getName();
        
        // Mettre à jour les informations du serveur
        discoveredServers.put(serverName, serverInfo);
        
        // S'assurer que le serveur est enregistré dans Velocity
        if (!proxyServer.getServer(serverName).isPresent()) {
            registerServerInVelocity(serverInfo);
        }
    }
    
    private void handleServerUnregister(String serverName) {
        // Supprimer le serveur de nos données
        discoveredServers.remove(serverName);
        
        // Supprimer le serveur de Velocity
        proxyServer.getServer(serverName).ifPresent(server -> {
            proxyServer.unregisterServer(server.getServerInfo());
            logger.info("Serveur désenregistré: {}", serverName);
        });
    }
    
    private void handlePlayerCountUpdate(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        String serverName = serverInfo.getName();
        
        // Mettre à jour les informations du serveur
        discoveredServers.put(serverName, serverInfo);
        
        logger.debug("Nombre de joueurs mis à jour pour {}: {}/{}", 
                    serverName, serverInfo.getCurrentPlayers(), serverInfo.getMaxPlayers());
    }
    
    private void registerServerInVelocity(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        try {
            InetSocketAddress address = new InetSocketAddress(serverInfo.getHost(), serverInfo.getPort());
            ServerInfo velocityServerInfo = new ServerInfo(serverInfo.getName(), address);
            
            RegisteredServer registeredServer = proxyServer.createRawRegisteredServer(velocityServerInfo);
            proxyServer.registerServer(velocityServerInfo);
            
            logger.debug("Serveur {} enregistré dans Velocity", serverInfo.getName());
        } catch (Exception e) {
            logger.error("Erreur lors de l'enregistrement du serveur {} dans Velocity", serverInfo.getName(), e);
        }
    }
    
    private void startCleanupTask() {
        int cleanupInterval = configManager.getCleanupInterval();
        int serverTimeout = configManager.getServerTimeout();
        
        scheduler.scheduleWithFixedDelay(() -> {
            if (!running.get()) return;
            
            long timeoutMs = serverTimeout * 1000L;
            
            // Identifier les serveurs expirés
            discoveredServers.entrySet().removeIf(entry -> {
                fr.nhsoul.dynamo.common.model.ServerInfo serverInfo = entry.getValue();
                
                if (serverInfo.isTimedOut(timeoutMs)) {
                    String serverName = entry.getKey();
                    
                    // Supprimer le serveur de Velocity
                    proxyServer.getServer(serverName).ifPresent(server -> {
                        proxyServer.unregisterServer(server.getServerInfo());
                        logger.info("Serveur expiré supprimé: {}", serverName);
                    });
                    
                    return true;
                }
                return false;
            });
            
        }, cleanupInterval, cleanupInterval, TimeUnit.SECONDS);
    }
    
    public Map<String, fr.nhsoul.dynamo.common.model.ServerInfo> getDiscoveredServers() {
        return new ConcurrentHashMap<>(discoveredServers);
    }
    
    public fr.nhsoul.dynamo.common.model.ServerInfo getServerInfo(String serverName) {
        return discoveredServers.get(serverName);
    }
}

