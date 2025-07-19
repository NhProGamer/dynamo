package fr.nhsoul.dynamo.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import fr.nhsoul.dynamo.common.model.ServerInfo;
import fr.nhsoul.dynamo.velocity.DynamoVelocityPlugin;
import fr.nhsoul.dynamo.velocity.service.LoadBalancingService;
import fr.nhsoul.dynamo.velocity.service.ServerDiscoveryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DynamoCommand implements SimpleCommand {
    private final DynamoVelocityPlugin plugin;
    
    public DynamoCommand(DynamoVelocityPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            sendHelp(invocation);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "list":
                handleListCommand(invocation);
                break;
                
            case "groups":
                handleGroupsCommand(invocation);
                break;
                
            case "status":
                handleStatusCommand(invocation);
                break;
                
            case "reload":
                handleReloadCommand(invocation);
                break;
                
            default:
                sendHelp(invocation);
        }
    }
    
    private void handleListCommand(Invocation invocation) {
        ServerDiscoveryService discoveryService = plugin.getDiscoveryService();
        Map<String, ServerInfo> servers = discoveryService.getDiscoveredServers();
        
        if (servers.isEmpty()) {
            invocation.source().sendMessage(
                Component.text("Aucun serveur découvert.", NamedTextColor.YELLOW)
            );
            return;
        }
        
        invocation.source().sendMessage(
            Component.text("Serveurs découverts:", NamedTextColor.GREEN)
        );
        
        servers.values().forEach(serverInfo -> {
            Component serverComponent = Component.text()
                .append(Component.text("  • ", NamedTextColor.GRAY))
                .append(Component.text(serverInfo.getName(), NamedTextColor.AQUA))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(serverInfo.getHost() + ":" + serverInfo.getPort(), NamedTextColor.WHITE))
                .append(Component.text(") - ", NamedTextColor.GRAY))
                .append(Component.text(serverInfo.getCurrentPlayers() + "/" + serverInfo.getMaxPlayers(), NamedTextColor.YELLOW))
                .append(Component.text(" joueurs", NamedTextColor.GRAY))
                .build();
            
            invocation.source().sendMessage(serverComponent);
        });
    }
    
    private void handleGroupsCommand(Invocation invocation) {
        LoadBalancingService loadBalancingService = plugin.getLoadBalancingService();
        Map<String, List<ServerInfo>> serversByGroup = loadBalancingService.getServersByGroup();
        
        if (serversByGroup.isEmpty()) {
            invocation.source().sendMessage(
                Component.text("Aucun groupe de serveurs disponible.", NamedTextColor.YELLOW)
            );
            return;
        }
        
        invocation.source().sendMessage(
            Component.text("Groupes de serveurs:", NamedTextColor.GREEN)
        );
        
        serversByGroup.forEach((groupName, servers) -> {
            Component groupComponent = Component.text()
                .append(Component.text("  • ", NamedTextColor.GRAY))
                .append(Component.text(groupName, NamedTextColor.GOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(Component.text(servers.size() + " serveurs", NamedTextColor.WHITE))
                .append(Component.text(")", NamedTextColor.GRAY))
                .build();
            
            invocation.source().sendMessage(groupComponent);
            
            servers.forEach(serverInfo -> {
                Component serverComponent = Component.text()
                    .append(Component.text("    - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(serverInfo.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(serverInfo.getCurrentPlayers() + "/" + serverInfo.getMaxPlayers(), NamedTextColor.YELLOW))
                    .append(Component.text(")", NamedTextColor.GRAY))
                    .build();
                
                invocation.source().sendMessage(serverComponent);
            });
        });
    }
    
    private void handleStatusCommand(Invocation invocation) {
        boolean natsConnected = plugin.getNatsService().isConnected();
        int totalServers = plugin.getDiscoveryService().getDiscoveredServers().size();
        
        Component statusComponent = Component.text()
            .append(Component.text("Statut Dynamo:", NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("  NATS: ", NamedTextColor.GRAY))
            .append(Component.text(natsConnected ? "Connecté" : "Déconnecté", 
                    natsConnected ? NamedTextColor.GREEN : NamedTextColor.RED))
            .append(Component.newline())
            .append(Component.text("  Serveurs: ", NamedTextColor.GRAY))
            .append(Component.text(totalServers + " découverts", NamedTextColor.YELLOW))
            .build();
        
        invocation.source().sendMessage(statusComponent);
    }
    
    private void handleReloadCommand(Invocation invocation) {
        if (!invocation.source().hasPermission("dynamo.reload")) {
            invocation.source().sendMessage(
                Component.text("Vous n'avez pas la permission d'utiliser cette commande.", NamedTextColor.RED)
            );
            return;
        }
        
        plugin.getConfigManager().reloadConfig();
        invocation.source().sendMessage(
            Component.text("Configuration rechargée avec succès.", NamedTextColor.GREEN)
        );
    }
    
    private void sendHelp(Invocation invocation) {
        Component helpComponent = Component.text()
            .append(Component.text("Commandes Dynamo:", NamedTextColor.GREEN))
            .append(Component.newline())
            .append(Component.text("  /dynamo list", NamedTextColor.AQUA))
            .append(Component.text(" - Liste tous les serveurs découverts", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  /dynamo groups", NamedTextColor.AQUA))
            .append(Component.text(" - Affiche les groupes de serveurs", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  /dynamo status", NamedTextColor.AQUA))
            .append(Component.text(" - Affiche le statut du système", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("  /dynamo reload", NamedTextColor.AQUA))
            .append(Component.text(" - Recharge la configuration", NamedTextColor.GRAY))
            .build();
        
        invocation.source().sendMessage(helpComponent);
    }
    
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        
        if (args.length <= 1) {
            return CompletableFuture.completedFuture(List.of("list", "groups", "status", "reload"));
        }
        
        return CompletableFuture.completedFuture(List.of());
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("dynamo.command");
    }
}