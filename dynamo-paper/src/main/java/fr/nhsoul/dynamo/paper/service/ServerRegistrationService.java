package fr.nhsoul.dynamo.paper.service;

import fr.nhsoul.dynamo.common.model.ServerEvent;
import fr.nhsoul.dynamo.common.model.ServerInfo;
import fr.nhsoul.dynamo.paper.config.PaperConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicBoolean;

public class ServerRegistrationService implements Listener {
    private final JavaPlugin plugin;
    private final NatsService natsService;
    private final PaperConfigManager configManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private ServerInfo serverInfo;
    private BukkitTask heartbeatTask;
    
    public ServerRegistrationService(JavaPlugin plugin, NatsService natsService, PaperConfigManager configManager) {
        this.plugin = plugin;
        this.natsService = natsService;
        this.configManager = configManager;
        
        // Créer les informations du serveur
        this.serverInfo = new ServerInfo(
            configManager.getServerName(),
            configManager.getServerHost(),
            configManager.getServerPort(),
            configManager.getServerGroups()
        );
        
        // Enregistrer les événements
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void start() {
        if (running.compareAndSet(false, true)) {
            // Enregistrer le serveur au démarrage
            registerServer();
            
            // Démarrer le heartbeat
            startHeartbeat();
            
            plugin.getLogger().info("Service d'enregistrement du serveur démarré.");
        }
    }
    
    public void stop() {
        if (running.compareAndSet(true, false)) {
            // Arrêter le heartbeat
            if (heartbeatTask != null) {
                heartbeatTask.cancel();
            }
            
            // Désenregistrer le serveur
            unregisterServer();
            
            plugin.getLogger().info("Service d'enregistrement du serveur arrêté.");
        }
    }
    
    private void registerServer() {
        updateServerInfo();
        ServerEvent event = new ServerEvent(ServerEvent.EventType.REGISTER, serverInfo);
        natsService.publishEvent(configManager.getTopicsConfig().getRegister(), event);
        plugin.getLogger().info("Serveur enregistré: " + serverInfo.getName());
    }
    
    private void unregisterServer() {
        updateServerInfo();
        ServerEvent event = new ServerEvent(ServerEvent.EventType.UNREGISTER, serverInfo);
        natsService.publishEvent(configManager.getTopicsConfig().getUnregister(), event);
        plugin.getLogger().info("Serveur désenregistré: " + serverInfo.getName());
    }
    
    private void sendHeartbeat() {
        if (!running.get()) return;
        
        updateServerInfo();
        ServerEvent event = new ServerEvent(ServerEvent.EventType.HEARTBEAT, serverInfo);
        natsService.publishEvent(configManager.getTopicsConfig().getHeartbeat(), event);
    }
    
    private void startHeartbeat() {
        int interval = configManager.getHeartbeatInterval();
        heartbeatTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
            this::sendHeartbeat, 
            interval * 20L, // Délai initial en ticks
            interval * 20L  // Intervalle en ticks (20 ticks = 1 seconde)
        );
    }
    
    private void updateServerInfo() {
        serverInfo.setCurrentPlayers(Bukkit.getOnlinePlayers().size());
        serverInfo.setMaxPlayers(Bukkit.getMaxPlayers());
        serverInfo.updateTimestamp();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (running.get()) {
            updateServerInfo();
            ServerEvent serverEvent = new ServerEvent(ServerEvent.EventType.PLAYER_JOIN, serverInfo);
            natsService.publishEvent(configManager.getTopicsConfig().getHeartbeat(), serverEvent);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (running.get()) {
            // Attendre un tick pour que le joueur soit effectivement déconnecté
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateServerInfo();
                ServerEvent serverEvent = new ServerEvent(ServerEvent.EventType.PLAYER_LEAVE, serverInfo);
                natsService.publishEvent(configManager.getTopicsConfig().getHeartbeat(), serverEvent);
            }, 1L);
        }
    }
    
    public ServerInfo getServerInfo() {
        return serverInfo;
    }
}