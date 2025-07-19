package fr.nhsoul.dynamo.common.model;

public class ServerEvent {
    public enum EventType {
        REGISTER,
        HEARTBEAT,
        UNREGISTER,
        PLAYER_JOIN,
        PLAYER_LEAVE
    }

    private EventType type;
    private ServerInfo serverInfo;
    private long timestamp;

    public ServerEvent() {}

    public ServerEvent(EventType type, ServerInfo serverInfo) {
        this.type = type;
        this.serverInfo = serverInfo;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et setters
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }

    public ServerInfo getServerInfo() { return serverInfo; }
    public void setServerInfo(ServerInfo serverInfo) { this.serverInfo = serverInfo; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return String.format("ServerEvent{type=%s, server=%s, timestamp=%d}",
                type, serverInfo != null ? serverInfo.getName() : "null", timestamp);
    }
}