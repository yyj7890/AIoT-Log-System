package com.aiot.log.service.impl;

import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.DeviceRuntimeLogCreateRequest;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.LogRecord;
import com.aiot.log.enums.LogLevel;
import com.aiot.log.enums.LogSource;
import com.aiot.log.enums.LogStatus;
import com.aiot.log.enums.LogType;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.LogRecordMapper;
import com.aiot.log.mapper.LogTagMapper;
import com.aiot.log.mapper.TagMapper;
import com.aiot.log.vo.LogVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    private static final String DEVICE_CODE = "TEST-DEVICE";

    @Mock
    private LogRecordMapper logRecordMapper;
    @Mock
    private DeviceMapper deviceMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private LogTagMapper logTagMapper;

    private LogServiceImpl logService;
    private Device device;
    private final AtomicLong nextLogId = new AtomicLong(100L);

    @BeforeEach
    void setUp() {
        MqttProperties mqttProperties = new MqttProperties();
        mqttProperties.setRuntimeLogMergeWindowSeconds(30);
        logService = new LogServiceImpl(
                logRecordMapper,
                mqttProperties,
                deviceMapper,
                tagMapper,
                logTagMapper);

        device = new Device();
        device.setId(1L);
        device.setName("测试设备");
        device.setDeviceCode(DEVICE_CODE);

        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(deviceMapper.selectById(device.getId())).thenReturn(device);
        when(logTagMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void mapsRemoteMqttTlsConnectedMessage() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "mqtt_connected",
                "Remote log MQTT TLS connected",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals("远程日志 MQTT（TLS 8883）已连接", result.getContent());
    }

    @Test
    void mapsRemoteMqttTlsConnectionFailedMessage() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "mqtt_connection_failed",
                "Remote log MQTT TLS connection failed and was retried",
                LogLevel.ERROR,
                LogType.ERROR));

        assertEquals("远程日志 MQTT（TLS 8883）连接失败，正在重试", result.getContent());
    }

    @Test
    void mapsRemoteMqttTlsConnectionRecoveredMessage() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "mqtt_reconnected",
                "Remote log MQTT TLS connection recovered",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals("远程日志 MQTT（TLS 8883）连接已恢复", result.getContent());
    }

    @Test
    void mapsLocalAiDiscoveryFailedMessage() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "local_ai_discovery_failed",
                "No valid local AI discovery response this boot",
                LogLevel.INFO,
                LogType.RUNNING));
        assertEquals("本次启动未发现本地 AI 服务", result.getContent());
    }

    @Test
    void mapsLocalAiDiscoveryFailedTitle() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "local_ai_discovery_failed",
                null,
                LogLevel.INFO,
                LogType.RUNNING));
        assertEquals("未发现本地 AI 服务", result.getContent());
    }

    @Test
    void mapsLocalAiFallbackToOfficialMessage() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "local_ai_fallback_to_official",
                "Local AI unavailable; official AI connected",
                LogLevel.INFO,
                LogType.RUNNING));
        assertEquals("本地 AI 不可用，已回退官方 AI", result.getContent());
    }

    @Test
    void mapsLocalAiFallbackToOfficialTitle() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "local_ai_fallback_to_official",
                null,
                LogLevel.INFO,
                LogType.RUNNING));
        assertEquals("已回退官方 AI", result.getContent());
    }

    @Test
    void normalizesFirmwareWarnLevelToWarning() {
        LogVO result = createNewRuntimeLog(runtimeRequest(
                "wifi_disconnected",
                "Wi-Fi disconnected",
                "WARN",
                LogType.RUNNING));

        assertEquals(LogLevel.WARNING, result.getLevel());
        assertEquals(LogStatus.PENDING, result.getStatus());
    }

    @Test
    void doesNotAppendImmediatelyAdjacentDuplicateEvent() {
        LogRecord existing = existingRuntimeLog(
                "Wi-Fi 已连接\n远程日志 MQTT（TLS 8883）已连接",
                2,
                LogLevel.INFO,
                LogType.RUNNING,
                LogStatus.RESOLVED);
        stubExistingLog(existing);

        LogVO result = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_connected",
                "Remote log MQTT TLS connected",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals("设备运行上报（2 条）", result.getTitle());
        assertEquals("Wi-Fi 已连接\n远程日志 MQTT（TLS 8883）已连接", result.getContent());
        verify(logRecordMapper, never()).updateById(any());
    }

    @Test
    void qosOneDuplicateFailureDoesNotAppendOrChangePendingState() {
        LogRecord existing = existingRuntimeLog(
                "远程日志 MQTT（TLS 8883）连接失败，正在重试",
                1,
                LogLevel.ERROR,
                LogType.ERROR,
                LogStatus.PENDING);
        stubExistingLog(existing);

        LogVO result = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_connection_failed",
                "Remote log MQTT TLS connection failed and was retried",
                LogLevel.ERROR,
                LogType.ERROR));

        assertEquals("设备运行上报（1 条）", result.getTitle());
        assertEquals(LogStatus.PENDING, result.getStatus());
        assertEquals("远程日志 MQTT（TLS 8883）连接失败，正在重试", result.getContent());
        verify(logRecordMapper, never()).updateById(any());
    }

    @Test
    void appendsDifferentEventNormally() {
        LogRecord existing = existingRuntimeLog(
                "Wi-Fi 已连接",
                1,
                LogLevel.INFO,
                LogType.RUNNING,
                LogStatus.RESOLVED);
        stubExistingLog(existing);

        LogVO result = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_connected",
                "Remote log MQTT TLS connected",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals("设备运行上报（2 条）", result.getTitle());
        assertEquals("Wi-Fi 已连接\n远程日志 MQTT（TLS 8883）已连接", result.getContent());
        verify(logRecordMapper).updateById(existing);
    }

    @Test
    void startupAlwaysCreatesNewBootBatch() {
        DeviceRuntimeLogCreateRequest request = runtimeRequest(
                "startup",
                "Firmware initialization started",
                LogLevel.INFO,
                LogType.RUNNING);
        LogVO result = createNewRuntimeLog(request);

        assertEquals("设备运行上报（1 条）", result.getTitle());
        assertEquals("固件开始初始化", result.getContent());
        verify(logRecordMapper, never()).selectOne(any());
        verify(logRecordMapper).insert(any());
    }

    @Test
    void failureRecoveryAndNewFailureKeepExpectedStatusTransitions() {
        LogRecord existing = existingRuntimeLog(
                "Wi-Fi 已连接",
                1,
                LogLevel.INFO,
                LogType.RUNNING,
                LogStatus.RESOLVED);
        stubExistingLog(existing);

        LogVO failed = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_connection_failed",
                "Remote log MQTT TLS connection failed and was retried",
                LogLevel.ERROR,
                LogType.ERROR));
        assertEquals(LogStatus.PENDING, failed.getStatus());
        assertEquals(LogLevel.ERROR, failed.getLevel());

        LogVO recovered = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_reconnected",
                "Remote log MQTT TLS connection recovered",
                LogLevel.INFO,
                LogType.RUNNING));
        assertEquals(LogStatus.RESOLVED, recovered.getStatus());
        assertEquals(LogLevel.ERROR, recovered.getLevel());

        LogVO failedAgain = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_connection_failed",
                "Remote log MQTT TLS connection failed and was retried",
                LogLevel.ERROR,
                LogType.ERROR));
        assertEquals(LogStatus.PENDING, failedAgain.getStatus());
        assertEquals(LogLevel.ERROR, failedAgain.getLevel());
        assertEquals("设备运行上报（4 条）", failedAgain.getTitle());
        assertEquals(
                "Wi-Fi 已连接\n"
                        + "远程日志 MQTT（TLS 8883）连接失败，正在重试\n"
                        + "远程日志 MQTT（TLS 8883）连接已恢复\n"
                        + "远程日志 MQTT（TLS 8883）连接失败，正在重试",
                failedAgain.getContent());
    }

    @Test
    void recoveryOutsideMergeWindowClosesPreviousPendingMqttIncident() {
        LogRecord pendingIncident = existingRuntimeLog(
                "远程日志 MQTT（TLS 8883）连接失败，正在重试",
                1,
                LogLevel.ERROR,
                LogType.ERROR,
                LogStatus.PENDING);
        when(logRecordMapper.selectOne(any())).thenReturn(pendingIncident, null);

        final LogRecord[] inserted = new LogRecord[1];
        when(logRecordMapper.insert(any())).thenAnswer(invocation -> {
            LogRecord record = invocation.getArgument(0);
            record.setId(nextLogId.getAndIncrement());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            inserted[0] = record;
            return 1;
        });
        when(logRecordMapper.selectById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return pendingIncident.getId().equals(id) ? pendingIncident : inserted[0];
        });

        LogVO recovered = logService.createDeviceRuntimeLog(runtimeRequest(
                "mqtt_reconnected",
                "Remote log MQTT TLS connection recovered",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals(LogStatus.RESOLVED, pendingIncident.getStatus());
        assertEquals(LogStatus.RESOLVED, recovered.getStatus());
        assertEquals("远程日志 MQTT（TLS 8883）连接已恢复", recovered.getContent());
        verify(logRecordMapper).updateById(pendingIncident);
        verify(logRecordMapper).insert(any());
    }

    @Test
    void nonMqttRecoveryEventDoesNotClosePendingMqttIncident() {
        LogRecord pendingIncident = existingRuntimeLog(
                "远程日志 MQTT（TLS 8883）连接失败，正在重试",
                1,
                LogLevel.ERROR,
                LogType.ERROR,
                LogStatus.PENDING);
        when(logRecordMapper.selectOne(any())).thenReturn(null);

        LogVO fallback = createNewRuntimeLog(runtimeRequest(
                "local_ai_fallback_to_official",
                "Local AI unavailable; official AI connected",
                LogLevel.INFO,
                LogType.RUNNING));

        assertEquals(LogStatus.RESOLVED, fallback.getStatus());
        assertEquals(LogStatus.PENDING, pendingIncident.getStatus());
        verify(logRecordMapper, never()).updateById(pendingIncident);
    }

    private LogVO createNewRuntimeLog(DeviceRuntimeLogCreateRequest request) {
        if (!"startup".equals(request.getEventType())
                && !"firmware_started".equals(request.getEventType())) {
            when(logRecordMapper.selectOne(any())).thenReturn(null);
        }
        final LogRecord[] inserted = new LogRecord[1];
        when(logRecordMapper.insert(any())).thenAnswer(invocation -> {
            LogRecord record = invocation.getArgument(0);
            record.setId(nextLogId.getAndIncrement());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            inserted[0] = record;
            return 1;
        });
        when(logRecordMapper.selectById(anyLong())).thenAnswer(invocation -> inserted[0]);
        return logService.createDeviceRuntimeLog(request);
    }

    private void stubExistingLog(LogRecord existing) {
        when(logRecordMapper.selectOne(any())).thenReturn(existing);
        when(logRecordMapper.selectById(existing.getId())).thenReturn(existing);
    }

    private LogRecord existingRuntimeLog(
            String content,
            int eventCount,
            String level,
            String logType,
            String status) {
        LogRecord record = new LogRecord();
        record.setId(10L);
        record.setDeviceId(device.getId());
        record.setTitle("设备运行上报（" + eventCount + " 条）");
        record.setContent(content);
        record.setLevel(level);
        record.setLogType(logType);
        record.setStatus(status);
        record.setSource(LogSource.DEVICE);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return record;
    }

    private DeviceRuntimeLogCreateRequest runtimeRequest(
            String eventType,
            String message,
            String level,
            String logType) {
        DeviceRuntimeLogCreateRequest request = new DeviceRuntimeLogCreateRequest();
        request.setDeviceCode(DEVICE_CODE);
        request.setEventType(eventType);
        request.setMessage(message);
        request.setLevel(level);
        request.setLogType(logType);
        return request;
    }
}
