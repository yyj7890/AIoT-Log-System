package com.aiot.log.announcement;

import com.aiot.log.config.MqttDeviceReportSubscriber;
import org.springframework.stereotype.Component;

@Component
public class PahoAnnouncementMqttGateway implements AnnouncementMqttGateway {
    private final MqttDeviceReportSubscriber subscriber;

    public PahoAnnouncementMqttGateway(MqttDeviceReportSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void publish(String topic, byte[] payload) {
        subscriber.publishAnnouncement(topic, payload);
    }
}
