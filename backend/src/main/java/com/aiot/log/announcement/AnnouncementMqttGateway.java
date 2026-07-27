package com.aiot.log.announcement;

public interface AnnouncementMqttGateway {
    void publish(String topic, byte[] payload);
}
