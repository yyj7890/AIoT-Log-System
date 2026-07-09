package com.aiot.log.config;

import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.service.DeviceReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MqttDeviceReportSubscriber implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceReportSubscriber.class);

    private final MqttProperties mqttProperties;
    private final DeviceReportService deviceReportService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private MqttClient mqttClient;
    private volatile boolean connected;
    private volatile LocalDateTime lastConnectedAt;
    private volatile LocalDateTime lastDisconnectedAt;
    private volatile LocalDateTime lastMessageAt;
    private volatile String lastMessageTopic;
    private volatile String lastError;
    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong handledCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();

    public MqttDeviceReportSubscriber(
            MqttProperties mqttProperties,
            DeviceReportService deviceReportService,
            ObjectMapper objectMapper,
            Validator validator) {
        this.mqttProperties = mqttProperties;
        this.deviceReportService = deviceReportService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(mqttProperties.getEnabled())) {
            log.info("MQTT subscriber is disabled.");
            return;
        }

        try {
            mqttClient = new MqttClient(
                    mqttProperties.getBrokerUrl(),
                    mqttProperties.getClientId(),
                    new MemoryPersistence());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);

            mqttClient.connect(options);
            connected = mqttClient.isConnected();
            if (connected) {
                lastConnectedAt = LocalDateTime.now();
                lastError = null;
            }
            subscribeReportTopic();
        } catch (MqttException exception) {
            connected = false;
            lastError = exception.getMessage();
            log.warn("MQTT subscriber could not connect to {}. HTTP reporting is still available.",
                    mqttProperties.getBrokerUrl(), exception);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connected = true;
        lastConnectedAt = LocalDateTime.now();
        lastError = null;
        if (reconnect) {
            log.info("MQTT reconnected to {}", serverURI);
            subscribeReportTopic();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        connected = false;
        lastDisconnectedAt = LocalDateTime.now();
        lastError = cause == null ? null : cause.getMessage();
        log.warn("MQTT connection lost.", cause);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        receivedCount.incrementAndGet();
        lastMessageAt = LocalDateTime.now();
        lastMessageTopic = topic;
        try {
            DeviceReportCreateRequest request = objectMapper.readValue(payload, DeviceReportCreateRequest.class);
            validateRequest(request);
            deviceReportService.createReport(request);
            handledCount.incrementAndGet();
            lastError = null;
            log.info("MQTT device report handled. topic={}, qos={}", topic, message.getQos());
        } catch (Exception exception) {
            failedCount.incrementAndGet();
            lastError = exception.getMessage();
            log.warn("MQTT device report ignored. topic={}, payload={}", topic, payload, exception);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // This backend only subscribes to device reports.
    }

    private void subscribeReportTopic() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        try {
            int qos = mqttProperties.getQos() == null ? 1 : mqttProperties.getQos();
            mqttClient.subscribe(mqttProperties.getTopic(), qos);
            log.info("MQTT subscribed topic={}, qos={}", mqttProperties.getTopic(), qos);
        } catch (MqttException exception) {
            lastError = exception.getMessage();
            log.warn("MQTT subscribe failed. topic={}", mqttProperties.getTopic(), exception);
        }
    }

    private void validateRequest(DeviceReportCreateRequest request) {
        Set<ConstraintViolation<DeviceReportCreateRequest>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder("MQTT payload validation failed: ");
        int index = 0;
        for (ConstraintViolation<DeviceReportCreateRequest> violation : violations) {
            if (index > 0) {
                builder.append("; ");
            }
            builder.append(violation.getPropertyPath()).append(" ").append(violation.getMessage());
            index++;
        }
        throw new IllegalArgumentException(builder.toString());
    }

    public boolean isConnected() {
        return connected && mqttClient != null && mqttClient.isConnected();
    }

    public LocalDateTime getLastConnectedAt() {
        return lastConnectedAt;
    }

    public LocalDateTime getLastDisconnectedAt() {
        return lastDisconnectedAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public String getLastMessageTopic() {
        return lastMessageTopic;
    }

    public String getLastError() {
        return lastError;
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getHandledCount() {
        return handledCount.get();
    }

    public long getFailedCount() {
        return failedCount.get();
    }
}
