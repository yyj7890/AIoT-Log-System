package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.DeviceComponentRequest;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.DeviceComponent;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.DeviceComponentMapper;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.vo.DeviceComponentVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devices/{deviceId}/components")
public class DeviceComponentController {
    private final DeviceMapper deviceMapper; private final DeviceComponentMapper componentMapper;
    public DeviceComponentController(DeviceMapper deviceMapper, DeviceComponentMapper componentMapper) { this.deviceMapper = deviceMapper; this.componentMapper = componentMapper; }
    @GetMapping public ApiResponse<List<DeviceComponentVO>> list(@PathVariable Long deviceId) { requireDevice(deviceId); return ApiResponse.success(componentMapper.selectList(new LambdaQueryWrapper<DeviceComponent>().eq(DeviceComponent::getDeviceId, deviceId).orderByAsc(DeviceComponent::getCreatedAt)).stream().map(this::toVO).toList()); }
    @PostMapping public ApiResponse<DeviceComponentVO> create(@PathVariable Long deviceId, @Valid @RequestBody DeviceComponentRequest request) { requireDevice(deviceId); DeviceComponent item = apply(new DeviceComponent(), deviceId, request); item.setSource("MANUAL"); componentMapper.insert(item); return ApiResponse.success(toVO(componentMapper.selectById(item.getId()))); }
    @PutMapping("/{id}") public ApiResponse<DeviceComponentVO> update(@PathVariable Long deviceId, @PathVariable Long id, @Valid @RequestBody DeviceComponentRequest request) { requireDevice(deviceId); DeviceComponent item = requireComponent(deviceId, id); componentMapper.updateById(apply(item, deviceId, request)); return ApiResponse.success(toVO(componentMapper.selectById(id))); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long deviceId, @PathVariable Long id) { requireDevice(deviceId); componentMapper.deleteById(requireComponent(deviceId, id).getId()); return ApiResponse.success(); }
    private void requireDevice(Long id) { if (deviceMapper.selectById(id) == null) throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND); }
    private DeviceComponent requireComponent(Long deviceId, Long id) { DeviceComponent item = componentMapper.selectById(id); if (item == null || !deviceId.equals(item.getDeviceId())) throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND); return item; }
    private DeviceComponent apply(DeviceComponent item, Long deviceId, DeviceComponentRequest request) { item.setDeviceId(deviceId); item.setName(request.getName()); item.setCategory(request.getCategory()); item.setModel(request.getModel()); item.setQuantity(request.getQuantity() == null ? 1 : request.getQuantity()); item.setNotes(request.getNotes()); return item; }
    private DeviceComponentVO toVO(DeviceComponent item) { DeviceComponentVO vo = new DeviceComponentVO(); vo.setId(item.getId()); vo.setDeviceId(item.getDeviceId()); vo.setName(item.getName()); vo.setCategory(item.getCategory()); vo.setModel(item.getModel()); vo.setQuantity(item.getQuantity()); vo.setNotes(item.getNotes()); vo.setSource(item.getSource()); vo.setUpdatedAt(item.getUpdatedAt()); return vo; }
}
