// ========== NatsService.java ==========
package fr.nhsoul.dynamo.velocity.service;

import fr.nhsoul.dynamo.common.config.NatsConfig;
import fr.nhsoul.dynamo.common.model.ServerEvent;
import fr.nhsoul.dynamo.common.util.ProtobufSerializer;
import fr.nhsoul.dynamo.velocity.config.VelocityConfigManager;
import io.nats.client.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.function.Consumer;

public class NatsService {
    private final VelocityConfigManager configManager;
    private final Logger logger;
    private Connection natsConnection;
    private volatile boolean running = false;

    public NatsService(VelocityConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public boolean start() {
        try {
            NatsConfig natsConfig = configManager.getNatsConfig();

            Options options = new Options.Builder()
                    .server(natsConfig.getUrl())
                    .connectionTimeout(Duration.ofMillis(natsConfig.getConnectionTimeout()))
                    .reconnectWait(Duration.ofMillis(natsConfig.getReconnectTimeout()))
                    .maxReconnects(natsConfig.getMaxReconnectAttempts())
                    .connectionListener(this::handleConnectionEvent)
                    //.errorListener(this::handleErrorEvent)
                    .build();

            natsConnection = Nats.connect(options);
            running = true;

            logger.info("Connexion NATS établie avec succès !");
            return true;

        } catch (Exception e) {
            logger.error("Erreur lors de la connexion à NATS", e);
            return false;
        }
    }

    public void stop() {
        running = false;

        if (natsConnection != null) {
            try {
                natsConnection.close();
                logger.info("Connexion NATS fermée.");
            } catch (InterruptedException e) {
                logger.warn("Interruption lors de la fermeture de NATS", e);
            }
        }
    }

    public Dispatcher subscribe(String subject, Consumer<ServerEvent> handler) {
        if (!running || natsConnection == null) {
            throw new IllegalStateException("Service NATS non démarré");
        }

        return natsConnection.createDispatcher(message -> {
            try {
                byte[] data = message.getData();
                ServerEvent event = ProtobufSerializer.deserializeServerEvent(data);
                handler.accept(event);
            } catch (Exception e) {
                logger.error("Erreur lors du traitement du message NATS", e);
            }
        }).subscribe(subject);
    }

    public boolean isConnected() {
        return natsConnection != null && natsConnection.getStatus() == Connection.Status.CONNECTED;
    }

    private void handleConnectionEvent(Connection conn, ConnectionListener.Events type) {
        switch (type) {
            case CONNECTED:
                logger.info("Connecté à NATS");
                break;
            case DISCONNECTED:
                logger.warn("Déconnecté de NATS");
                break;
            case RECONNECTED:
                logger.info("Reconnecté à NATS");
                break;
            case CLOSED:
                logger.info("Connexion NATS fermée");
                break;
        }
    }

    /*private void handleErrorEvent(Connection conn, ErrorListener.ErrorEvent errorEvent) {
        logger.error("Erreur NATS: {}", errorEvent.getError().getMessage());
    }*/
}