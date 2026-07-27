package com.aiot.log.announcement;

public record AnnouncementAudioFrame(int index, byte[] payload, String crc32) {
}
