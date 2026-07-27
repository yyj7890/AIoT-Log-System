package com.aiot.log.announcement;

import com.aiot.log.entity.AnnouncementDelivery;
import com.aiot.log.entity.AnnouncementDeliveryEvent;
import com.aiot.log.mapper.AnnouncementDeliveryEventMapper;
import com.aiot.log.mapper.AnnouncementDeliveryMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementAckServiceTest {

    @Test
    void recordsReceivedAckAndUpdatesDelivery() {
        AnnouncementDeliveryMapper deliveryMapper = mock(AnnouncementDeliveryMapper.class);
        AnnouncementDeliveryEventMapper eventMapper = mock(AnnouncementDeliveryEventMapper.class);
        AnnouncementDelivery delivery = delivery();
        when(deliveryMapper.selectOne(any())).thenReturn(delivery);
        when(eventMapper.selectCount(any())).thenReturn(0L);
        AnnouncementAckService service = new AnnouncementAckService(deliveryMapper, eventMapper);

        boolean recorded = service.record(ack("received", null));

        assertTrue(recorded);
        org.junit.jupiter.api.Assertions.assertEquals("RECEIVED", delivery.getStatus());
        verify(eventMapper).insert(any(AnnouncementDeliveryEvent.class));
        verify(deliveryMapper).updateById(delivery);
    }

    @Test
    void ignoresDuplicateAckWithoutCreatingAnotherEvent() {
        AnnouncementDeliveryMapper deliveryMapper = mock(AnnouncementDeliveryMapper.class);
        AnnouncementDeliveryEventMapper eventMapper = mock(AnnouncementDeliveryEventMapper.class);
        when(deliveryMapper.selectOne(any())).thenReturn(delivery());
        when(eventMapper.selectCount(any())).thenReturn(1L);
        AnnouncementAckService service = new AnnouncementAckService(deliveryMapper, eventMapper);

        assertFalse(service.record(ack("played", null)));
        verify(eventMapper, org.mockito.Mockito.never()).insert(any());
    }

    private AnnouncementDelivery delivery() {
        AnnouncementDelivery delivery = new AnnouncementDelivery();
        delivery.setId(1L);
        delivery.setTaskId("task-1");
        delivery.setDeviceCode("DEVICE-001");
        delivery.setStatus("PUBLISHED");
        return delivery;
    }

    private AnnouncementAck ack(String status, String reason) {
        AnnouncementAck ack = new AnnouncementAck();
        ack.setProtocol("aiot-announcement-v1");
        ack.setTaskId("task-1");
        ack.setDeviceCode("DEVICE-001");
        ack.setStatus(status);
        ack.setReason(reason);
        ack.setReportedAt(LocalDateTime.of(2026, 7, 27, 12, 0));
        return ack;
    }
}
