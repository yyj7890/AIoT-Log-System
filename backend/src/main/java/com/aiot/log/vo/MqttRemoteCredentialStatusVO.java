package com.aiot.log.vo;

public class MqttRemoteCredentialStatusVO {

    private String username;
    private Boolean passwordConfigured;
    private Boolean runtimeOverrideEnabled;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getPasswordConfigured() {
        return passwordConfigured;
    }

    public void setPasswordConfigured(Boolean passwordConfigured) {
        this.passwordConfigured = passwordConfigured;
    }

    public Boolean getRuntimeOverrideEnabled() {
        return runtimeOverrideEnabled;
    }

    public void setRuntimeOverrideEnabled(Boolean runtimeOverrideEnabled) {
        this.runtimeOverrideEnabled = runtimeOverrideEnabled;
    }
}
