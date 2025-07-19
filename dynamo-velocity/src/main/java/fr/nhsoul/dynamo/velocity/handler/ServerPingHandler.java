package fr.nhsoul.dynamo.velocity.handler;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import fr.nhsoul.dynamo.common.model.ServerInfo;
import fr.nhsoul.dynamo.velocity.DynamoVelocityPlugin;

import java.util.Collection;

public class ServerPingHandler {
    private final DynamoVelocityPlugin plugin;
    
    public ServerPingHandler(DynamoVelocityPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder builder = event.getPing().asBuilder();
        
        // Calculer le nombre total de joueurs sur tous les serveurs découverts
        Collection<ServerInfo> servers = plugin.getDiscoveryService().getDiscoveredServers().values();
        
        int totalPlayers = servers.stream()
            .mapToInt(ServerInfo::getCurrentPlayers)
            .sum();
        
        int maxPlayers = servers.stream()
            .mapToInt(ServerInfo::getMaxPlayers)
            .sum();
        
        // Mettre à jour les informations de ping
        builder.onlinePlayers(totalPlayers);
        builder.maximumPlayers(maxPlayers);
        
        // Personnaliser le MOTD avec le nombre de serveurs
        /*Component motd = Component.text()
            .append(Component.text("Réseau Dynamo", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(Component.text(servers.size() + " serveurs disponibles", NamedTextColor.GRAY))
            .build();
        
        builder.description(motd);
        */
        event.setPing(builder.build());
    }
}

