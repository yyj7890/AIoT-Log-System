package com.aiot.log.vo;

public class MqttGlobalCredentialStatusVO {
    private String username;
    private Boolean passwordConfigured;
    private Boolean anonymousAccessEnabled;
    private Boolean activationPending;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Boolean getPasswordConfigured() { return passwordConfigured; }
    public void setPasswordConfigured(Boolean passwordConfigured) { this.passwordConfigured = passwordConfigured; }
    public Boolean getAnonymousAccessEnabled() { return anonymousAccessEnabled; }
    public void setAnonymousAccessEnabled(Boolean anonymousAccessEnabled) { this.anonymousAccessEnabled = anonymousAccessEnabled; }
    public Boolean getActivationPending() { return activationPending; }
    public void setActivationPending(Boolean activationPending) { this.activationPending = activationPending; }
}
