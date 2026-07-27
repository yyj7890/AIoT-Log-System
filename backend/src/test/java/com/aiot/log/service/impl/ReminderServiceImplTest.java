package com.aiot.log.service.impl;

import com.aiot.log.announcement.FixedTestAnnouncementService;
import com.aiot.log.config.ReminderProperties;
import com.aiot.log.dto.ReminderRequest;
import com.aiot.log.entity.Reminder;
import com.aiot.log.mapper.DeviceMapper;
import com.aiot.log.mapper.ReminderMapper;
import com.aiot.log.vo.ReminderVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReminderServiceImplTest {

    @Test
    void returnsExistingReminderForSameRequestId() {
        ReminderMapper reminderMapper = mock(ReminderMapper.class);
        DeviceMapper deviceMapper = mock(DeviceMapper.class);
        Reminder existing = new Reminder();
        LocalDateTime due = LocalDateTime.now().plusMinutes(5).withNano(0);
        existing.setId(8L); existing.setRequestId("mcp-request-8"); existing.setDeviceCode("DEVICE-001");
        existing.setMessage("拿书"); existing.setRemindAt(due); existing.setStatus("SCHEDULED");
        when(reminderMapper.selectOne(any())).thenReturn(existing);
        ReminderServiceImpl service = new ReminderServiceImpl(reminderMapper, deviceMapper,
                mock(FixedTestAnnouncementService.class), new ReminderProperties());
        ReminderRequest request = new ReminderRequest();
        request.setRequestId("mcp-request-8"); request.setDeviceCode("DEVICE-001");
        request.setMessage("拿书"); request.setRemindAt(due);

        ReminderVO result = service.create(request);

        assertEquals(8L, result.id());
        assertEquals("mcp-request-8", result.requestId());
        verifyNoInteractions(deviceMapper);
    }
}
