package com.aiot.log.service;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceReportCreateRequest;
import com.aiot.log.vo.DeviceReportVO;

public interface DeviceReportService {

    DeviceReportVO createReport(DeviceReportCreateRequest request);

    PageResult<DeviceReportVO> listReports(Long page, Long pageSize, Long deviceId, String status);
}
