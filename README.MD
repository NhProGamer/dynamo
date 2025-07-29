# Dynamo - Service Discovery for Minecraft

Auto-discovery et load balancing pour réseaux Minecraft avec PaperMC et Velocity.

## Qu'est-ce que Dynamo ?

Dynamo permet aux serveurs PaperMC de s'enregistrer automatiquement auprès du proxy Velocity via NATS, sans configuration manuelle.

**Avant Dynamo :**
- Configuration manuelle des serveurs dans Velocity
- Pas de détection des pannes
- Load balancing statique

**Avec Dynamo :**
- Serveurs détectés automatiquement
- Failover automatique en cas de panne
- Load balancing intelligent

## Installation rapide

### 1. Démarrer NATS
```bash
# Linux/macOS
wget https://github.com/nats-io/nats-server/releases/latest/download/nats-server-linux-amd64.zip
unzip nats-server-linux-amd64.zip
./nats-server --port 4222

# Windows
# Télécharger depuis https://github.com/nats-io/nats-server/releases
nats-server.exe --port 4222

# Docker
docker run -d --name nats -p 4222:4222 nats:latest
```

### 2. Build des plugins
```bash
git clone https://github.com/NhProGamer/dynamo.git
cd dynamo
./gradlew buildAll
```

### 3. Installer les plugins
- Copier `dynamo-paper/build/libs/dynamo-paper-1.0.0.jar` dans `plugins/` de vos serveurs PaperMC
- Copier `dynamo-velocity/build/libs/dynamo-velocity-1.0.0.jar` dans `plugins/` de votre proxy Velocity
- Redémarrer les serveurs

## Configuration

### Plugin PaperMC (`plugins/DynamoPaper/config.yml`)
```yaml
nats:
  url: "nats://localhost:4222"

server:
  name: "lobby-1"
  groups:
    - "lobby"
    - "hub"
  port: 25565
  heartbeat-interval: 10
```

### Plugin Velocity (`plugins/dynamo-velocity/config.yml`)
```yaml
nats:
  url: "nats://localhost:4222"

groups:
  lobby:
    load-balancing: "LEAST_PLAYERS"
    priority: 1
  
  survival:
    load-balancing: "ROUND_ROBIN"
    fallback-server: "lobby-1"
    priority: 2

load-balancing:
  default-group: "lobby"
```

## Utilisation

### Commandes
- `/dynamo list` - Liste tous les serveurs
- `/dynamo groups` - Affiche les groupes
- `/dynamo status` - Statut du système
- `/dynamo reload` - Recharge la config

### Démarrage
1. Démarrer NATS Server
2. Démarrer vos serveurs PaperMC (ordre libre)
3. Démarrer Velocity (peut être en dernier)

Les serveurs s'enregistrent automatiquement !

## Comment ça marche ?

```
PaperMC Serveur → NATS → Velocity Proxy
     │                        │
 • Heartbeat              • Auto-discovery
 • Métadonnées            • Load balancing
 • Auto-register          • Failover
```

1. **PaperMC** envoie un heartbeat toutes les 10 secondes via NATS
2. **Velocity** écoute les heartbeats et met à jour sa liste de serveurs
3. **Load balancing** automatique selon la stratégie configurée
4. **Failover** automatique si un serveur ne répond plus (30s timeout)

## Stratégies de Load Balancing

- **LEAST_PLAYERS** - Serveur avec le moins de joueurs
- **ROUND_ROBIN** - Rotation équitable
- **RANDOM** - Sélection aléatoire
- **FIRST_AVAILABLE** - Premier serveur disponible

## Performance

- **NATS Server** : 10-15 MB RAM
- **Plugin PaperMC** : ~5 MB RAM par serveur
- **Plugin Velocity** : ~10 MB RAM
- **Latence** : < 1ms

## Structure du projet

```
dynamo/
├── dynamo-common/      # Classes partagées
├── dynamo-paper/       # Plugin PaperMC
├── dynamo-velocity/    # Plugin Velocity
├── build.gradle        # Configuration build
└── settings.gradle     # Modules
```

## Développement

```bash
# Build
./gradlew buildAll

# Tests
./gradlew test

# Clean
./gradlew cleanAll
```

## Licence

Custom License - Voir [LICENSE](LICENSE)

---

**Questions ?** Ouvrir une [issue GitHub](https://github.com/NhProGamer/dynamo/issues)
