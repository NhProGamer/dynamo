package fr.nhsoul.dynamo.common.config;

public class TopicsConfig {
    private String register;
    private String heartbeat;
    private String unregister;
    
    public TopicsConfig() {}
    
    public TopicsConfig(String register, String heartbeat, String unregister) {
        this.register = register;
        this.heartbeat = heartbeat;
        this.unregister = unregister;
    }
    
    // Getters et setters
    public String getRegister() { return register; }
    public void setRegister(String register) { this.register = register; }
    
    public String getHeartbeat() { return heartbeat; }
    public void setHeartbeat(String heartbeat) { this.heartbeat = heartbeat; }
    
    public String getUnregister() { return unregister; }
    public void setUnregister(String unregister) { this.unregister = unregister; }
}