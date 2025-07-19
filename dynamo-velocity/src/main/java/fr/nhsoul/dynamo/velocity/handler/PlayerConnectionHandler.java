package fr.nhsoul.dynamo.velocity.handler;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.nhsoul.dynamo.common.model.ServerInfo;
import fr.nhsoul.dynamo.velocity.DynamoVelocityPlugin;
import fr.nhsoul.dynamo.velocity.service.LoadBalancingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Optional;

public class PlayerConnectionHandler {
    private final DynamoVelocityPlugin plugin;
    private final Logger logger;

    public PlayerConnectionHandler(DynamoVelocityPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();

        // Sélectionner un serveur par défaut pour le joueur
        Optional<ServerInfo> serverInfo = plugin.getLoadBalancingService().selectDefaultServer();

        if (serverInfo.isPresent()) {
            String serverName = serverInfo.get().getName();
            // Correction : utiliser le proxy pour obtenir le serveur enregistré
            Optional<RegisteredServer> server = plugin.getProxy().getServer(serverName);

            if (server.isPresent()) {
                // Le joueur sera connecté au serveur sélectionné automatiquement
                logger.debug("Joueur {} dirigé vers le serveur {}", player.getUsername(), serverName);
            } else {
                logger.warn("Serveur {} non trouvé dans Velocity pour le joueur {}",
                        serverName, player.getUsername());
            }
        } else {
            logger.warn("Aucun serveur par défaut disponible pour le joueur {}", player.getUsername());
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        RegisteredServer server = event.getServer();

        logger.debug("Joueur {} connecté au serveur {}", player.getUsername(), server.getServerInfo().getName());
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        RegisteredServer kickedFrom = event.getServer();

        logger.info("Joueur {} éjecté du serveur {}", player.getUsername(), kickedFrom.getServerInfo().getName());

        // Essayer de rediriger vers un serveur de fallback
        Optional<ServerInfo> fallbackServer = findFallbackServer(kickedFrom.getServerInfo().getName());

        if (fallbackServer.isPresent()) {
            String fallbackServerName = fallbackServer.get().getName();
            // Correction : utiliser le proxy pour obtenir le serveur enregistré
            Optional<RegisteredServer> server = plugin.getProxy().getServer(fallbackServerName);

            if (server.isPresent()) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(server.get()));

                player.sendMessage(Component.text()
                        .append(Component.text("Vous avez été redirigé vers ", NamedTextColor.YELLOW))
                        .append(Component.text(fallbackServerName, NamedTextColor.GREEN))
                        .build());

                logger.info("Joueur {} redirigé vers le serveur de fallback {}",
                        player.getUsername(), fallbackServerName);
            } else {
                logger.warn("Serveur de fallback {} non trouvé pour le joueur {}",
                        fallbackServerName, player.getUsername());
                // Fallback vers la déconnexion avec message personnalisé
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                        Component.text("Aucun serveur de fallback disponible", NamedTextColor.RED)
                ));
            }
        } else {
            logger.warn("Aucun serveur de fallback disponible pour le joueur {}", player.getUsername());
            // Fallback vers la déconnexion avec message personnalisé
            event.setResult(KickedFromServerEvent.DisconnectPlayer.create(
                    Component.text("Aucun serveur disponible", NamedTextColor.RED)
            ));
        }
    }

    private Optional<ServerInfo> findFallbackServer(String originalServerName) {
        LoadBalancingService loadBalancingService = plugin.getLoadBalancingService();

        // Essayer de trouver un serveur de fallback dans le même groupe
        ServerInfo originalServer = plugin.getDiscoveryService().getServerInfo(originalServerName);

        if (originalServer != null) {
            for (String group : originalServer.getGroups()) {
                Optional<ServerInfo> fallback = loadBalancingService.selectServer(group);
                if (fallback.isPresent() && !fallback.get().getName().equals(originalServerName)) {
                    return fallback;
                }
            }
        }

        // Fallback vers le serveur par défaut
        return loadBalancingService.selectDefaultServer();
    }
}