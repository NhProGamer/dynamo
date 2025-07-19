package fr.nhsoul.dynamo.paper.service;

import fr.nhsoul.dynamo.common.config.NatsConfig;
import fr.nhsoul.dynamo.common.model.ServerEvent;
import fr.nhsoul.dynamo.common.util.ProtobufSerializer;
import fr.nhsoul.dynamo.paper.config.PaperConfigManager;
import io.nats.client.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class NatsService {
    private final PaperConfigManager configManager;
    private final Logger logger;
    private Connection natsConnection;
    private volatile boolean running = false;

    public NatsService(PaperConfigManager configManager, Logger logger) {
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
            logger.severe("Erreur lors de la connexion à NATS: " + e.getMessage());
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
                logger.warning("Interruption lors de la fermeture de NATS: " + e.getMessage());
            }
        }
    }

    public CompletableFuture<Void> publishEvent(String subject, ServerEvent event) {
        if (!running || natsConnection == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Service NATS non démarré"));
        }

        try {
            byte[] data = ProtobufSerializer.serializeServerEvent(event);
            natsConnection.publish(subject, data);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.warning("Erreur lors de l'envoi de l'événement: " + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
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
                logger.warning("Déconnecté de NATS");
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
        logger.warning("Erreur NATS: " + errorEvent.getError().getMessage());
    }*/
}