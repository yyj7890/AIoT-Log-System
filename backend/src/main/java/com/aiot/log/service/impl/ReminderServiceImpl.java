package com.aiot.log.service.impl;

import com.aiot.log.announcement.FixedTestAnnouncementService;
import com.aiot.log.config.ReminderProperties;
import com.aiot.log.dto.ReminderRequest;
import com.aiot.log.entity.Device;
import com.aiot.log.entity.Reminder;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.ReminderMapper;
import com.aiot.log.service.ReminderService;
import com.aiot.log.vo.ReminderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderServiceImpl implements ReminderService {
    private final ReminderMapper reminderMapper;
    private final DeviceMapper deviceMapper;
    private final FixedTestAnnouncementService announcementService;
    private final ReminderProperties properties;
    public ReminderServiceImpl(ReminderMapper reminderMapper, DeviceMapper deviceMapper,
                               FixedTestAnnouncementService announcementService, ReminderProperties properties) {
        this.reminderMapper = reminderMapper; this.deviceMapper = deviceMapper;
        this.announcementService = announcementService; this.properties = properties;
    }
    @Override public ReminderVO create(ReminderRequest request) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>().eq(Device::getDeviceCode, request.getDeviceCode()));
        if (device == null) throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND);
        Reminder reminder = new Reminder();
        reminder.setDeviceId(device.getId()); reminder.setDeviceCode(device.getDeviceCode());
        reminder.setMessage(request.getMessage().trim()); reminder.setRemindAt(request.getRemindAt()); reminder.setStatus("SCHEDULED");
        reminderMapper.insert(reminder); return toVO(reminder);
    }
    @Override public List<ReminderVO> list(String deviceCode) {
        LambdaQueryWrapper<Reminder> query = new LambdaQueryWrapper<Reminder>().orderByDesc(Reminder::getRemindAt);
        if (deviceCode != null && !deviceCode.isBlank()) query.eq(Reminder::getDeviceCode, deviceCode);
        return reminderMapper.selectList(query).stream().map(this::toVO).toList();
    }
    @Override public void cancel(Long id) {
        int changed = reminderMapper.update(null, new LambdaUpdateWrapper<Reminder>().eq(Reminder::getId, id)
                .eq(Reminder::getStatus, "SCHEDULED").set(Reminder::getStatus, "CANCELED"));
        if (changed == 0) throw new BusinessException(ErrorCode.REMINDER_NOT_CANCELLABLE);
    }
    @Scheduled(fixedDelayString = "${reminder.poll-interval-ms:30000}")
    @Override public void publishDueReminders() {
        if (!properties.isSchedulerEnabled()) return;
        for (Reminder reminder : reminderMapper.selectList(new LambdaQueryWrapper<Reminder>()
                .eq(Reminder::getStatus, "SCHEDULED").le(Reminder::getRemindAt, LocalDateTime.now()))) {
            int claimed = reminderMapper.update(null, new LambdaUpdateWrapper<Reminder>().eq(Reminder::getId, reminder.getId())
                    .eq(Reminder::getStatus, "SCHEDULED").set(Reminder::getStatus, "TRIGGERING"));
            if (claimed == 0) continue;
            try {
                String taskId = announcementService.publish(reminder.getDeviceCode());
                reminderMapper.update(null, new LambdaUpdateWrapper<Reminder>().eq(Reminder::getId, reminder.getId())
                        .set(Reminder::getStatus, "PUBLISHED").set(Reminder::getDeliveryTaskId, taskId).set(Reminder::getTriggeredAt, LocalDateTime.now()));
            } catch (RuntimeException ex) {
                reminderMapper.update(null, new LambdaUpdateWrapper<Reminder>().eq(Reminder::getId, reminder.getId()).set(Reminder::getStatus, "FAILED"));
            }
        }
    }
    private ReminderVO toVO(Reminder r) { return new ReminderVO(r.getId(), r.getDeviceCode(), r.getMessage(), r.getRemindAt(), r.getStatus(), r.getDeliveryTaskId(), r.getTriggeredAt()); }
}
