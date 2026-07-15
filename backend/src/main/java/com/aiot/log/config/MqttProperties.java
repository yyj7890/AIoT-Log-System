package com.aiot.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private Boolean enabled = false;
    private String mode = "lan";
    private String brokerUrl = "tcp://127.0.0.1:1883";
    private String clientId = "aiot-log-backend";
    private String username = "";
    private String password = "";
    private String topic = "aiot/device/+/report";
    private String logTopic = "aiot/device/+/log";
    private Integer runtimeLogMergeWindowSeconds = 30;
    private Integer qos = 1;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isRemoteMode() {
        return "remote".equalsIgnoreCase(mode);
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLogTopic() {
        return logTopic;
    }

    public void setLogTopic(String logTopic) {
        this.logTopic = logTopic;
    }

    public Integer getRuntimeLogMergeWindowSeconds() {
        return runtimeLogMergeWindowSeconds;
    }

    public void setRuntimeLogMergeWindowSeconds(Integer runtimeLogMergeWindowSeconds) {
        this.runtimeLogMergeWindowSeconds = runtimeLogMergeWindowSeconds;
    }

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }
}
