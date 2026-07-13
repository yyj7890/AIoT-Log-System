package com.aiot.log.service;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.LogCreateRequest;
import com.aiot.log.dto.DeviceRuntimeLogCreateRequest;
import com.aiot.log.dto.LogStatusUpdateRequest;
import com.aiot.log.dto.LogUpdateRequest;
import com.aiot.log.vo.LogVO;

import java.time.LocalDateTime;
import java.util.List;

public interface LogService {

    PageResult<LogVO> listLogs(
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
            LocalDateTime endTime);

    LogVO getLog(Long id);

    LogVO createLog(LogCreateRequest request);

    LogVO createDeviceRuntimeLog(DeviceRuntimeLogCreateRequest request);

    LogVO updateLog(Long id, LogUpdateRequest request);

    LogVO updateLogStatus(Long id, LogStatusUpdateRequest request);

    void deleteLog(Long id);

    void deleteLogs(List<Long> ids);
}
