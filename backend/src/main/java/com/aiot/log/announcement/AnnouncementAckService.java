package com.aiot.log.announcement;

import com.aiot.log.entity.AnnouncementDelivery;
import com.aiot.log.entity.AnnouncementDeliveryEvent;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.AnnouncementDeliveryEventMapper;
import com.aiot.log.mapper.AnnouncementDeliveryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AnnouncementAckService {
    private static final String PROTOCOL = "aiot-announcement-v1";
    private static final Set<String> STATUSES = Set.of("received", "played", "failed");
    private final AnnouncementDeliveryMapper deliveryMapper;
    private final AnnouncementDeliveryEventMapper eventMapper;

    public AnnouncementAckService(AnnouncementDeliveryMapper deliveryMapper,
                                  AnnouncementDeliveryEventMapper eventMapper) {
        this.deliveryMapper = deliveryMapper;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public boolean record(AnnouncementAck ack) {
        validate(ack);
        AnnouncementDelivery delivery = deliveryMapper.selectOne(new LambdaQueryWrapper<AnnouncementDelivery>()
                .eq(AnnouncementDelivery::getTaskId, ack.getTaskId()));
        if (delivery == null) throw new BusinessException(ErrorCode.ANNOUNCEMENT_DELIVERY_NOT_FOUND);
        if (!delivery.getDeviceCode().equals(ack.getDeviceCode())) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_ACK_INVALID, "ACK deviceCode does not match task");
        }
        String status = ack.getStatus().toUpperCase();
        Long duplicates = eventMapper.selectCount(new LambdaQueryWrapper<AnnouncementDeliveryEvent>()
                .eq(AnnouncementDeliveryEvent::getDeliveryId, delivery.getId())
                .eq(AnnouncementDeliveryEvent::getStatus, status));
        if (duplicates != null && duplicates > 0) return false;
        if ("PLAYED".equals(delivery.getStatus()) || "FAILED".equals(delivery.getStatus())) {
            return false;
        }

        LocalDateTime reportedAt = ack.getReportedAt() == null ? LocalDateTime.now() : ack.getReportedAt();
        String reason = sanitizeReason(ack.getReason());
        AnnouncementDeliveryEvent event = new AnnouncementDeliveryEvent();
        event.setDeliveryId(delivery.getId());
        event.setStatus(status);
        event.setReason(reason);
        event.setReportedAt(reportedAt);
        eventMapper.insert(event);

        if ("RECEIVED".equals(status)) {
            delivery.setStatus("RECEIVED");
            delivery.setReceivedAt(reportedAt);
        } else if ("PLAYED".equals(status)) {
            delivery.setStatus("PLAYED");
            delivery.setPlayedAt(reportedAt);
        } else {
            delivery.setStatus("FAILED");
            delivery.setFailedAt(reportedAt);
            delivery.setFailureReason(reason);
        }
        deliveryMapper.updateById(delivery);
        return true;
    }

    private void validate(AnnouncementAck ack) {
        if (ack == null || !PROTOCOL.equals(ack.getProtocol()) || !StringUtils.hasText(ack.getTaskId())
                || !StringUtils.hasText(ack.getDeviceCode()) || !STATUSES.contains(ack.getStatus())) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_ACK_INVALID);
        }
        if (!"failed".equals(ack.getStatus()) && StringUtils.hasText(ack.getReason())) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_ACK_INVALID, "Only failed ACK may contain a reason");
        }
    }

    private String sanitizeReason(String reason) {
        if (!StringUtils.hasText(reason)) return null;
        return reason.replaceAll("[\\r\\n\\t]", " ").trim().substring(0, Math.min(reason.trim().length(), 200));
    }
}
