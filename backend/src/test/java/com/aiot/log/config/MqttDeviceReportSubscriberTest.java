package com.aiot.log.config;

import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.dto.DeviceRuntimeLogCreateRequest;
import com.aiot.log.announcement.AnnouncementAckService;
import com.aiot.log.service.DeviceReportService;
import com.aiot.log.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttDeviceReportSubscriberTest {

    @Mock
    private DeviceReportService deviceReportService;

    @Mock
    private LogService logService;

    @Mock
    private AnnouncementAckService announcementAckService;

    private MqttDeviceReportSubscriber subscriber;

    @BeforeEach
    void setUp() {
        MqttProperties properties = new MqttProperties();
        properties.setEnabled(false);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        subscriber = new MqttDeviceReportSubscriber(
                properties,
                deviceReportService,
                logService,
                new ObjectMapper(),
                validator,
                announcementAckService);
    }

    @Test
    void routesValidRuntimeLogAndUpdatesCounters() {
        subscriber.messageArrived(
                "aiot/device/DEVICE-001/log",
                message("""
                        {
                          "deviceCode": "DEVICE-001",
                          "eventType": "mqtt_connected",
                          "message": "Log MQTT connected",
                          "level": "INFO",
                          "logType": "RUNNING"
                        }
                        """, 1));

        ArgumentCaptor<DeviceRuntimeLogCreateRequest> request =
                ArgumentCaptor.forClass(DeviceRuntimeLogCreateRequest.class);
        verify(logService).createDeviceRuntimeLog(request.capture());
        verify(deviceReportService, never()).createReport(org.mockito.ArgumentMatchers.any());
        assertEquals("DEVICE-001", request.getValue().getDeviceCode());
        assertEquals(1, subscriber.getReceivedCount());
        assertEquals(1, subscriber.getHandledCount());
        assertEquals(0, subscriber.getFailedCount());
        assertEquals("aiot/device/DEVICE-001/log", subscriber.getLastMessageTopic());
        assertNotNull(subscriber.getLastMessageAt());
        assertNull(subscriber.getLastError());
    }

    @Test
    void routesValidDeviceReport() {
        subscriber.messageArrived(
                "aiot/device/DEVICE-002/report",
                message("""
                        {
                          "deviceCode": "DEVICE-002",
                          "temperature": 25.5,
                          "humidity": 60,
                          "pressure": 1008.4,
                          "illuminance": 312.5,
                          "status": "NORMAL"
                        }
                        """, 1));

        ArgumentCaptor<DeviceReportCreateRequest> request =
                ArgumentCaptor.forClass(DeviceReportCreateRequest.class);
        verify(deviceReportService).createReport(request.capture());
        verify(logService, never()).createDeviceRuntimeLog(org.mockito.ArgumentMatchers.any());
        assertEquals("DEVICE-002", request.getValue().getDeviceCode());
        assertEquals(new java.math.BigDecimal("1008.4"), request.getValue().getPressure());
        assertEquals(new java.math.BigDecimal("312.5"), request.getValue().getIlluminance());
        assertEquals(1, subscriber.getHandledCount());
        assertEquals(0, subscriber.getFailedCount());
    }

    @Test
    void routesAnnouncementAckWithoutAffectingReportOrLogServices() {
        when(announcementAckService.record(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        subscriber.messageArrived(
                "aiot/device/DEVICE-005/announcement/ack",
                message("""
                        {
                          "protocol": "aiot-announcement-v1",
                          "taskId": "test-task",
                          "deviceCode": "DEVICE-005",
                          "status": "received"
                        }
                        """, 1));

        verify(announcementAckService).record(org.mockito.ArgumentMatchers.any());
        verify(logService, never()).createDeviceRuntimeLog(org.mockito.ArgumentMatchers.any());
        verify(deviceReportService, never()).createReport(org.mockito.ArgumentMatchers.any());
        assertEquals(1, subscriber.getHandledCount());
    }

    @Test
    void rejectsMalformedOrInvalidPayloadWithoutCallingServices() {
        subscriber.messageArrived(
                "aiot/device/DEVICE-003/log",
                message("{invalid-json", 1));
        subscriber.messageArrived(
                "aiot/device/DEVICE-003/log",
                message("""
                        {
                          "deviceCode": "DEVICE-003",
                          "eventType": "mqtt_connected"
                        }
                        """, 1));

        verify(logService, never()).createDeviceRuntimeLog(org.mockito.ArgumentMatchers.any());
        verify(deviceReportService, never()).createReport(org.mockito.ArgumentMatchers.any());
        assertEquals(2, subscriber.getReceivedCount());
        assertEquals(0, subscriber.getHandledCount());
        assertEquals(2, subscriber.getFailedCount());
        assertNotNull(subscriber.getLastError());
    }

    @Test
    void rejectsTopicAndPayloadDeviceCodeMismatch() {
        subscriber.messageArrived(
                "aiot/device/DEVICE-004/log",
                message("""
                        {
                          "deviceCode": "OTHER-DEVICE",
                          "eventType": "mqtt_connected",
                          "message": "Log MQTT connected"
                        }
                        """, 1));

        verify(logService, never()).createDeviceRuntimeLog(org.mockito.ArgumentMatchers.any());
        assertEquals(1, subscriber.getReceivedCount());
        assertEquals(0, subscriber.getHandledCount());
        assertEquals(1, subscriber.getFailedCount());
        assertEquals("MQTT topic device code does not match payload", subscriber.getLastError());
    }

    @Test
    void recordsConnectionLossAndRecoveryCallbacks() {
        subscriber.connectionLost(new IllegalStateException("network unavailable"));
        assertNotNull(subscriber.getLastDisconnectedAt());
        assertEquals("network unavailable", subscriber.getLastError());

        subscriber.connectComplete(false, "ssl://broker.example:8883");
        assertNotNull(subscriber.getLastConnectedAt());
        assertNull(subscriber.getLastError());
    }

    private MqttMessage message(String payload, int qos) {
        MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(qos);
        return message;
    }
}
