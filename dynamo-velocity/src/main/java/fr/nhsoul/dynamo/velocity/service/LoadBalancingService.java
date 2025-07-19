// ========== LoadBalancingService.java ==========
package fr.nhsoul.dynamo.velocity.service;

import fr.nhsoul.dynamo.common.model.LoadBalancingStrategy;
import fr.nhsoul.dynamo.velocity.config.VelocityConfigManager;
import fr.nhsoul.dynamo.velocity.config.VelocityConfigManager.GroupConfig;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LoadBalancingService {
    private final ServerDiscoveryService discoveryService;
    private final VelocityConfigManager configManager;
    private final Logger logger;
    
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    
    public LoadBalancingService(ServerDiscoveryService discoveryService, 
                              VelocityConfigManager configManager, Logger logger) {
        this.discoveryService = discoveryService;
        this.configManager = configManager;
        this.logger = logger;
    }
    
    public void start() {
        running = true;
        logger.info("Service de load balancing démarré.");
    }
    
    public void stop() {
        running = false;
        logger.info("Service de load balancing arrêté.");
    }
    
    public Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectServer(String groupName) {
        if (!running) {
            return Optional.empty();
        }
        
        // Obtenir les serveurs du groupe
        List<fr.nhsoul.dynamo.common.model.ServerInfo> groupServers = getServersInGroup(groupName);
        
        if (groupServers.isEmpty()) {
            logger.warn("Aucun serveur disponible dans le groupe: {}", groupName);
            return Optional.empty();
        }
        
        // Obtenir la configuration du groupe
        GroupConfig groupConfig = configManager.getGroupsConfig().get(groupName);
        LoadBalancingStrategy strategy = groupConfig != null ? 
            groupConfig.getStrategy() : LoadBalancingStrategy.ROUND_ROBIN;
        
        // Sélectionner un serveur selon la stratégie
        return selectServerByStrategy(groupServers, strategy, groupName);
    }
    
    public Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectDefaultServer() {
        String defaultGroup = configManager.getDefaultGroup();
        return selectServer(defaultGroup);
    }
    
    public List<fr.nhsoul.dynamo.common.model.ServerInfo> getServersInGroup(String groupName) {
        return discoveryService.getDiscoveredServers().values().stream()
            .filter(serverInfo -> serverInfo.getGroups().contains(groupName))
            .filter(this::isServerHealthy)
            .collect(Collectors.toList());
    }
    
    public Map<String, List<fr.nhsoul.dynamo.common.model.ServerInfo>> getServersByGroup() {
        Map<String, List<fr.nhsoul.dynamo.common.model.ServerInfo>> result = new HashMap<>();
        
        for (fr.nhsoul.dynamo.common.model.ServerInfo serverInfo : discoveryService.getDiscoveredServers().values()) {
            if (isServerHealthy(serverInfo)) {
                for (String group : serverInfo.getGroups()) {
                    result.computeIfAbsent(group, k -> new ArrayList<>()).add(serverInfo);
                }
            }
        }
        
        return result;
    }
    
    private Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectServerByStrategy(
            List<fr.nhsoul.dynamo.common.model.ServerInfo> servers, 
            LoadBalancingStrategy strategy, 
            String groupName) {
        
        switch (strategy) {
            case ROUND_ROBIN:
                return selectRoundRobin(servers, groupName);
                
            case LEAST_PLAYERS:
                return selectLeastPlayers(servers);
                
            case RANDOM:
                return selectRandom(servers);
                
            case FIRST_AVAILABLE:
                return servers.stream().findFirst();
                
            default:
                logger.warn("Stratégie de load balancing non reconnue: {}", strategy);
                return selectRoundRobin(servers, groupName);
        }
    }
    
    private Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectRoundRobin(
            List<fr.nhsoul.dynamo.common.model.ServerInfo> servers, String groupName) {
        
        if (servers.isEmpty()) return Optional.empty();
        
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(groupName, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % servers.size();
        
        return Optional.of(servers.get(index));
    }
    
    private Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectLeastPlayers(
            List<fr.nhsoul.dynamo.common.model.ServerInfo> servers) {
        
        return servers.stream()
            .min(Comparator.comparingDouble(this::getPlayerRatio));
    }
    
    private Optional<fr.nhsoul.dynamo.common.model.ServerInfo> selectRandom(
            List<fr.nhsoul.dynamo.common.model.ServerInfo> servers) {
        
        if (servers.isEmpty()) return Optional.empty();
        
        int randomIndex = ThreadLocalRandom.current().nextInt(servers.size());
        return Optional.of(servers.get(randomIndex));
    }
    
    private boolean isServerHealthy(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        // Vérifier si le serveur n'est pas expiré
        long timeoutMs = configManager.getServerTimeout() * 1000L;
        
        if (serverInfo.isTimedOut(timeoutMs)) {
            return false;
        }
        
        // Vérifier si le serveur n'est pas plein
        if (serverInfo.getCurrentPlayers() >= serverInfo.getMaxPlayers()) {
            return false;
        }
        
        return true;
    }
    
    private double getPlayerRatio(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        if (serverInfo.getMaxPlayers() == 0) {
            return 0.0;
        }
        return (double) serverInfo.getCurrentPlayers() / serverInfo.getMaxPlayers();
    }
    
    public boolean shouldMigrate(fr.nhsoul.dynamo.common.model.ServerInfo serverInfo) {
        if (!configManager.isAutoMigrate()) {
            return false;
        }
        
        double ratio = getPlayerRatio(serverInfo) * 100;
        return ratio >= configManager.getMigrationThreshold();
    }
}