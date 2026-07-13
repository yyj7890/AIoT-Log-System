package com.aiot.log.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mqtt.discovery")
public class MqttDiscoveryProperties {

    private Boolean enabled = true;
    private Integer port = 19830;
    private String brokerHost = "";
    private Integer brokerPort = 1883;
    private Boolean tls = false;
    private String token = "";

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        this.brokerHost = brokerHost;
    }

    public Integer getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(Integer brokerPort) {
        this.brokerPort = brokerPort;
    }

    public Boolean getTls() {
        return tls;
    }

    public void setTls(Boolean tls) {
        this.tls = tls;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
