package com.aiot.log.service.impl;

import com.aiot.log.common.PageResult;
import com.aiot.log.config.MqttProperties;
import com.aiot.log.dto.LogCreateRequest;
import com.aiot.log.dto.DeviceRuntimeLogCreateRequest;
import com.aiot.log.dto.LogStatusUpdateRequest;
import com.aiot.log.dto.LogUpdateRequest;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.LogTag;
import com.aiot.log.entity.LogRecord;
import com.aiot.log.entity.Tag;
import com.aiot.log.enums.LogLevel;
import com.aiot.log.enums.LogSource;
import com.aiot.log.enums.LogStatus;
import com.aiot.log.enums.LogType;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.LogTagMapper;
import com.aiot.log.mapper.LogRecordMapper;
import com.aiot.log.mapper.TagMapper;
import com.aiot.log.service.LogService;
import com.aiot.log.vo.LogVO;
import com.aiot.log.vo.TagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LogServiceImpl implements LogService {

    private static final String DEFAULT_SOURCE = "MANUAL";

    private final LogRecordMapper logRecordMapper;
    private final MqttProperties mqttProperties;
    private final DeviceMapper deviceMapper;
    private final TagMapper tagMapper;
    private final LogTagMapper logTagMapper;

    public LogServiceImpl(
            LogRecordMapper logRecordMapper,
            MqttProperties mqttProperties,
            DeviceMapper deviceMapper,
            TagMapper tagMapper,
            LogTagMapper logTagMapper) {
        this.logRecordMapper = logRecordMapper;
        this.mqttProperties = mqttProperties;
        this.deviceMapper = deviceMapper;
        this.tagMapper = tagMapper;
        this.logTagMapper = logTagMapper;
    }

    @Override
    public PageResult<LogVO> listLogs(
            Long page,
            Long pageSize,
            Long deviceId,
            String logType,
            String level,
            String status,
            String source,
            Long tagId,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        long current = page == null || page < 1 ? 1 : page;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;

        LambdaQueryWrapper<LogRecord> queryWrapper = new LambdaQueryWrapper<LogRecord>()
                .orderByDesc(LogRecord::getUpdatedAt)
                .orderByDesc(LogRecord::getId);

        if (deviceId != null) {
            queryWrapper.eq(LogRecord::getDeviceId, deviceId);
        }
        if (StringUtils.hasText(logType)) {
            validateLogType(logType);
            queryWrapper.eq(LogRecord::getLogType, logType);
        }
        if (StringUtils.hasText(level)) {
            validateLevel(level);
            queryWrapper.eq(LogRecord::getLevel, level);
        }
        if (StringUtils.hasText(status)) {
            validateStatus(status);
            queryWrapper.eq(LogRecord::getStatus, status);
        }
        if (StringUtils.hasText(source)) {
            validateSource(source);
            queryWrapper.eq(LogRecord::getSource, source);
        }
        if (startTime != null) {
            queryWrapper.ge(LogRecord::getCreatedAt, startTime);
        }
        if (endTime != null) {
            queryWrapper.le(LogRecord::getCreatedAt, endTime);
        }
        if (tagId != null) {
            ensureTagExists(tagId);
            Set<Long> logIds = findLogIdsByTagId(tagId);
            if (logIds.isEmpty()) {
                return new PageResult<LogVO>(new ArrayList<LogVO>(), 0L, current, size);
            }
            queryWrapper.in(LogRecord::getId, logIds);
        }
        if (StringUtils.hasText(keyword)) {
            applyKeywordFilter(queryWrapper, keyword);
        }

        Page<LogRecord> result = logRecordMapper.selectPage(new Page<LogRecord>(current, size), queryWrapper);
        List<LogVO> records = new ArrayList<LogVO>();
        for (LogRecord logRecord : result.getRecords()) {
            records.add(toVO(logRecord));
        }
        return new PageResult<LogVO>(records, result.getTotal(), current, size);
    }

    @Override
    public LogVO getLog(Long id) {
        return toVO(getExistingLog(id));
    }

    @Override
    public LogVO createLog(LogCreateRequest request) {
        ensureDeviceExists(request.getDeviceId());
        validateLogType(request.getLogType());
        validateLevelOrDefault(request.getLevel());
        validateStatusOrDefault(request.getStatus());

        LogRecord logRecord = new LogRecord();
        logRecord.setDeviceId(request.getDeviceId());
        logRecord.setTitle(request.getTitle());
        logRecord.setContent(request.getContent());
        logRecord.setLogType(request.getLogType());
        logRecord.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel() : LogLevel.INFO);
        logRecord.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : LogStatus.PENDING);
        logRecord.setSource(LogSource.MANUAL);
        logRecordMapper.insert(logRecord);
        updateLogTags(logRecord.getId(), request.getTagIds());

        return toVO(logRecordMapper.selectById(logRecord.getId()));
    }

    @Override
    public synchronized LogVO createDeviceRuntimeLog(DeviceRuntimeLogCreateRequest request) {
        Device device = getDeviceByCode(request.getDeviceCode());
        String level = normalizeDeviceRuntimeLevel(request.getLevel());
        validateLevel(level);

        String logType = StringUtils.hasText(request.getLogType())
                ? request.getLogType()
                : (LogLevel.INFO.equals(level) ? LogType.RUNNING : LogType.ERROR);
        validateLogType(logType);

        String eventTitle = resolveDeviceRuntimeTitle(request);
        String eventContent = resolveDeviceRuntimeContent(request);
        String eventSummary = summarizeRuntimeEvent(eventTitle, eventContent);
        // A startup event starts a new firmware boot sequence. It must never be merged
        // into the tail of the previous sequence, even when the reset happens within
        // the normal runtime-event merge window.
        LogRecord logRecord = isStartupEvent(request) ? null : findRecentRuntimeLog(device.getId());
        if (logRecord != null) {
            // QoS 1 may redeliver a message. Suppress only an immediately adjacent,
            // byte-for-byte identical normalized summary in the same boot batch.
            // Do not rely solely on the MQTT duplicate flag because a legitimate
            // retry after a failed first handling attempt still needs processing.
            if (eventSummary.equals(lastRuntimeEventSummary(logRecord.getContent()))) {
                return toVO(logRecordMapper.selectById(logRecord.getId()));
            }
            logRecord.setContent(logRecord.getContent() + "\n" + eventSummary);
            logRecord.setTitle(buildRuntimeLogTitle(countRuntimeEvents(logRecord.getContent())));
            if (levelPriority(level) > levelPriority(logRecord.getLevel())) {
                logRecord.setLevel(level);
                logRecord.setLogType(logType);
            }
            // Keep the highest severity as incident history, but let an explicit
            // recovery close that incident. A later warning/error reopens it even
            // when its severity equals the already stored maximum.
            if (isMqttRecoveryEvent(request)) {
                logRecord.setStatus(LogStatus.RESOLVED);
            } else if (!LogLevel.INFO.equals(level)) {
                logRecord.setStatus(LogStatus.PENDING);
            }
            logRecordMapper.updateById(logRecord);
            return toVO(logRecordMapper.selectById(logRecord.getId()));
        }

        logRecord = new LogRecord();
        logRecord.setDeviceId(device.getId());
        logRecord.setTitle(buildRuntimeLogTitle(1));
        logRecord.setContent(eventSummary);
        logRecord.setLogType(logType);
        logRecord.setLevel(level);
        logRecord.setStatus(LogLevel.INFO.equals(level) ? LogStatus.RESOLVED : LogStatus.PENDING);
        logRecord.setSource(LogSource.DEVICE);
        // The ESP32 may not have synchronized its clock during startup. The database receive time is more reliable here.
        logRecordMapper.insert(logRecord);
        return toVO(logRecordMapper.selectById(logRecord.getId()));
    }

    @Override
    public LogVO updateLog(Long id, LogUpdateRequest request) {
        validateLogType(request.getLogType());
        validateLevelOrDefault(request.getLevel());
        validateStatusOrDefault(request.getStatus());

        LogRecord logRecord = getExistingLog(id);
        logRecord.setTitle(request.getTitle());
        logRecord.setContent(request.getContent());
        logRecord.setLogType(request.getLogType());
        logRecord.setLevel(StringUtils.hasText(request.getLevel()) ? request.getLevel() : LogLevel.INFO);
        logRecord.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : LogStatus.PENDING);
        logRecordMapper.updateById(logRecord);
        updateLogTags(id, request.getTagIds());

        return toVO(logRecordMapper.selectById(id));
    }

    @Override
    public LogVO updateLogStatus(Long id, LogStatusUpdateRequest request) {
        validateStatus(request.getStatus());
        LogRecord logRecord = getExistingLog(id);
        logRecord.setStatus(request.getStatus());
        logRecordMapper.updateById(logRecord);
        return toVO(logRecordMapper.selectById(id));
    }

    @Override
    public void deleteLog(Long id) {
        deleteLogs(List.of(id));
    }

    @Override
    @Transactional
    public void deleteLogs(List<Long> ids) {
        Set<Long> uniqueIds = new LinkedHashSet<Long>(ids);
        if (uniqueIds.isEmpty() || uniqueIds.contains(null)) {
            throw new BusinessException(400, "日志编号不能为空");
        }

        List<LogRecord> existingLogs = logRecordMapper.selectList(new LambdaQueryWrapper<LogRecord>()
                .in(LogRecord::getId, uniqueIds));
        if (existingLogs.size() != uniqueIds.size()) {
            throw new BusinessException(404, "部分日志不存在");
        }

        logTagMapper.delete(new LambdaQueryWrapper<LogTag>().in(LogTag::getLogId, uniqueIds));
        logRecordMapper.delete(new LambdaQueryWrapper<LogRecord>().in(LogRecord::getId, uniqueIds));
    }

    private void updateLogTags(Long logId, List<Long> tagIds) {
        logTagMapper.delete(new LambdaQueryWrapper<LogTag>().eq(LogTag::getLogId, logId));
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueTagIds = new LinkedHashSet<Long>(tagIds);
        ensureTagsExist(uniqueTagIds);
        for (Long tagId : uniqueTagIds) {
            LogTag logTag = new LogTag();
            logTag.setLogId(logId);
            logTag.setTagId(tagId);
            logTagMapper.insert(logTag);
        }
    }

    private void ensureTagsExist(Set<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getId, tagIds));
        if (tags.size() != tagIds.size()) {
            throw new BusinessException(404, "部分标签不存在");
        }
    }

    private void ensureTagExists(Long tagId) {
        if (tagMapper.selectById(tagId) == null) {
            throw new BusinessException(404, "标签不存在");
        }
    }

    private Set<Long> findLogIdsByTagId(Long tagId) {
        List<LogTag> logTags = logTagMapper.selectList(new LambdaQueryWrapper<LogTag>()
                .eq(LogTag::getTagId, tagId));
        Set<Long> logIds = new HashSet<Long>();
        for (LogTag logTag : logTags) {
            logIds.add(logTag.getLogId());
        }
        return logIds;
    }

    private List<TagVO> findTagsByLogId(Long logId) {
        List<LogTag> logTags = logTagMapper.selectList(new LambdaQueryWrapper<LogTag>()
                .eq(LogTag::getLogId, logId));
        List<TagVO> result = new ArrayList<TagVO>();
        for (LogTag logTag : logTags) {
            Tag tag = tagMapper.selectById(logTag.getTagId());
            if (tag != null) {
                result.add(toTagVO(tag));
            }
        }
        return result;
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCreatedAt(tag.getCreatedAt());
        vo.setUpdatedAt(tag.getUpdatedAt());
        return vo;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<LogRecord> queryWrapper, String keyword) {
        Set<Long> deviceIds = findDeviceIdsByKeyword(keyword);
        queryWrapper.and(wrapper -> {
            wrapper.like(LogRecord::getTitle, keyword)
                    .or()
                    .like(LogRecord::getContent, keyword);
            if (!deviceIds.isEmpty()) {
                wrapper.or().in(LogRecord::getDeviceId, deviceIds);
            }
        });
    }

    private Set<Long> findDeviceIdsByKeyword(String keyword) {
        List<Device> devices = deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .like(Device::getName, keyword)
                .or()
                .like(Device::getDeviceCode, keyword));
        Set<Long> deviceIds = new HashSet<Long>();
        for (Device device : devices) {
            deviceIds.add(device.getId());
        }
        return deviceIds;
    }

    private LogRecord getExistingLog(Long id) {
        LogRecord logRecord = logRecordMapper.selectById(id);
        if (logRecord == null) {
            throw new BusinessException(404, "日志不存在");
        }
        return logRecord;
    }

    private Device ensureDeviceExists(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new BusinessException(404, "设备不存在");
        }
        return device;
    }

    private Device getDeviceByCode(String deviceCode) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceCode, deviceCode));
        if (device == null) {
            throw new BusinessException(404, "设备编号不存在");
        }
        return device;
    }

    private LogRecord findRecentRuntimeLog(Long deviceId) {
        int windowSeconds = mqttProperties.getRuntimeLogMergeWindowSeconds() == null
                ? 30
                : Math.max(1, mqttProperties.getRuntimeLogMergeWindowSeconds());
        return logRecordMapper.selectOne(new LambdaQueryWrapper<LogRecord>()
                .eq(LogRecord::getDeviceId, deviceId)
                .eq(LogRecord::getSource, LogSource.DEVICE)
                .likeRight(LogRecord::getTitle, "设备运行上报（")
                .ge(LogRecord::getUpdatedAt, LocalDateTime.now().minusSeconds(windowSeconds))
                .orderByDesc(LogRecord::getUpdatedAt)
                .last("LIMIT 1"));
    }

    private String summarizeRuntimeEvent(String eventTitle, String eventContent) {
        String normalizedContent = eventContent == null ? "" : eventContent.replace('\r', ' ').replace('\n', ' ');
        if (!StringUtils.hasText(normalizedContent) || normalizedContent.equals(eventTitle)) {
            return eventTitle;
        }
        return normalizedContent;
    }

    private String buildRuntimeLogTitle(int eventCount) {
        return "设备运行上报（" + eventCount + " 条）";
    }

    private int countRuntimeEvents(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return content.split("\\r?\\n").length;
    }

    private String lastRuntimeEventSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String[] summaries = content.split("\\r?\\n");
        return summaries[summaries.length - 1];
    }

    private int levelPriority(String level) {
        if (LogLevel.ERROR.equals(level)) {
            return 2;
        }
        if (LogLevel.WARNING.equals(level)) {
            return 1;
        }
        return 0;
    }

    private String normalizeDeviceRuntimeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return LogLevel.INFO;
        }
        String normalizedLevel = level.trim().toUpperCase(Locale.ROOT);
        return "WARN".equals(normalizedLevel) ? LogLevel.WARNING : normalizedLevel;
    }

    private String resolveDeviceRuntimeTitle(DeviceRuntimeLogCreateRequest request) {
        String localizedEvent = localizeRuntimeEvent(request.getEventType());
        if (localizedEvent != null) {
            return localizedEvent;
        }
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle();
        }
        if (StringUtils.hasText(request.getEventType())) {
            return "设备运行事件：" + request.getEventType();
        }
        return "设备运行日志";
    }

    private String resolveDeviceRuntimeContent(DeviceRuntimeLogCreateRequest request) {
        String message = request.getMessage();
        if ("Firmware initialization started".equals(message)) {
            return "固件开始初始化";
        }
        if ("Firmware initialization completed".equals(message)) {
            return "固件初始化完成";
        }
        if ("Wi-Fi connected".equals(message)) {
            return "Wi-Fi 已连接";
        }
        if ("Log MQTT connected".equals(message)) {
            return "日志 MQTT 已连接";
        }
        if ("Log MQTT connection failed and was retried".equals(message)) {
            return "日志 MQTT 连接失败，正在重试";
        }
        if ("Log MQTT connection recovered".equals(message)) {
            return "日志 MQTT 连接已恢复";
        }
        if ("Remote log MQTT TLS connected".equals(message)) {
            return "远程日志 MQTT（TLS 8883）已连接";
        }
        if ("Remote log MQTT TLS connection failed and was retried".equals(message)) {
            return "远程日志 MQTT（TLS 8883）连接失败，正在重试";
        }
        if ("Remote log MQTT TLS connection recovered".equals(message)) {
            return "远程日志 MQTT（TLS 8883）连接已恢复";
        }
        if ("Local AI server discovered".equals(message)) {
            return "已发现本地 AI 服务";
        }
        if ("Local AI server connected".equals(message)) {
            return "已连接本地 AI 服务";
        }
        if ("Local AI server connection failed".equals(message)) {
            return "本地 AI 服务连接失败";
        }
        if ("Local AI WebSocket hello completed".equals(message)) {
            return "本地 AI WebSocket 握手完成";
        }
        if ("Official AI protocol connected or reconnected".equals(message)) {
            return "官方 AI 协议已连接或重连";
        }
        if ("Official AI protocol connected".equals(message)) {
            return "官方 AI 协议已连接";
        }
        return message;
    }

    private boolean isStartupEvent(DeviceRuntimeLogCreateRequest request) {
        return "startup".equals(request.getEventType())
                || "firmware_started".equals(request.getEventType());
    }

    private boolean isMqttRecoveryEvent(DeviceRuntimeLogCreateRequest request) {
        return "mqtt_reconnected".equals(request.getEventType())
                || "mqtt_connection_recovered".equals(request.getEventType());
    }

    private String localizeRuntimeEvent(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        return switch (eventType) {
            case "startup" -> "设备启动";
            case "firmware_started" -> "固件启动";
            case "wifi_connected" -> "Wi-Fi 已连接";
            case "wifi_disconnected" -> "Wi-Fi 已断开";
            case "mqtt_connected" -> "日志 MQTT 已连接";
            case "mqtt_connect_failed", "mqtt_connection_failed" -> "日志 MQTT 连接失败";
            case "mqtt_reconnected", "mqtt_connection_recovered" -> "日志 MQTT 连接已恢复";
            case "mqtt_disconnected" -> "日志 MQTT 已断开";
            case "local_ai_server_discovered" -> "已发现本地 AI 服务";
            case "local_ai_connected" -> "已连接本地 AI 服务";
            case "local_ai_connection_failed" -> "本地 AI 服务连接失败";
            case "local_ai_websocket_hello_completed" -> "本地 AI WebSocket 握手完成";
            case "local_mqtt_broker_discovered" -> "已发现本地日志 MQTT 服务";
            case "official_protocol_connected" -> "官方 AI 协议已连接";
            case "official_protocol_disconnected" -> "官方 AI 协议已断开";
            case "official_protocol_error" -> "官方 AI 协议异常";
            default -> null;
        };
    }

    private void validateLogType(String logType) {
        if (!LogType.isValid(logType)) {
            throw new BusinessException(400, "日志类型不合法");
        }
    }

    private void validateLevel(String level) {
        if (!LogLevel.isValid(level)) {
            throw new BusinessException(400, "日志等级不合法");
        }
    }

    private void validateLevelOrDefault(String level) {
        if (StringUtils.hasText(level)) {
            validateLevel(level);
        }
    }

    private void validateStatus(String status) {
        if (!LogStatus.isValid(status)) {
            throw new BusinessException(400, "日志状态不合法");
        }
    }

    private void validateStatusOrDefault(String status) {
        if (StringUtils.hasText(status)) {
            validateStatus(status);
        }
    }

    private void validateSource(String source) {
        if (!LogSource.isValid(source)) {
            throw new BusinessException(400, "日志来源不合法");
        }
    }

    private LogVO toVO(LogRecord logRecord) {
        Device device = ensureDeviceExists(logRecord.getDeviceId());
        LogVO vo = new LogVO();
        vo.setId(logRecord.getId());
        vo.setDeviceId(logRecord.getDeviceId());
        vo.setDeviceName(device.getName());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setTitle(logRecord.getTitle());
        vo.setContent(logRecord.getContent());
        vo.setLogType(logRecord.getLogType());
        vo.setLevel(logRecord.getLevel());
        vo.setStatus(logRecord.getStatus());
        vo.setSource(logRecord.getSource());
        vo.setTags(findTagsByLogId(logRecord.getId()));
        vo.setCreatedAt(logRecord.getCreatedAt());
        vo.setUpdatedAt(logRecord.getUpdatedAt());
        return vo;
    }
}
