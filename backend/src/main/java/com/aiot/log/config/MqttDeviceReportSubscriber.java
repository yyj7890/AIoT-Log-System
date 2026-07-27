package com.aiot.log.config;

import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.dto.DeviceRuntimeLogCreateRequest;
import com.aiot.log.announcement.AnnouncementAck;
import com.aiot.log.announcement.AnnouncementAckService;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.service.DeviceReportService;
import com.aiot.log.service.LogService;
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
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MqttDeviceReportSubscriber implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceReportSubscriber.class);

    private final MqttProperties mqttProperties;
    private final DeviceReportService deviceReportService;
    private final LogService logService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final AnnouncementAckService announcementAckService;
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
            LogService logService,
            ObjectMapper objectMapper,
            Validator validator,
            AnnouncementAckService announcementAckService) {
        this.mqttProperties = mqttProperties;
        this.deviceReportService = deviceReportService;
        this.logService = logService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.announcementAckService = announcementAckService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(mqttProperties.getEnabled())) {
            log.info("MQTT subscriber is disabled.");
            return;
        }

        if (!hasValidModeConfiguration()) {
            return;
        }

        connect(false);
    }

    public synchronized void reconnect() {
        closeCurrentClient();
        if (!hasValidModeConfiguration()) {
            throw new BusinessException(ErrorCode.MQTT_RECONNECT_FAILED, lastError);
        }
        connect(true);
    }

    private synchronized void connect(boolean throwOnFailure) {
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
            if (StringUtils.hasText(mqttProperties.getUsername())) {
                options.setUserName(mqttProperties.getUsername());
                options.setPassword((mqttProperties.getPassword() == null ? "" : mqttProperties.getPassword())
                        .toCharArray());
            }

            mqttClient.connect(options);
            connected = mqttClient.isConnected();
            if (connected) {
                lastConnectedAt = LocalDateTime.now();
                lastError = null;
            }
            subscribeTopics();
        } catch (MqttException exception) {
            connected = false;
            lastError = exception.getMessage();
            log.warn("MQTT subscriber could not connect to {}. HTTP reporting is still available.",
                    mqttProperties.getBrokerUrl(), exception);
            if (throwOnFailure) {
                throw new BusinessException(
                        ErrorCode.MQTT_RECONNECT_FAILED,
                        ErrorCode.MQTT_RECONNECT_FAILED.getMessage(),
                        exception);
            }
        }
    }

    private void closeCurrentClient() {
        MqttClient current = mqttClient;
        mqttClient = null;
        connected = false;
        if (current == null) {
            return;
        }
        try {
            if (current.isConnected()) {
                current.disconnectForcibly(1000, 1000, false);
            }
            current.close();
            lastDisconnectedAt = LocalDateTime.now();
        } catch (MqttException exception) {
            log.warn("MQTT client could not be cleanly closed before credential reload.", exception);
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connected = true;
        lastConnectedAt = LocalDateTime.now();
        lastError = null;
        if (reconnect) {
            log.info("MQTT reconnected to {}", serverURI);
            subscribeTopics();
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
            if (isAnnouncementAckTopic(topic)) {
                AnnouncementAck ack = objectMapper.readValue(payload, AnnouncementAck.class);
                validateAnnouncementAckTopic(topic, ack.getDeviceCode());
                boolean recorded = announcementAckService.record(ack);
                log.info("MQTT announcement ACK handled. topic={}, status={}, recorded={}",
                        topic, ack.getStatus(), recorded);
            } else if (isRuntimeLogTopic(topic)) {
                DeviceRuntimeLogCreateRequest request = objectMapper.readValue(payload, DeviceRuntimeLogCreateRequest.class);
                validateRequest(request);
                validateTopicDeviceCode(topic, request.getDeviceCode());
                logService.createDeviceRuntimeLog(request);
                log.info("MQTT device runtime log handled. topic={}, qos={}", topic, message.getQos());
            } else {
                DeviceReportCreateRequest request = objectMapper.readValue(payload, DeviceReportCreateRequest.class);
                validateRequest(request);
                validateTopicDeviceCode(topic, request.getDeviceCode());
                deviceReportService.createReport(request);
                log.info("MQTT device report handled. topic={}, qos={}", topic, message.getQos());
            }
            handledCount.incrementAndGet();
            lastError = null;
        } catch (Exception exception) {
            failedCount.incrementAndGet();
            lastError = exception.getMessage();
            log.warn("MQTT device message ignored. topic={}, payloadBytes={}, reason={}",
                    topic, message.getPayload().length, exception.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // This backend only subscribes to device reports.
    }

    private void subscribeTopics() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            return;
        }
        try {
            int qos = mqttProperties.getQos() == null ? 1 : mqttProperties.getQos();
            mqttClient.subscribe(mqttProperties.getTopic(), qos);
            mqttClient.subscribe(mqttProperties.getLogTopic(), qos);
            mqttClient.subscribe(mqttProperties.getAnnouncementAckTopic(), qos);
            log.info("MQTT subscribed reportTopic={}, logTopic={}, announcementAckTopic={}, qos={}",
                    mqttProperties.getTopic(), mqttProperties.getLogTopic(),
                    mqttProperties.getAnnouncementAckTopic(), qos);
        } catch (MqttException exception) {
            lastError = exception.getMessage();
            log.warn("MQTT subscribe failed. reportTopic={}, logTopic={}",
                    mqttProperties.getTopic(), mqttProperties.getLogTopic(), exception);
        }
    }

    private boolean hasValidModeConfiguration() {
        if (!mqttProperties.isRemoteMode()) {
            return true;
        }
        if (!StringUtils.hasText(mqttProperties.getBrokerUrl())
                || !mqttProperties.getBrokerUrl().regionMatches(true, 0, "ssl://", 0, "ssl://".length())) {
            lastError = "Remote MQTT mode requires an ssl:// broker URL.";
            log.error("Remote MQTT subscriber is disabled because MQTT_BROKER_URL must use ssl://.");
            return false;
        }
        if (!StringUtils.hasText(mqttProperties.getUsername()) || !StringUtils.hasText(mqttProperties.getPassword())) {
            lastError = "Remote MQTT mode requires MQTT username and password.";
            log.error("Remote MQTT subscriber is disabled because credentials are missing.");
            return false;
        }
        return true;
    }

    private boolean isRuntimeLogTopic(String topic) {
        return topic != null && topic.endsWith("/log");
    }

    private boolean isAnnouncementAckTopic(String topic) {
        return topic != null && topic.endsWith("/announcement/ack");
    }

    private void validateAnnouncementAckTopic(String topic, String deviceCode) {
        String[] segments = topic == null ? new String[0] : topic.split("/", -1);
        if (!isAnnouncementAckTopic(topic) || segments.length != 5 || !"aiot".equals(segments[0])
                || !"device".equals(segments[1]) || !"announcement".equals(segments[3])
                || !segments[2].equals(deviceCode)) {
            throw new IllegalArgumentException("MQTT topic is not an announcement ACK topic");
        }
    }

    public synchronized void publishAnnouncement(String topic, byte[] payload) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_PUBLISH_FAILED);
        }
        try {
            mqttClient.publish(topic, payload, 1, false);
            log.info("MQTT announcement message published. topic={}, payloadBytes={}", topic, payload.length);
        } catch (MqttException exception) {
            lastError = exception.getMessage();
            log.warn("MQTT announcement publish failed. topic={}, payloadBytes={}", topic, payload.length);
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_PUBLISH_FAILED);
        }
    }

    private void validateTopicDeviceCode(String topic, String deviceCode) {
        if (!StringUtils.hasText(topic)) {
            throw new IllegalArgumentException("MQTT topic is missing");
        }
        String[] segments = topic.split("/", -1);
        if (segments.length < 2 || !StringUtils.hasText(segments[segments.length - 2])) {
            throw new IllegalArgumentException("MQTT topic does not contain a device code");
        }
        String topicDeviceCode = segments[segments.length - 2];
        if (!topicDeviceCode.equals(deviceCode)) {
            throw new IllegalArgumentException("MQTT topic device code does not match payload");
        }
    }

    private void validateRequest(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder("MQTT payload validation failed: ");
        int index = 0;
        for (ConstraintViolation<Object> violation : violations) {
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
