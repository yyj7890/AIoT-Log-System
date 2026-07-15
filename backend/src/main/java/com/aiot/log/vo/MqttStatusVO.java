package com.aiot.log.vo;

import java.time.LocalDateTime;

public class MqttStatusVO {

    private Boolean enabled;
    private String mode;
    private String brokerUrl;
    private String clientId;
    private String topic;
    private String logTopic;
    private Integer qos;
    private Boolean connected;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime lastDisconnectedAt;
    private LocalDateTime lastMessageAt;
    private String lastMessageTopic;
    private Long receivedCount;
    private Long handledCount;
    private Long failedCount;
    private String lastError;

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

    public Integer getQos() {
        return qos;
    }

    public void setQos(Integer qos) {
        this.qos = qos;
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public LocalDateTime getLastConnectedAt() {
        return lastConnectedAt;
    }

    public void setLastConnectedAt(LocalDateTime lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }

    public LocalDateTime getLastDisconnectedAt() {
        return lastDisconnectedAt;
    }

    public void setLastDisconnectedAt(LocalDateTime lastDisconnectedAt) {
        this.lastDisconnectedAt = lastDisconnectedAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessageTopic() {
        return lastMessageTopic;
    }

    public void setLastMessageTopic(String lastMessageTopic) {
        this.lastMessageTopic = lastMessageTopic;
    }

    public Long getReceivedCount() {
        return receivedCount;
    }

    public void setReceivedCount(Long receivedCount) {
        this.receivedCount = receivedCount;
    }

    public Long getHandledCount() {
        return handledCount;
    }

    public void setHandledCount(Long handledCount) {
        this.handledCount = handledCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
