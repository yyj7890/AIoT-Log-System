package com.aiot.log.service;

import com.aiot.log.dto.ReminderRequest;
import com.aiot.log.vo.ReminderVO;
import java.util.List;

public interface ReminderService {
    ReminderVO create(ReminderRequest request);
    List<ReminderVO> list(String deviceCode);
    void cancel(Long id);
    void publishDueReminders();
}
