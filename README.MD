# Dynamo - Service Discovery for Minecraft

Auto-discovery and load balancing for Minecraft networks using PaperMC and Velocity.

## What is Dynamo?

Dynamo allows PaperMC servers to automatically register themselves with the Velocity proxy via NATS, without manual configuration.

**Before Dynamo:**

* Manual configuration of servers in Velocity
* No failure detection
* Static load balancing

**With Dynamo:**

* Servers detected automatically
* Automatic failover in case of failure
* Intelligent load balancing

## Quick Installation

### 1. Start NATS

```bash
# Linux/macOS
wget https://github.com/nats-io/nats-server/releases/latest/download/nats-server-linux-amd64.zip
unzip nats-server-linux-amd64.zip
./nats-server --port 4222

# Windows
# Download from https://github.com/nats-io/nats-server/releases
nats-server.exe --port 4222

# Docker
docker run -d --name nats -p 4222:4222 nats:latest
```

### 2. Build the plugins

```bash
git clone https://github.com/NhProGamer/dynamo.git
cd dynamo
./gradlew buildAll
```

### 3. Install the plugins

* Copy `dynamo-paper/build/libs/dynamo-paper-1.0.0.jar` into the `plugins/` folder of your PaperMC servers
* Copy `dynamo-velocity/build/libs/dynamo-velocity-1.0.0.jar` into the `plugins/` folder of your Velocity proxy
* Restart the servers

## Usage

### Commands

* `/dynamo list` - Lists all servers
* `/dynamo groups` - Shows server groups
* `/dynamo status` - System status
* `/dynamo reload` - Reloads the configuration

### Startup

1. Start the NATS server
2. Start your PaperMC servers (any order)
3. Start Velocity (can be last)

Servers will register automatically!

## How does it work?

```
PaperMC Server → NATS → Velocity Proxy
     │                        │
 • Heartbeat              • Auto-discovery
 • Metadata               • Load balancing
 • Auto-register          • Failover
```

1. **PaperMC** sends a heartbeat every 10 seconds via NATS
2. **Velocity** listens to the heartbeats and updates its server list
3. **Automatic load balancing** based on the configured strategy
4. **Automatic failover** if a server stops responding (30s timeout)

## Load Balancing Strategies

* **LEAST\_PLAYERS** - Server with the fewest players
* **ROUND\_ROBIN** - Even rotation
* **RANDOM** - Random selection
* **FIRST\_AVAILABLE** - First available server

## Performance

* **NATS Server**: 10–15 MB RAM
* **PaperMC Plugin**: \~5 MB RAM per server
* **Velocity Plugin**: \~10 MB RAM
* **Latency**: < 1ms

## Project Structure

```
dynamo/
├── dynamo-common/      # Shared classes
├── dynamo-paper/       # PaperMC plugin
├── dynamo-velocity/    # Velocity plugin
├── build.gradle        # Build configuration
└── settings.gradle     # Modules
```

## Development

```bash
# Build
./gradlew buildAll

# Tests
./gradlew test

# Clean
./gradlew cleanAll
```

## License

Custom License – See [LICENSE](LICENSE)

---

**Questions?** Open a [GitHub issue](https://github.com/NhProGamer/dynamo/issues)
