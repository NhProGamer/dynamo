// ========== ServerInfo.java ==========
package fr.nhsoul.dynamo.common.model;

import java.util.List;

public class ServerInfo {
    private String name;
    private String host;
    private int port;
    private List<String> groups;
    private long timestamp;
    private int currentPlayers;
    private int maxPlayers;

    public ServerInfo() {}

    public ServerInfo(String name, String host, int port, List<String> groups) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.groups = groups;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - timestamp > timeoutMs;
    }

    @Override
    public String toString() {
        return String.format("ServerInfo{name='%s', host='%s', port=%d, groups=%s, players=%d/%d}",
                name, host, port, groups, currentPlayers, maxPlayers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ServerInfo that = (ServerInfo) obj;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}