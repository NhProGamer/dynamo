package fr.nhsoul.dynamo.common.config;

public class NatsConfig {
    private String url;
    private int connectionTimeout;
    private int reconnectTimeout;
    private int maxReconnectAttempts;
    
    public NatsConfig() {}
    
    public NatsConfig(String url, int connectionTimeout, int reconnectTimeout, int maxReconnectAttempts) {
        this.url = url;
        this.connectionTimeout = connectionTimeout;
        this.reconnectTimeout = reconnectTimeout;
        this.maxReconnectAttempts = maxReconnectAttempts;
    }
    
    // Getters et setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public int getReconnectTimeout() { return reconnectTimeout; }
    public void setReconnectTimeout(int reconnectTimeout) { this.reconnectTimeout = reconnectTimeout; }
    
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public void setMaxReconnectAttempts(int maxReconnectAttempts) { this.maxReconnectAttempts = maxReconnectAttempts; }
}