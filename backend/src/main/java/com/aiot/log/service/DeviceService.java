package com.aiot.log.service;

import com.aiot.log.common.PageResult;
import com.aiot.log.dto.DeviceCreateRequest;
import com.aiot.log.dto.DeviceUpdateRequest;
import com.aiot.log.vo.DeviceVO;

public interface DeviceService {

    PageResult<DeviceVO> listDevices(Long page, Long pageSize, String keyword, String type, String status);

    DeviceVO getDevice(Long id);

    DeviceVO createDevice(DeviceCreateRequest request);

    DeviceVO updateDevice(Long id, DeviceUpdateRequest request);

    void deleteDevice(Long id);
}

