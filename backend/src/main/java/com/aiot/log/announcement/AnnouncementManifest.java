package com.aiot.log.announcement;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementManifest(String protocol, String taskId, String deviceCode, int priority,
                                   LocalDateTime createdAt, LocalDateTime expiresAt, Audio audio) {
    public record Audio(String codec, int sampleRate, int channels, int frameDurationMs,
                        int frameCount, List<Frame> frames) {
    }

    public record Frame(int index, int bytes, String crc32) {
    }
}
