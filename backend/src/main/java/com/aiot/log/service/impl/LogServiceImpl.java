package com.aiot.log.service.impl;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.LogCreateRequest;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LogServiceImpl implements LogService {

    private static final String DEFAULT_SOURCE = "MANUAL";

    private final LogRecordMapper logRecordMapper;
    private final DeviceMapper deviceMapper;
    private final TagMapper tagMapper;
    private final LogTagMapper logTagMapper;

    public LogServiceImpl(
            LogRecordMapper logRecordMapper,
            DeviceMapper deviceMapper,
            TagMapper tagMapper,
            LogTagMapper logTagMapper) {
        this.logRecordMapper = logRecordMapper;
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
                .orderByDesc(LogRecord::getCreatedAt);

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
        getExistingLog(id);
        logTagMapper.delete(new LambdaQueryWrapper<LogTag>().eq(LogTag::getLogId, id));
        logRecordMapper.deleteById(id);
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
